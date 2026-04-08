package com.example.lunadesk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelInfo(
    val id: String,
    @SerialName("object") val objectType: String? = null,
    @SerialName("owned_by") val ownedBy: String? = null
)

@Serializable
data class ModelsResponse(
    val data: List<ModelInfo> = emptyList()
)

@Serializable
data class LoadModelRequest(
    val model: String,
    @SerialName("context_length") val contextLength: Int? = null,
    @SerialName("flash_attention") val flashAttention: Boolean? = null
)

@Serializable
data class ChatMessagePayload(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessagePayload>,
    val temperature: Float,
    @SerialName("max_tokens") val maxTokens: Int,
    val stream: Boolean
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ChatChoice(
    val message: ChatAssistantMessage? = null
)

@Serializable
data class ChatAssistantMessage(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class ChatStreamResponse(
    val choices: List<ChatStreamChoice> = emptyList()
)

@Serializable
data class ChatStreamChoice(
    val delta: ChatStreamDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChatStreamDelta(
    val content: String? = null
)

data class StreamChunk(
    val delta: String = "",
    val done: Boolean = false,
    val cancelled: Boolean = false,
    val error: String? = null
)

data class ChatMessageUi(
    val id: String,
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
    val errorText: String? = null
)
