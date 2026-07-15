package com.example.lunadesk.data.remote

import com.example.lunadesk.data.model.ChatCompletionRequest
import com.example.lunadesk.data.model.ModelInfo
import com.example.lunadesk.data.model.StreamChunk
import kotlinx.coroutines.flow.Flow

interface OpenAiCompatibleService {
    suspend fun testConnection(baseUrl: String, apiKey: String = ""): Int

    suspend fun fetchModels(baseUrl: String, apiKey: String = ""): List<ModelInfo>

    fun streamChat(
        baseUrl: String,
        requestModel: ChatCompletionRequest,
        apiKey: String = ""
    ): Flow<StreamChunk>

    fun cancelActiveChat()
}
