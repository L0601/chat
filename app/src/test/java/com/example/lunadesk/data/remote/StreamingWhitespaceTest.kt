package com.example.lunadesk.data.remote

import com.example.lunadesk.data.model.ChatCompletionRequest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.ServerSocket
import kotlin.concurrent.thread

class StreamingWhitespaceTest {
    @Test
    fun `stream retains newline and space only chunks between markdown blocks`() = runBlocking {
        ServerSocket(0).use { server ->
            val writer = thread {
                server.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader()
                    while (reader.readLine()?.isNotEmpty() == true) Unit
                    val parts = listOf("标题", "\n\n", "**", "重点", "**", " ", "内容") + List(250) { "\n" }
                    val body = parts.joinToString("") { value ->
                        val escaped = value.replace("\n", "\\n")
                        "data: {\"choices\":[{\"delta\":{\"content\":\"$escaped\"}}]}\n\n"
                    } + "data: [DONE]\n\n"
                    val bytes = body.toByteArray(Charsets.UTF_8)
                    socket.getOutputStream().apply {
                        write("HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        write(bytes); flush()
                    }
                }
            }
            val service = LmStudioRepository(OkHttpClient())
            val request = ChatCompletionRequest("test", emptyList(), 0.3f, 100, true)
            val result = service.streamChat("http://127.0.0.1:${server.localPort}", request, "").toList()
            writer.join()
            assertEquals("标题\n\n**重点** 内容" + "\n".repeat(250), result.joinToString("") { it.delta })
        }
    }
}
