package com.example.lunadesk.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(
                    "CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { ProfileListIntro(actions.onCreate) }
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
private fun ProfileListIntro(onCreate: () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("服务配置", style = MaterialTheme.typography.titleMedium)
                Text(
                    "地址、密钥、模型与生成参数独立保存",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onCreate, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("新增", modifier = Modifier.padding(start = 4.dp))
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = if (isActive) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEdit)
                    .semantics { role = Role.Button }
                    .padding(start = 10.dp, end = 4.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(56.dp)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.shapes.extraSmall
                        )
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            profile.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "编辑 ${profile.name}")
                }
                IconButton(onClick = onDelete, enabled = canDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "删除 ${profile.name}")
                }
            }
            if (!isActive) {
                TextButton(
                    onClick = onActivate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Text("设为当前配置", color = MaterialTheme.colorScheme.secondary)
                }
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
            tint = MaterialTheme.colorScheme.secondary
        )
        Text(
            "当前",
            modifier = Modifier.padding(start = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
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
        contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        state.validationError?.let { error ->
            item { ValidationNotice(error) }
        }
        item {
            SettingsSection(
                title = "连接信息",
                subtitle = "为这套配置指定服务地址和访问密钥",
                icon = Icons.Outlined.Link
            ) {
                ConnectionFields(draft, actions)
            }
        }
        item {
            SettingsSection(
                title = "生成参数",
                subtitle = "控制回答的随机度与最大长度",
                icon = Icons.Outlined.Tune
            ) {
                GenerationFields(draft, actions)
            }
        }
        item {
            EditorActions(
                state = state,
                onSave = actions.onSave,
                onTest = actions.onTestConnection
            )
        }
        item {
            SettingsSection(
                title = "模型",
                subtitle = "手动填写，或从服务端读取可用模型",
                icon = Icons.Outlined.Memory
            ) {
                ModelControls(draft, state, actions)
            }
        }
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
private fun SettingsSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsSectionHeader(title, subtitle, icon)
        content()
        HorizontalDivider(
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, subtitle: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ValidationNotice(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small
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
            shape = MaterialTheme.shapes.medium
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
            shape = MaterialTheme.shapes.medium
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
            shape = MaterialTheme.shapes.medium
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
                shape = MaterialTheme.shapes.medium
            )
            OutlinedTextField(
                value = draft.maxTokensInput,
                onValueChange = actions.onMaxTokensChange,
                modifier = modifier,
                singleLine = true,
                label = { Text("最大输出") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("1–200000") },
                shape = MaterialTheme.shapes.medium
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
        Button(
            onClick = onSave,
            enabled = state.hasUnsavedChanges,
            modifier = Modifier.weight(1f)
        ) {
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
        OutlinedTextField(
            value = draft.selectedModel,
            onValueChange = actions.onModelChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("模型") },
            placeholder = { Text("手动填写模型 ID") },
            shape = MaterialTheme.shapes.medium
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
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}

@Composable
private fun ModelRow(model: ModelInfo, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !selected, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else MaterialTheme.colorScheme.surface
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
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
