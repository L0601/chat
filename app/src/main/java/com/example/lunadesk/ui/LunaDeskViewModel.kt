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
    val isLoadingModels: Boolean = false,
    val isSwitchingModel: Boolean = false,
    val isSending: Boolean = false,
    val inlineMessage: String? = null
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
                    maxTokensInput = settings.maxTokens.toString()
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

    fun clearInlineMessage() {
        _uiState.update { it.copy(inlineMessage = null) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = uiState.value
            val settings = buildSettings(state) ?: return@launch showMessage("参数格式不正确")
            container.settingsRepository.saveSettings(settings)
            showMessage("配置已保存")
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
                container.lmStudioRepository.fetchModels(baseUrl)
            }.onSuccess { models ->
                _uiState.update { state ->
                    state.copy(
                        models = models,
                        isLoadingModels = false,
                        selectedModel = state.selectedModel.ifBlank {
                            models.firstOrNull()?.id.orEmpty()
                        }
                    )
                }
                persistCurrentSettings()
                showMessage("模型列表已更新")
            }.onFailure {
                _uiState.update { it.copy(isLoadingModels = false) }
                showMessage(it.message ?: "获取模型列表失败")
            }
        }
    }

    fun switchModel(modelId: String) {
        val baseUrl = uiState.value.baseUrl.trim()
        if (baseUrl.isBlank()) {
            showMessage("请先填写服务地址")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSwitchingModel = true, inlineMessage = null) }
            runCatching {
                container.lmStudioRepository.loadModel(baseUrl, modelId)
            }.onSuccess {
                _uiState.update { it.copy(selectedModel = modelId, isSwitchingModel = false) }
                persistCurrentSettings()
                showMessage("已切换到 $modelId")
            }.onFailure {
                _uiState.update { it.copy(isSwitchingModel = false) }
                showMessage(it.message ?: "切换模型失败")
            }
        }
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
            container.lmStudioRepository.streamChat(settings.baseUrl, request).collect { chunk ->
                when {
                    chunk.delta.isNotBlank() -> appendAssistantDelta(assistantId, chunk.delta)
                    chunk.done -> finishAssistantMessage(assistantId)
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
                    if (msg.isStreaming) msg.copy(isStreaming = false) else msg
                }
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
            maxTokens = maxTokens
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
                messages = state.messages.map { msg ->
                    if (msg.id == assistantId) msg.copy(isStreaming = false) else msg
                }
            )
        }
    }

    private fun markAssistantError(assistantId: String, error: String) {
        _uiState.update { state ->
            state.copy(
                isSending = false,
                inlineMessage = error,
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
}
