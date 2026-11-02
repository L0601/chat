package com.example.lunadesk.data.remote

import com.example.lunadesk.data.model.ChatCompletionRequest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import kotlin.concurrent.thread

class StreamingUsageTest {
    @Test fun `usage after finish reason is retained`() = runBlocking {
        ServerSocket(0).use { server ->
            var requestBody = ""
            val writer = thread {
                server.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader()
                    var length = 0
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        if (line.startsWith("Content-Length:", true)) length = line.substringAfter(':').trim().toInt()
                    }
                    val bodyChars = CharArray(length)
                    var offset = 0
                    while (offset < length) { val n = reader.read(bodyChars, offset, length - offset); if (n < 0) break; offset += n }
                    requestBody = String(bodyChars, 0, offset)
                    val body = "data: {\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}\n\n" +
                        "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n" +
                        "data: {\"choices\":[],\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":25}}\n\n" + "data: [DONE]\n\n"
                    val bytes = body.toByteArray(Charsets.UTF_8)
                    socket.getOutputStream().apply { write("HTTP/1.1 200 OK\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray()); write(bytes); flush() }
                }
            }
            val request = ChatCompletionRequest("test", emptyList(), 0.3f, 100, true)
            val chunks = LmStudioRepository(OkHttpClient()).streamChat("http://127.0.0.1:${server.localPort}", request, "").toList()
            writer.join()
            assertTrue(requestBody.contains("\"include_usage\":true"))
            assertEquals(100, chunks.last().metrics?.inputTokens)
            assertEquals(25, chunks.last().metrics?.outputTokens)
            assertTrue(chunks.last().done)
        }
    }
}
