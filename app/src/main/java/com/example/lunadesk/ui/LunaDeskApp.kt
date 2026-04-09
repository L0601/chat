package com.example.lunadesk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lunadesk.ui.screen.chat.ChatScreen
import com.example.lunadesk.ui.screen.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun LunaDeskRoot(viewModel: LunaDeskViewModel) {
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxSize(0.82f),
                drawerContainerColor = Color(0xFFF8F5EC)
            ) {
                DrawerContent(
                    state = state,
                    onNavigate = { tab ->
                        viewModel.switchTab(tab)
                        scope.launch { drawerState.close() }
                    },
                    onReset = {
                        viewModel.resetConversation()
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF7F4EA), Color(0xFFE5EEF4), Color(0xFFDDE3DB))
                    )
                )
        ) {
            when (state.currentTab) {
                AppTab.Chat -> ChatScreen(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    state = state,
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onInputChange = viewModel::updateChatInput,
                    onSend = viewModel::sendMessage,
                    onStop = viewModel::stopStreaming,
                    onDismissMessage = viewModel::clearInlineMessage
                )

                AppTab.Settings -> SettingsScreen(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    state = state,
                    onBack = { viewModel.switchTab(AppTab.Chat) },
                    onResetConversation = viewModel::resetConversation,
                    onBaseUrlChange = viewModel::updateBaseUrl,
                    onTemperatureChange = viewModel::updateTemperature,
                    onMaxTokensChange = viewModel::updateMaxTokens,
                    onSave = viewModel::saveSettings,
                    onTestConnection = viewModel::testConnection,
                    onRefreshModels = viewModel::refreshModels,
                    onSwitchModel = viewModel::switchModel
                )
            }
        }
    }
}

@Composable
private fun DrawerContent(
    state: LunaDeskUiState,
    onNavigate: (AppTab) -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 28.dp)) {
        Text(text = "LunaDesk", color = Color(0xFF223732))
        Text(
            text = state.selectedModel.ifBlank { "未选择模型" },
            color = Color(0xFF6D7E79),
            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
        )
        NavigationDrawerItem(
            label = { Text("聊天") },
            selected = state.currentTab == AppTab.Chat,
            onClick = { onNavigate(AppTab.Chat) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color(0xFFE4EEE8),
                unselectedContainerColor = Color.Transparent
            )
        )
        NavigationDrawerItem(
            label = { Text("设置") },
            selected = state.currentTab == AppTab.Settings,
            onClick = { onNavigate(AppTab.Settings) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = Color(0xFFE4EEE8),
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier.padding(top = 8.dp)
        )
        NavigationDrawerItem(
            label = { Text("重置当前聊天") },
            selected = false,
            onClick = onReset,
            colors = NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = Color(0xFFF3E8D1)
            ),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
