package com.example.lunadesk.data.remote

import com.example.lunadesk.data.model.ChatCompletionRequest
import com.example.lunadesk.data.model.ChatCompletionResponse
import com.example.lunadesk.data.model.ChatStreamResponse
import com.example.lunadesk.data.model.LoadModelRequest
import com.example.lunadesk.data.model.ModelInfo
import com.example.lunadesk.data.model.ModelsResponse
import com.example.lunadesk.data.model.StreamChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
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
) {
    @Volatile
    private var activeChatCall: Call? = null

    suspend fun testConnection(baseUrl: String, apiKey: String = ""): Int {
        val request = Request.Builder()
            .url("${normalizeBaseUrl(baseUrl)}/v1/models")
            .get()
            .withAuth(apiKey)
            .build()

        return execute(request) { 200 }
    }

    suspend fun fetchModels(baseUrl: String, apiKey: String = ""): List<ModelInfo> {
        val request = Request.Builder()
            .url("${normalizeBaseUrl(baseUrl)}/v1/models")
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

    fun streamChat(baseUrl: String, requestModel: ChatCompletionRequest, apiKey: String = ""): Flow<StreamChunk> {
        return callbackFlow {
            val streamJob = launch(Dispatchers.IO) {
                var emittedContent = false
                val call = client.newCall(buildChatRequest(baseUrl, requestModel, apiKey))
                activeChatCall = call

                runCatching {
                    call.execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException(response.body?.string().orEmpty().ifBlank { response.message })
                        }
                        val reader = response.body?.charStream()?.buffered()
                            ?: throw IOException("响应体为空")
                        val pending = StringBuilder()
                        var completed = false

                        reader.useLines { lines ->
                            lines.forEach { line ->
                                if (line.startsWith("data:")) {
                                    pending.append(line.removePrefix("data:").trim()).append('\n')
                                } else if (line.startsWith("event:")) {
                                    pending.append(line.trim()).append('\n')
                                } else if (line.isBlank()) {
                                    val done = parseEvent(pending.toString()) { delta ->
                                        if (delta.isNotBlank()) {
                                            emittedContent = true
                                            trySend(StreamChunk(delta = delta))
                                        }
                                    }
                                    pending.clear()
                                    if (done) {
                                        completed = true
                                        return@useLines
                                    }
                                }
                            }
                        }

                        if (!completed && pending.isNotEmpty()) {
                            parseEvent(pending.toString()) { delta ->
                                if (delta.isNotBlank()) {
                                    emittedContent = true
                                    trySend(StreamChunk(delta = delta))
                                }
                            }
                        }
                    }
                }.onSuccess {
                    trySend(StreamChunk(done = true))
                    close()
                }.onFailure { error ->
                    if (error is CancellationException) {
                        trySend(StreamChunk(cancelled = true))
                        close()
                        return@onFailure
                    }
                    if (emittedContent) {
                        trySend(StreamChunk(error = humanizeError(error)))
                        close()
                        return@onFailure
                    }

                    fallbackToNonStream(baseUrl, requestModel, apiKey, error.message)
                        .onSuccess { content ->
                            if (content.isNotBlank()) {
                                trySend(StreamChunk(delta = content))
                            }
                            trySend(StreamChunk(done = true))
                            close()
                        }.onFailure { fallbackError ->
                            trySend(StreamChunk(error = humanizeError(fallbackError)))
                            close()
                        }
                }
            }

            awaitClose {
                streamJob.cancel()
                activeChatCall?.cancel()
                activeChatCall = null
            }
        }
    }

    fun cancelActiveChat() {
        activeChatCall?.cancel()
        activeChatCall = null
    }

    private suspend fun fallbackToNonStream(
        baseUrl: String,
        requestModel: ChatCompletionRequest,
        apiKey: String,
        reason: String?
    ): Result<String> {
        return runCatching {
            val response = execute(buildChatRequest(baseUrl, requestModel.copy(stream = false), apiKey)) { body ->
                json.decodeFromString<ChatCompletionResponse>(body)
            }
            response.choices.firstOrNull()?.message?.content.orEmpty().ifBlank {
                throw IOException(reason ?: "未收到有效回复")
            }
        }
    }

    private fun parseEvent(payload: String, onDelta: (String) -> Unit): Boolean {
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

        chunk.choices.forEach { choice ->
            onDelta(choice.delta?.content.orEmpty())
        }
        return chunk.choices.any { it.finishReason != null }
    }

    private fun buildChatRequest(baseUrl: String, requestModel: ChatCompletionRequest, apiKey: String = ""): Request {
        val body = json.encodeToString(requestModel).toRequestBody(JSON)
        return Request.Builder()
            .url("${normalizeBaseUrl(baseUrl)}/v1/chat/completions")
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
