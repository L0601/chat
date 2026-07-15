package com.example.lunadesk.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lunadesk.BuildConfig
import com.example.lunadesk.data.local.ApiProfile
import com.example.lunadesk.data.model.ModelInfo
import com.example.lunadesk.ui.LunaDeskUiState
import com.example.lunadesk.ui.ProfileDraft

data class SettingsActions(
    val onBack: () -> Unit,
    val onCreate: () -> Unit,
    val onEdit: (String) -> Unit,
    val onCloseEditor: () -> Unit,
    val onActivate: (String) -> Unit,
    val onDelete: (String) -> Unit,
    val onNameChange: (String) -> Unit,
    val onBaseUrlChange: (String) -> Unit,
    val onApiKeyChange: (String) -> Unit,
    val onTemperatureChange: (String) -> Unit,
    val onMaxTokensChange: (String) -> Unit,
    val onModelChange: (String) -> Unit,
    val onSave: () -> Unit,
    val onTestConnection: () -> Unit,
    val onRefreshModels: () -> Unit,
    val onSwitchModel: (String) -> Unit,
    val onModelSearchChange: (String) -> Unit
)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: LunaDeskUiState,
    actions: SettingsActions
) {
    val draft = state.editorDraft
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
    ) {
        SettingsHeader(
            title = when {
                draft?.isNew == true -> "新增配置"
                draft != null -> "编辑配置"
                else -> "API 配置"
            },
            onBack = if (draft == null) actions.onBack else actions.onCloseEditor
        )
        if (draft == null) {
            ProfileList(
                modifier = Modifier.weight(1f),
                state = state,
                actions = actions
            )
        } else {
            ProfileEditor(
                modifier = Modifier.weight(1f),
                state = state,
                draft = draft,
                actions = actions
            )
        }
    }
}

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ProfileList(
    modifier: Modifier,
    state: LunaDeskUiState,
    actions: SettingsActions
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("服务配置", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "每套配置独立保存地址、密钥、模型和生成参数",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(onClick = actions.onCreate) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("新增", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        items(state.profiles, key = { it.id }) { profile ->
            ProfileCard(
                profile = profile,
                isActive = profile.id == state.activeProfileId,
                canDelete = state.profiles.size > 1,
                onEdit = { actions.onEdit(profile.id) },
                onActivate = { actions.onActivate(profile.id) },
                onDelete = { actions.onDelete(profile.id) }
            )
        }
        item {
            Text(
                text = "LunaDesk v${BuildConfig.VERSION_NAME} · ${BuildConfig.BUILD_TAG}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ApiProfile,
    isActive: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .semantics { role = Role.Button },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.name, style = MaterialTheme.typography.titleMedium)
                    if (isActive) ActiveLabel()
                }
                Text(
                    profile.baseUrl.ifBlank { "尚未填写服务地址" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    profile.selectedModel.ifBlank { "尚未选择模型" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!isActive) {
                TextButton(onClick = onActivate) { Text("设为当前") }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "编辑 ${profile.name}")
            }
            IconButton(onClick = onDelete, enabled = canDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "删除 ${profile.name}")
            }
        }
    }
}

@Composable
private fun ActiveLabel() {
    Row(
        modifier = Modifier.padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "当前",
            modifier = Modifier.padding(start = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ProfileEditor(
    modifier: Modifier,
    state: LunaDeskUiState,
    draft: ProfileDraft,
    actions: SettingsActions
) {
    val models = remember(state.models, state.modelSearchQuery) {
        if (state.modelSearchQuery.isBlank()) state.models
        else state.models.filter { it.id.contains(state.modelSearchQuery, ignoreCase = true) }
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.validationError?.let { error ->
            item { ValidationNotice(error) }
        }
        item { ConnectionFields(draft, actions) }
        item { GenerationFields(draft, actions) }
        item {
            EditorActions(
                state = state,
                onSave = actions.onSave,
                onTest = actions.onTestConnection
            )
        }
        item { ModelControls(draft, state, actions) }
        if (models.isNotEmpty()) {
            item {
                Text(
                    "可用模型 · ${models.size}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(models, key = { it.id }) { model ->
                ModelRow(
                    model = model,
                    selected = model.id == draft.selectedModel,
                    onClick = { actions.onSwitchModel(model.id) }
                )
            }
        }
        if (!draft.isNew) {
            item {
                TextButton(
                    onClick = { actions.onDelete(draft.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("删除此配置", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ValidationNotice(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ConnectionFields(draft: ProfileDraft, actions: SettingsActions) {
    var showApiKey by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = actions.onNameChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("配置名称 *") },
            shape = RoundedCornerShape(16.dp)
        )
        OutlinedTextField(
            value = draft.baseUrl,
            onValueChange = actions.onBaseUrlChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("API 地址 *") },
            placeholder = { Text("http://192.168.1.10:1234 或 https://host/v1") },
            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
            supportingText = { Text("兼容包含或不包含 /v1 的地址") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = RoundedCornerShape(16.dp)
        )
        OutlinedTextField(
            value = draft.apiKey,
            onValueChange = actions.onApiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("API Key（可选）") },
            leadingIcon = { Icon(Icons.Outlined.VpnKey, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        if (showApiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showApiKey) "隐藏 API Key" else "显示 API Key"
                    )
                }
            },
            visualTransformation = if (showApiKey) {
                VisualTransformation.None
            } else PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun GenerationFields(draft: ProfileDraft, actions: SettingsActions) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 380.dp
        val content: @Composable (Modifier) -> Unit = { modifier ->
            OutlinedTextField(
                value = draft.temperatureInput,
                onValueChange = actions.onTemperatureChange,
                modifier = modifier,
                singleLine = true,
                label = { Text("温度") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text("0–2") },
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = draft.maxTokensInput,
                onValueChange = actions.onMaxTokensChange,
                modifier = modifier,
                singleLine = true,
                label = { Text("最大输出") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("1–200000") },
                shape = RoundedCornerShape(16.dp)
            )
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                content(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EditorActions(
    state: LunaDeskUiState,
    onSave: () -> Unit,
    onTest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onSave, modifier = Modifier.weight(1f)) {
            Text(if (state.hasUnsavedChanges) "保存配置" else "已保存")
        }
        OutlinedButton(
            onClick = onTest,
            enabled = !state.isTestingConnection,
            modifier = Modifier.weight(1f)
        ) {
            Text(if (state.isTestingConnection) "测试中…" else "测试连接")
        }
    }
}

@Composable
private fun ModelControls(
    draft: ProfileDraft,
    state: LunaDeskUiState,
    actions: SettingsActions
) {
    val focusManager = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        OutlinedTextField(
            value = draft.selectedModel,
            onValueChange = actions.onModelChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("模型") },
            placeholder = { Text("手动填写模型 ID") },
            shape = RoundedCornerShape(16.dp)
        )
        OutlinedButton(
            onClick = actions.onRefreshModels,
            enabled = !state.isLoadingModels,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isLoadingModels) "正在获取…" else "获取服务端模型")
        }
        if (state.models.isNotEmpty()) {
            OutlinedTextField(
                value = state.modelSearchQuery,
                onValueChange = actions.onModelSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("搜索模型") },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun ModelRow(model: ModelInfo, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !selected, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
            }
            Text(
                model.id,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (selected) "已选择" else "选择",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
