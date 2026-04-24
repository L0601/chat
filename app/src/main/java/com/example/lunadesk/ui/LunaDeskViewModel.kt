package com.example.lunadesk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lunadesk.data.AppContainer
import com.example.lunadesk.data.local.UserSettings
import com.example.lunadesk.data.model.ChatCompletionRequest
import com.example.lunadesk.data.model.ChatMessagePayload
import com.example.lunadesk.data.model.ChatMessageUi
import com.example.lunadesk.data.model.ModelInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab(val title: String) {
    Chat("聊天"),
    Settings("设置")
}

data class LunaDeskUiState(
    val currentTab: AppTab = AppTab.Chat,
    val baseUrl: String = "",
    val selectedModel: String = "",
    val temperatureInput: String = "0.7",
    val maxTokensInput: String = "2048",
    val models: List<ModelInfo> = emptyList(),
    val chatInput: String = "",
    val messages: List<ChatMessageUi> = emptyList(),
    val connectionStatus: String? = null,
    val isLoadingModels: Boolean = false,
    val isTestingConnection: Boolean = false,
    val isSwitchingModel: Boolean = false,
    val switchingModelId: String? = null,
    val isSending: Boolean = false,
    val inlineMessage: String? = null,
    val apiKey: String = ""
)

class LunaDeskViewModel(
    private val container: AppContainer
) : ViewModel() {
    private val _uiState = MutableStateFlow(LunaDeskUiState())
    val uiState: StateFlow<LunaDeskUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = container.settingsRepository.getSettings()
            _uiState.update { state ->
                state.copy(
                    baseUrl = settings.baseUrl,
                    selectedModel = settings.selectedModel,
                    temperatureInput = settings.temperature.toString(),
                    maxTokensInput = settings.maxTokens.toString(),
                    apiKey = settings.apiKey
                )
            }
        }
    }

    fun switchTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun updateBaseUrl(value: String) {
        _uiState.update { it.copy(baseUrl = value) }
    }

    fun updateTemperature(value: String) {
        _uiState.update { it.copy(temperatureInput = value) }
    }

    fun updateMaxTokens(value: String) {
        _uiState.update { it.copy(maxTokensInput = value) }
    }

    fun updateChatInput(value: String) {
        _uiState.update { it.copy(chatInput = value) }
    }

    fun updateApiKey(value: String) {
        _uiState.update { it.copy(apiKey = value) }
    }

    fun clearInlineMessage() {
        _uiState.update { it.copy(inlineMessage = null) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = uiState.value
            val settings = buildSettings(state) ?: return@launch showMessage("参数格式不正确")
            container.settingsRepository.saveSettings(settings)
            _uiState.update { it.copy(connectionStatus = "配置已保存") }
            showMessage("配置已保存")
        }
    }

    fun testConnection() {
        val baseUrl = uiState.value.baseUrl.trim()
        if (baseUrl.isBlank()) {
            showMessage("请先填写服务地址")
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTestingConnection = true,
                    inlineMessage = null,
                    connectionStatus = "正在测试连接"
                )
            }
            runCatching {
                container.lmStudioRepository.testConnection(baseUrl, uiState.value.apiKey)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        connectionStatus = "连接成功，可访问模型服务"
                    )
                }
                showMessage("连接测试成功")
            }.onFailure { error ->
                val message = readableError(error)
                _uiState.update { state ->
                    state.copy(
                        isTestingConnection = false,
                        connectionStatus = message
                    )
                }
                showMessage(message)
            }
        }
    }

    fun refreshModels() {
        val baseUrl = uiState.value.baseUrl.trim()
        if (baseUrl.isBlank()) {
            showMessage("请先填写服务地址")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModels = true, inlineMessage = null) }
            runCatching {
                container.lmStudioRepository.fetchModels(baseUrl, uiState.value.apiKey)
            }.onSuccess { models ->
                _uiState.update { state ->
                    state.copy(
                        models = models,
                        isLoadingModels = false,
                        connectionStatus = "连接正常，已获取 ${models.size} 个模型",
                        selectedModel = resolveSelectedModel(state.selectedModel, models)
                    )
                }
                persistCurrentSettings()
            }.onFailure {
                _uiState.update { it.copy(isLoadingModels = false) }
                showMessage(readableError(it))
            }
        }
    }

    fun switchModel(modelId: String) {
        val state = uiState.value
        if (state.selectedModel == modelId) {
            return
        }

        _uiState.update {
            it.copy(
                selectedModel = modelId,
                inlineMessage = "已选择 $modelId",
                connectionStatus = "当前模型：$modelId"
            )
        }
        persistCurrentSettings()
    }

    fun sendMessage() {
        val state = uiState.value
        if (state.isSending) return
        if (state.baseUrl.isBlank()) return showMessage("请先配置服务地址")
        if (state.selectedModel.isBlank()) return showMessage("请先选择模型")
        if (state.chatInput.isBlank()) return

        val settings = buildSettings(state) ?: return showMessage("参数格式不正确")
        val userMessage = ChatMessageUi(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = state.chatInput.trim()
        )
        val assistantId = UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessageUi(
            id = assistantId,
            role = "assistant",
            content = "",
            isStreaming = true
        )
        val request = buildChatRequest(settings, state.messages, userMessage)

        _uiState.update {
            it.copy(
                chatInput = "",
                isSending = true,
                inlineMessage = "正在生成",
                messages = it.messages + userMessage + assistantPlaceholder
            )
        }

        viewModelScope.launch {
            container.lmStudioRepository.streamChat(settings.baseUrl, request, settings.apiKey).collect { chunk ->
                when {
                    chunk.delta.isNotBlank() -> appendAssistantDelta(assistantId, chunk.delta)
                    chunk.done -> finishAssistantMessage(assistantId)
                    chunk.cancelled -> finishCancelledMessage(assistantId)
                    chunk.error != null -> markAssistantError(assistantId, chunk.error)
                }
            }
        }
    }

    fun stopStreaming() {
        container.lmStudioRepository.cancelActiveChat()
        _uiState.update { state ->
            state.copy(
                isSending = false,
                inlineMessage = "已停止生成",
                messages = state.messages.map { msg ->
                    if (msg.isStreaming) {
                        val content = msg.content.ifBlank { "已停止生成" }
                        msg.copy(content = content, isStreaming = false)
                    } else {
                        msg
                    }
                }
            )
        }
    }

    fun resetConversation() {
        container.lmStudioRepository.cancelActiveChat()
        _uiState.update {
            it.copy(
                messages = emptyList(),
                chatInput = "",
                isSending = false,
                inlineMessage = "已重置本轮对话"
            )
        }
    }

    private fun buildSettings(state: LunaDeskUiState): UserSettings? {
        val temperature = state.temperatureInput.toFloatOrNull() ?: return null
        val maxTokens = state.maxTokensInput.toIntOrNull() ?: return null
        return UserSettings(
            baseUrl = state.baseUrl.trim().trimEnd('/'),
            selectedModel = state.selectedModel.trim(),
            temperature = temperature,
            maxTokens = maxTokens,
            apiKey = state.apiKey.trim()
        )
    }

    private fun buildChatRequest(
        settings: UserSettings,
        messages: List<ChatMessageUi>,
        userMessage: ChatMessageUi
    ): ChatCompletionRequest {
        val history = messages
            .filter { it.content.isNotBlank() }
            .map { ChatMessagePayload(role = it.role, content = it.content) }

        return ChatCompletionRequest(
            model = settings.selectedModel,
            messages = history + ChatMessagePayload("user", userMessage.content),
            temperature = settings.temperature,
            maxTokens = settings.maxTokens,
            stream = true
        )
    }

    private fun appendAssistantDelta(assistantId: String, delta: String) {
        _uiState.update { state ->
            state.copy(
                inlineMessage = null,
                messages = state.messages.map { msg ->
                    if (msg.id == assistantId) {
                        msg.copy(content = msg.content + delta, isStreaming = true)
                    } else {
                        msg
                    }
                }
            )
        }
    }

    private fun finishAssistantMessage(assistantId: String) {
        _uiState.update { state ->
            state.copy(
                isSending = false,
                inlineMessage = null,
                connectionStatus = "本轮对话已完成，当前共有 ${countCompletedMessages(state.messages)} 条消息",
                messages = state.messages.map { msg ->
                    if (msg.id == assistantId) msg.copy(isStreaming = false) else msg
                }
            )
        }
    }

    private fun finishCancelledMessage(assistantId: String) {
        _uiState.update { state ->
            state.copy(
                isSending = false,
                inlineMessage = "已停止生成",
                messages = state.messages.map { msg ->
                    if (msg.id == assistantId) {
                        msg.copy(
                            content = msg.content.ifBlank { "已停止生成" },
                            isStreaming = false
                        )
                    } else {
                        msg
                    }
                }
            )
        }
    }

    private fun markAssistantError(assistantId: String, error: String) {
        _uiState.update { state ->
            state.copy(
                isSending = false,
                inlineMessage = error,
                connectionStatus = "请求失败，请检查服务状态",
                messages = state.messages.map { msg ->
                    if (msg.id == assistantId) {
                        msg.copy(isStreaming = false, errorText = error)
                    } else {
                        msg
                    }
                }
            )
        }
    }

    private fun persistCurrentSettings() {
        viewModelScope.launch {
            buildSettings(uiState.value)?.let { settings ->
                container.settingsRepository.saveSettings(settings)
            }
        }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(inlineMessage = message) }
    }

    private fun countCompletedMessages(messages: List<ChatMessageUi>): Int {
        return messages.count { it.content.isNotBlank() }
    }

    private fun readableError(error: Throwable): String {
        return error.message ?: "请求失败，请稍后重试"
    }
}

internal fun resolveSelectedModel(
    selectedModel: String,
    models: List<ModelInfo>
): String {
    if (selectedModel.isBlank()) return ""
    return selectedModel.takeIf { current ->
        models.any { it.id == current }
    }.orEmpty()
}
