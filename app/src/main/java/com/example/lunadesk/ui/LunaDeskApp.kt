package com.example.lunadesk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lunadesk.data.local.ApiProfile
import com.example.lunadesk.ui.components.showAppToast
import com.example.lunadesk.ui.screen.chat.ChatScreen
import com.example.lunadesk.ui.screen.settings.SettingsActions
import com.example.lunadesk.ui.screen.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun LunaDeskRoot(viewModel: LunaDeskViewModel) {
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var activeToast by remember { mutableStateOf<android.widget.Toast?>(null) }

    LaunchedEffect(state.toastEvent?.id) {
        val event = state.toastEvent ?: return@LaunchedEffect
        activeToast = showAppToast(context, event.message, activeToast)
        viewModel.consumeToast(event.id)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(0.86f),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                DrawerContent(
                    state = state,
                    onSelectProfile = { id ->
                        viewModel.requestActivateProfile(id)
                        scope.launch { drawerState.close() }
                    },
                    onCreateProfile = {
                        viewModel.requestCreateProfile()
                        scope.launch { drawerState.close() }
                    },
                    onNavigate = { tab ->
                        viewModel.switchTab(tab)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (state.currentTab) {
                AppTab.Chat -> ChatScreen(
                    state = state,
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onInputChange = viewModel::updateChatInput,
                    onSend = viewModel::sendMessage,
                    onStop = viewModel::stopStreaming,
                    onReset = viewModel::resetConversation
                )
                AppTab.Settings -> SettingsScreen(
                    state = state,
                    actions = SettingsActions(
                        onBack = { viewModel.switchTab(AppTab.Chat) },
                        onCreate = viewModel::requestCreateProfile,
                        onEdit = viewModel::requestEditProfile,
                        onCloseEditor = viewModel::requestCloseEditor,
                        onActivate = viewModel::requestActivateProfile,
                        onDelete = viewModel::requestDeleteProfile,
                        onNameChange = viewModel::updateProfileName,
                        onBaseUrlChange = viewModel::updateBaseUrl,
                        onApiKeyChange = viewModel::updateApiKey,
                        onTemperatureChange = viewModel::updateTemperature,
                        onMaxTokensChange = viewModel::updateMaxTokens,
                        onModelChange = viewModel::updateSelectedModel,
                        onSave = viewModel::saveProfile,
                        onTestConnection = viewModel::testConnection,
                        onRefreshModels = viewModel::refreshModels,
                        onSwitchModel = viewModel::switchModel,
                        onModelSearchChange = viewModel::updateModelSearch
                    )
                )
            }
        }
    }
    PendingActionDialog(
        action = state.pendingAction,
        onConfirm = viewModel::confirmPendingAction,
        onDismiss = viewModel::cancelPendingAction
    )
}

@Composable
private fun DrawerContent(
    state: LunaDeskUiState,
    onSelectProfile: (String) -> Unit,
    onCreateProfile: () -> Unit,
    onNavigate: (AppTab) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 20.dp)) {
        DrawerBrandHeader(onCreateProfile)
        NavigationDrawerItem(
            label = { Text("聊天") },
            icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
            selected = state.currentTab == AppTab.Chat,
            onClick = { onNavigate(AppTab.Chat) },
            modifier = Modifier.padding(top = 8.dp),
            shape = RoundedCornerShape(8.dp),
            colors = drawerItemColors()
        )
        Text(
            "API PROFILES",
            modifier = Modifier.padding(start = 12.dp, top = 24.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.9.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.profiles, key = { it.id }) { profile ->
                DrawerProfileItem(
                    profile = profile,
                    selected = profile.id == state.activeProfileId,
                    onClick = { onSelectProfile(profile.id) }
                )
            }
        }
        NavigationDrawerItem(
            label = { Text("管理 API 配置") },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            selected = state.currentTab == AppTab.Settings,
            onClick = { onNavigate(AppTab.Settings) },
            shape = RoundedCornerShape(8.dp),
            colors = drawerItemColors()
        )
    }
}

@Composable
private fun DrawerBrandHeader(onCreateProfile: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "LunaDesk",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "AI WORKBENCH",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onCreateProfile) {
                Icon(Icons.Default.Add, contentDescription = "新增 API 配置")
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun DrawerProfileItem(profile: ApiProfile, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = {
            Column {
                Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    profile.selectedModel.ifBlank { "未选择模型" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        icon = { ProfileMarker(selected) },
        badge = {
            if (selected) {
                Text(
                    "当前",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = drawerItemColors()
    )
}

@Composable
private fun ProfileMarker(selected: Boolean) {
    Box(
        modifier = Modifier
            .width(20.dp)
            .height(32.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .background(
                    color = if (selected) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(1.dp)
                )
        )
    }
}

@Composable
private fun drawerItemColors() = NavigationDrawerItemDefaults.colors(
    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    selectedIconColor = MaterialTheme.colorScheme.secondary,
    selectedTextColor = MaterialTheme.colorScheme.onSurface,
    unselectedContainerColor = Color.Transparent
)

@Composable
private fun PendingActionDialog(
    action: PendingProfileAction?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (action == null) return
    val deleting = action is PendingProfileAction.DeleteProfile
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (deleting) "删除这套配置？" else "放弃未保存修改？") },
        text = {
            Text(
                if (deleting) "删除后无法恢复；如果删除当前配置，将自动切换到其他配置。"
                else "当前编辑内容尚未保存，继续操作会丢失这些修改。"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    if (deleting) "删除" else "放弃修改",
                    color = if (deleting) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
