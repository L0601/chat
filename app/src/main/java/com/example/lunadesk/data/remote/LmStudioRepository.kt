package com.example.lunadesk.data.remote

import com.example.lunadesk.data.model.ChatCompletionRequest
import com.example.lunadesk.data.model.ChatCompletionResponse
import com.example.lunadesk.data.model.ChatStreamResponse
import com.example.lunadesk.data.model.LoadModelRequest
import com.example.lunadesk.data.model.ModelInfo
import com.example.lunadesk.data.model.ModelsResponse
import com.example.lunadesk.data.model.StreamChunk
import com.example.lunadesk.data.model.StreamOptions
import com.example.lunadesk.data.model.TokenUsage
import com.example.lunadesk.data.model.CallMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.SocketTimeoutException
import java.io.IOException
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LmStudioRepository(
    private val client: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : OpenAiCompatibleService {
    @Volatile
    private var activeChatCall: Call? = null

    override suspend fun testConnection(baseUrl: String, apiKey: String): Int {
        val request = Request.Builder()
            .url(buildOpenAiEndpoint(baseUrl, "models"))
            .get()
            .withAuth(apiKey)
            .build()

        return execute(request) { 200 }
    }

    override suspend fun fetchModels(baseUrl: String, apiKey: String): List<ModelInfo> {
        val request = Request.Builder()
            .url(buildOpenAiEndpoint(baseUrl, "models"))
            .get()
            .withAuth(apiKey)
            .build()

        return execute(request) { body ->
            json.decodeFromString<ModelsResponse>(body).data
        }
    }

    suspend fun loadModel(baseUrl: String, modelId: String, apiKey: String = "") {
        val body = json.encodeToString(LoadModelRequest(model = modelId))
            .toRequestBody(JSON)

        val request = Request.Builder()
            .url("${normalizeBaseUrl(baseUrl)}/api/v1/models/load")
            .post(body)
            .withAuth(apiKey)
            .build()

        execute(request) { }
    }

    override fun streamChat(baseUrl: String, requestModel: ChatCompletionRequest, apiKey: String): Flow<StreamChunk> = callbackFlow {
        val job = launch(Dispatchers.IO) {
            var emitted = false
            var usage: TokenUsage? = null
            var first: Long? = null
            var last: Long? = null
            var visibleChunks = 0
            var started = 0L
            fun collectDelta(delta: String) {
                if (delta.isEmpty()) return
                if (delta.isNotBlank()) {
                    val now = System.nanoTime()
                    if (first == null) first = now
                    last = now
                    visibleChunks++
                }
                emitted = true
                trySendBlocking(StreamChunk(delta = delta))
            }
            fun runStream(withUsage: Boolean) {
                val model = requestModel.copy(stream = true, streamOptions = if (withUsage) StreamOptions(true) else null)
                val call = client.newCall(buildChatRequest(baseUrl, model, apiKey))
                activeChatCall = call
                started = System.nanoTime()
                call.execute().use { response ->
                    if (!response.isSuccessful) throw StreamHttpException(response.code, response.body?.string().orEmpty())
                    val reader = response.body?.charStream()?.buffered() ?: throw IOException("响应体为空")
                    val pending = StringBuilder()
                    var terminal = false
                    reader.useLines { lines ->
                        for (line in lines) {
                            if (line.startsWith("data:")) pending.append(line.removePrefix("data:").trim()).append('\n')
                            else if (line.startsWith("event:")) pending.append(line.trim()).append('\n')
                            else if (line.isBlank()) {
                                val done = parseEvent(pending.toString(), ::collectDelta, { usage = it }) { terminal = true }
                                pending.clear()
                                if (done) { terminal = true; break }
                            }
                        }
                    }
                    if (pending.isNotEmpty() && parseEvent(pending.toString(), ::collectDelta, { usage = it }) { terminal = true }) terminal = true
                    if (!terminal) throw IOException("流式响应未正常结束")
                }
            }
            val result = runCatching {
                try {
                    try { runStream(true) }
                    catch (error: StreamHttpException) {
                        if (!emitted && error.rejectsUsageOption()) runStream(false) else throw error
                    }
                } catch (error: StreamHttpException) {
                    if (emitted || !error.rejectsStreaming()) throw error
                    val response = execute(buildChatRequest(baseUrl, requestModel.copy(stream = false, streamOptions = null), apiKey)) {
                        json.decodeFromString<ChatCompletionResponse>(it)
                    }
                    val content = response.choices.firstOrNull()?.message?.content.orEmpty()
                    if (content.isBlank()) throw IOException("未收到有效回复")
                    usage = response.usage
                    trySendBlocking(StreamChunk(delta = content))
                }
            }
            result.onSuccess {
                val durationMs = if (visibleChunks >= 2 && first != null && last != null) (last!! - first!!) / 1_000_000 else null
                trySendBlocking(StreamChunk(done = true, metrics = CallMetrics(
                    inputTokens = usage?.promptTokens, outputTokens = usage?.completionTokens,
                    firstTextMs = first?.let { (it - started) / 1_000_000 },
                    outputMs = durationMs?.takeIf { it >= 100 }, model = requestModel.model, completed = true
                )))
            }.onFailure { error ->
                trySendBlocking(StreamChunk(error = humanizeError(error), metrics = CallMetrics(
                    inputTokens = usage?.promptTokens, outputTokens = usage?.completionTokens,
                    firstTextMs = first?.let { (it - started) / 1_000_000 }, model = requestModel.model
                )))
            }
            close()
        }
        awaitClose { job.cancel(); activeChatCall?.cancel(); activeChatCall = null }
    }

    private class StreamHttpException(val code: Int, val responseBody: String) : IOException(responseBody.ifBlank { "HTTP $code" }) {
        private fun isRejected(): Boolean = code in listOf(400, 422) &&
            listOf("unsupported", "unknown", "unexpected", "unrecognized", "not supported", "invalid").any { it in responseBody.lowercase() }
        fun rejectsUsageOption(): Boolean = isRejected() && "stream_options" in responseBody.lowercase()
        fun rejectsStreaming(): Boolean = isRejected() && "stream" in responseBody.lowercase() && "stream_options" !in responseBody.lowercase()
    }

    override fun cancelActiveChat() {
        activeChatCall?.cancel()
        activeChatCall = null
    }

    private fun parseEvent(payload: String, onDelta: (String) -> Unit, onUsage: (TokenUsage) -> Unit, onFinish: () -> Unit): Boolean {
        val lines = payload
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return false

        val dataLines = lines.filter { it.startsWith("data:") || !it.startsWith("event:") }
        val content = dataLines.joinToString("\n") { line ->
            if (line.startsWith("data:")) line.removePrefix("data:").trim() else line
        }.trim()
        if (content.isBlank()) return false
        if (content == "[DONE]") return true

        val chunk = runCatching {
            json.decodeFromString<ChatStreamResponse>(content)
        }.getOrNull() ?: return false

        chunk.usage?.let(onUsage)
        if (chunk.choices.any { it.finishReason != null }) onFinish()
        chunk.choices.forEach { choice ->
            onDelta(choice.delta?.content.orEmpty())
        }
        return false
    }

    private fun buildChatRequest(baseUrl: String, requestModel: ChatCompletionRequest, apiKey: String = ""): Request {
        val body = json.encodeToString(requestModel).toRequestBody(JSON)
        return Request.Builder()
            .url(buildOpenAiEndpoint(baseUrl, "chat/completions"))
            .post(body)
            .header("Accept", "text/event-stream")
            .withAuth(apiKey)
            .build()
    }

    private fun Request.Builder.withAuth(apiKey: String): Request.Builder {
        if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
        return this
    }

    private suspend fun <T> execute(request: Request, parser: (String) -> T): T {
        return suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            cont.resumeWithException(IOException(body.ifBlank { response.message }))
                            return
                        }
                        runCatching { parser(body) }
                            .onSuccess { if (cont.isActive) cont.resume(it) }
                            .onFailure { if (cont.isActive) cont.resumeWithException(it) }
                    }
                }
            })
        }
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        return baseUrl.trim().trimEnd('/')
    }

    private fun humanizeError(error: Throwable): String {
        return when (error) {
            is SocketTimeoutException -> "连接超时，请检查地址或服务状态"
            is IOException -> error.message ?: "网络请求失败"
            else -> error.message ?: "请求失败"
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

internal fun buildOpenAiEndpoint(baseUrl: String, path: String): String {
    val root = baseUrl.trim().trimEnd('/')
    val versionedRoot = if (root.endsWith("/v1", ignoreCase = true)) root else "$root/v1"
    return "$versionedRoot/${path.trimStart('/')}"
}
