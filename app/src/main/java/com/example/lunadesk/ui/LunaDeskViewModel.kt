package com.example.lunadesk.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lunadesk.data.IdGenerator
import com.example.lunadesk.data.local.ApiProfile
import com.example.lunadesk.data.local.ProfileStore
import com.example.lunadesk.data.model.ChatCompletionRequest
import com.example.lunadesk.data.model.ChatMessagePayload
import com.example.lunadesk.data.model.ChatMessageUi
import com.example.lunadesk.data.model.ModelInfo
import com.example.lunadesk.data.remote.OpenAiCompatibleService
import java.net.URI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    Chat("聊天"),
    Settings("API 配置")
}

data class ProfileDraft(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val selectedModel: String,
    val temperatureInput: String,
    val maxTokensInput: String,
    val isNew: Boolean
) {
    companion object {
        fun from(profile: ApiProfile, isNew: Boolean = false) = ProfileDraft(
            id = profile.id,
            name = profile.name,
            baseUrl = profile.baseUrl,
            apiKey = profile.apiKey,
            selectedModel = profile.selectedModel,
            temperatureInput = profile.temperature.toString(),
            maxTokensInput = profile.maxTokens.toString(),
            isNew = isNew
        )
    }
}

sealed interface PendingProfileAction {
    data object GoToChat : PendingProfileAction
    data object CloseEditor : PendingProfileAction
    data object CreateProfile : PendingProfileAction
    data class EditProfile(val profileId: String) : PendingProfileAction
    data class ActivateProfile(val profileId: String) : PendingProfileAction
    data class DeleteProfile(val profileId: String) : PendingProfileAction
}

data class LunaDeskUiState(
    val currentTab: AppTab = AppTab.Chat,
    val profiles: List<ApiProfile> = emptyList(),
    val activeProfileId: String = "",
    val editorDraft: ProfileDraft? = null,
    val hasUnsavedChanges: Boolean = false,
    val pendingAction: PendingProfileAction? = null,
    val validationError: String? = null,
    val models: List<ModelInfo> = emptyList(),
    val modelSearchQuery: String = "",
    val chatInput: String = "",
    val messages: List<ChatMessageUi> = emptyList(),
    val connectionStatus: String? = null,
    val isLoadingModels: Boolean = false,
    val isTestingConnection: Boolean = false,
    val isSending: Boolean = false,
    val toastEvent: ToastEvent? = null
) {
    val activeProfile: ApiProfile?
        get() = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.firstOrNull()

    val selectedModel: String
        get() = activeProfile?.selectedModel.orEmpty()
}

data class ToastEvent(
    val id: Long,
    val message: String
)

class LunaDeskViewModel(
    private val profileStore: ProfileStore,
    private val chatService: OpenAiCompatibleService,
    private val idGenerator: IdGenerator
) : ViewModel() {
    private val _uiState = MutableStateFlow(LunaDeskUiState())
    val uiState: StateFlow<LunaDeskUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileStore.settings.collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        profiles = settings.profiles,
                        activeProfileId = settings.activeProfileId,
                        editorDraft = syncedDraft(state, settings.profiles)
                    )
                }
            }
        }
    }

    fun switchTab(tab: AppTab) {
        if (tab == uiState.value.currentTab) return
        if (tab == AppTab.Chat && uiState.value.hasUnsavedChanges) {
            queueAction(PendingProfileAction.GoToChat)
            return
        }
        _uiState.update { it.copy(currentTab = tab, editorDraft = null) }
    }

    fun requestCreateProfile() {
        runOrQueue(PendingProfileAction.CreateProfile) { createProfileDraft() }
    }

    fun requestEditProfile(profileId: String) {
        if (uiState.value.editorDraft?.id == profileId) return
        runOrQueue(PendingProfileAction.EditProfile(profileId)) { openProfileEditor(profileId) }
    }

    fun requestCloseEditor() {
        runOrQueue(PendingProfileAction.CloseEditor, ::closeEditor)
    }

    fun requestActivateProfile(profileId: String) {
        val state = uiState.value
        if (state.isSending) return showToast("请先停止当前生成，再切换配置")
        if (profileId == state.activeProfileId) return
        runOrQueue(PendingProfileAction.ActivateProfile(profileId)) { activateProfile(profileId) }
    }

    fun requestDeleteProfile(profileId: String) {
        if (uiState.value.profiles.size <= 1) {
            showToast("至少保留一套配置")
            return
        }
        queueAction(PendingProfileAction.DeleteProfile(profileId))
    }

    fun confirmPendingAction() {
        val action = uiState.value.pendingAction ?: return
        _uiState.update { it.copy(pendingAction = null, hasUnsavedChanges = false) }
        when (action) {
            PendingProfileAction.GoToChat -> switchTab(AppTab.Chat)
            PendingProfileAction.CloseEditor -> closeEditor()
            PendingProfileAction.CreateProfile -> createProfileDraft()
            is PendingProfileAction.EditProfile -> openProfileEditor(action.profileId)
            is PendingProfileAction.ActivateProfile -> activateProfile(action.profileId)
            is PendingProfileAction.DeleteProfile -> deleteProfile(action.profileId)
        }
    }

    fun cancelPendingAction() {
        _uiState.update { it.copy(pendingAction = null) }
    }

    fun updateProfileName(value: String) = updateDraft { it.copy(name = value) }

    fun updateBaseUrl(value: String) = updateDraft { it.copy(baseUrl = value) }

    fun updateApiKey(value: String) = updateDraft { it.copy(apiKey = value) }

    fun updateTemperature(value: String) = updateDraft { it.copy(temperatureInput = value) }

    fun updateMaxTokens(value: String) = updateDraft { it.copy(maxTokensInput = value) }

    fun updateSelectedModel(value: String) = updateDraft { it.copy(selectedModel = value) }

    fun updateChatInput(value: String) {
        _uiState.update { it.copy(chatInput = value) }
    }

    fun updateModelSearch(query: String) {
        _uiState.update { it.copy(modelSearchQuery = query) }
    }

    fun consumeToast(toastId: Long) {
        _uiState.update { state ->
            state.takeIf { it.toastEvent?.id == toastId }?.copy(toastEvent = null) ?: state
        }
    }

    fun saveProfile() {
        val state = uiState.value
        val draft = state.editorDraft ?: return
        val profile = draft.toProfileOrNull()
            ?: return showValidationError("温度或最大输出格式不正确")
        validateProfile(profile, state.profiles)?.let { return showValidationError(it) }

        viewModelScope.launch {
            runCatching { profileStore.upsert(profile, makeActive = draft.isNew) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            editorDraft = ProfileDraft.from(profile),
                            hasUnsavedChanges = false,
                            validationError = null
                        )
                    }
                    showToast("配置已保存")
                }
                .onFailure { showToast(readableError(it)) }
        }
    }

    fun testConnection() {
        val draft = uiState.value.editorDraft ?: return
        if (!isValidHttpUrl(draft.baseUrl)) return showValidationError("请填写有效的 HTTP/HTTPS 地址")
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, connectionStatus = "正在测试连接") }
            runCatching { chatService.testConnection(draft.baseUrl, draft.apiKey) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(isTestingConnection = false, connectionStatus = "连接成功")
                    }
                    showToast("连接测试成功")
                }
                .onFailure { error ->
                    val message = readableError(error)
                    _uiState.update { it.copy(isTestingConnection = false, connectionStatus = message) }
                    showToast(message)
                }
        }
    }

    fun refreshModels() {
        val draft = uiState.value.editorDraft ?: return
        if (!isValidHttpUrl(draft.baseUrl)) return showValidationError("请填写有效的 HTTP/HTTPS 地址")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModels = true) }
            runCatching { chatService.fetchModels(draft.baseUrl, draft.apiKey) }
                .onSuccess { models ->
                    val uniqueModels = models.distinctBy { it.id }
                    _uiState.update {
                        it.copy(
                            models = uniqueModels,
                            isLoadingModels = false,
                            connectionStatus = "已获取 ${uniqueModels.size} 个模型"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingModels = false) }
                    showToast(readableError(error))
                }
        }
    }

    fun switchModel(modelId: String) {
        updateSelectedModel(modelId)
        showToast("已选择 $modelId，保存后生效")
    }

    fun sendMessage() {
        val state = uiState.value
        val profile = state.activeProfile ?: return showToast("请先创建 API 配置")
        if (state.isSending) return
        if (profile.baseUrl.isBlank()) return showToast("请先配置服务地址")
        if (profile.selectedModel.isBlank()) return showToast("请先选择模型")
        if (state.chatInput.isBlank()) return

        val userMessage = ChatMessageUi(
            id = idGenerator.nextId(),
            role = "user",
            content = state.chatInput.trim()
        )
        val assistantId = idGenerator.nextId()
        val request = buildChatRequest(profile, state.messages, userMessage)

        _uiState.update {
            it.copy(
                chatInput = "",
                isSending = true,
                messages = it.messages + userMessage + ChatMessageUi(
                    id = assistantId,
                    role = "assistant",
                    content = "",
                    isStreaming = true
                )
            )
        }

        viewModelScope.launch {
            chatService.streamChat(profile.baseUrl, request, profile.apiKey).collect { chunk ->
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
        chatService.cancelActiveChat()
        _uiState.update { state ->
            state.copy(
                isSending = false,
                messages = state.messages.map { message ->
                    if (message.isStreaming) {
                        message.copy(
                            content = message.content.ifBlank { "已停止生成" },
                            isStreaming = false
                        )
                    } else message
                }
            )
        }
    }

    fun resetConversation() {
        chatService.cancelActiveChat()
        _uiState.update { it.copy(messages = emptyList(), chatInput = "", isSending = false) }
        showToast("已重置本轮对话")
    }

    private fun createProfileDraft() {
        val state = uiState.value
        val profile = ApiProfile(
            id = idGenerator.nextId(),
            name = nextProfileName(state.profiles)
        )
        _uiState.update {
            it.copy(
                currentTab = AppTab.Settings,
                editorDraft = ProfileDraft.from(profile, isNew = true),
                models = emptyList(),
                modelSearchQuery = "",
                hasUnsavedChanges = true,
                validationError = null
            )
        }
    }

    private fun openProfileEditor(profileId: String) {
        val profile = uiState.value.profiles.firstOrNull { it.id == profileId } ?: return
        _uiState.update {
            it.copy(
                currentTab = AppTab.Settings,
                editorDraft = ProfileDraft.from(profile),
                models = emptyList(),
                modelSearchQuery = "",
                hasUnsavedChanges = false,
                validationError = null
            )
        }
    }

    private fun closeEditor() {
        _uiState.update {
            it.copy(
                editorDraft = null,
                models = emptyList(),
                modelSearchQuery = "",
                hasUnsavedChanges = false,
                validationError = null
            )
        }
    }

    private fun activateProfile(profileId: String) {
        viewModelScope.launch {
            runCatching { profileStore.setActive(profileId) }
                .onSuccess {
                    _uiState.update {
                        it.copy(connectionStatus = null, models = emptyList(), modelSearchQuery = "")
                    }
                    val name = uiState.value.profiles.firstOrNull { it.id == profileId }?.name
                    showToast("已切换到 ${name ?: "所选配置"}")
                }
                .onFailure { showToast(readableError(it)) }
        }
    }

    private fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            runCatching { profileStore.delete(profileId) }
                .onSuccess {
                    if (uiState.value.editorDraft?.id == profileId) closeEditor()
                    showToast("配置已删除")
                }
                .onFailure { showToast(readableError(it)) }
        }
    }

    private fun updateDraft(transform: (ProfileDraft) -> ProfileDraft) {
        _uiState.update { state ->
            val draft = state.editorDraft ?: return@update state
            state.copy(
                editorDraft = transform(draft),
                hasUnsavedChanges = true,
                validationError = null
            )
        }
    }

    private fun runOrQueue(action: PendingProfileAction, block: () -> Unit) {
        if (uiState.value.hasUnsavedChanges) queueAction(action) else block()
    }

    private fun queueAction(action: PendingProfileAction) {
        _uiState.update { it.copy(pendingAction = action) }
    }

    private fun showValidationError(message: String) {
        _uiState.update { it.copy(validationError = message) }
        showToast(message)
    }

    private fun buildChatRequest(
        profile: ApiProfile,
        messages: List<ChatMessageUi>,
        userMessage: ChatMessageUi
    ): ChatCompletionRequest {
        val history = messages
            .filter { it.content.isNotBlank() }
            .map { ChatMessagePayload(role = it.role, content = it.content) }
        return ChatCompletionRequest(
            model = profile.selectedModel,
            messages = history + ChatMessagePayload("user", userMessage.content),
            temperature = profile.temperature,
            maxTokens = profile.maxTokens,
            stream = true
        )
    }

    private fun appendAssistantDelta(assistantId: String, delta: String) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map { message ->
                if (message.id == assistantId) {
                    message.copy(content = message.content + delta, isStreaming = true)
                } else message
            })
        }
    }

    private fun finishAssistantMessage(assistantId: String) {
        _uiState.update { state ->
            state.copy(
                isSending = false,
                connectionStatus = "本轮对话已完成",
                messages = state.messages.map { message ->
                    if (message.id == assistantId) message.copy(isStreaming = false) else message
                }
            )
        }
    }

    private fun finishCancelledMessage(assistantId: String) {
        val wasSending = uiState.value.isSending
        _uiState.update { state ->
            state.copy(
                isSending = false,
                messages = state.messages.map { message ->
                    if (message.id == assistantId) {
                        message.copy(
                            content = message.content.ifBlank { "已停止生成" },
                            isStreaming = false
                        )
                    } else message
                }
            )
        }
        if (wasSending) showToast("已停止生成")
    }

    private fun markAssistantError(assistantId: String, error: String) {
        _uiState.update { state ->
            state.copy(
                isSending = false,
                connectionStatus = "请求失败，请检查服务状态",
                messages = state.messages.map { message ->
                    if (message.id == assistantId) {
                        message.copy(isStreaming = false, errorText = error)
                    } else message
                }
            )
        }
        showToast(error)
    }

    private fun syncedDraft(state: LunaDeskUiState, profiles: List<ApiProfile>): ProfileDraft? {
        val draft = state.editorDraft ?: return null
        if (state.hasUnsavedChanges || draft.isNew) return draft
        val profile = profiles.firstOrNull { it.id == draft.id } ?: return null
        return ProfileDraft.from(profile)
    }

    private fun showToast(message: String) {
        _uiState.update { state ->
            state.copy(toastEvent = nextToastEvent(message, state.toastEvent))
        }
    }

    private fun readableError(error: Throwable): String {
        return error.message ?: "操作失败，请稍后重试"
    }
}

internal fun ProfileDraft.toProfileOrNull(): ApiProfile? {
    val temperature = temperatureInput.toFloatOrNull() ?: return null
    val maxTokens = maxTokensInput.toIntOrNull() ?: return null
    return ApiProfile(
        id = id,
        name = name.trim(),
        baseUrl = baseUrl.trim().trimEnd('/'),
        apiKey = apiKey.trim(),
        selectedModel = selectedModel.trim(),
        temperature = temperature,
        maxTokens = maxTokens
    )
}

internal fun validateProfile(profile: ApiProfile, profiles: List<ApiProfile>): String? {
    if (profile.name.isBlank()) return "配置名称不能为空"
    if (profiles.any { it.id != profile.id && it.name.equals(profile.name, ignoreCase = true) }) {
        return "配置名称不能重复"
    }
    if (!isValidHttpUrl(profile.baseUrl)) return "请填写有效的 HTTP/HTTPS 地址"
    if (profile.temperature !in 0f..2f) return "温度范围应为 0 到 2"
    if (profile.maxTokens !in 1..200000) return "最大输出范围应为 1 到 200000"
    return null
}

internal fun isValidHttpUrl(value: String): Boolean {
    return runCatching {
        val uri = URI(value.trim())
        uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}

internal fun nextProfileName(profiles: List<ApiProfile>): String {
    val names = profiles.map { it.name }.toSet()
    return generateSequence(1) { it + 1 }
        .map { "配置 $it" }
        .first { it !in names }
}

internal fun nextToastEvent(message: String, current: ToastEvent?): ToastEvent {
    return ToastEvent(id = (current?.id ?: 0L) + 1L, message = message)
}

internal fun resolveSelectedModel(selectedModel: String, models: List<ModelInfo>): String {
    if (selectedModel.isBlank()) return ""
    return selectedModel.takeIf { current -> models.any { it.id == current } }.orEmpty()
}
