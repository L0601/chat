package com.example.lunadesk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lunadesk.ui.screen.chat.ChatScreen
import com.example.lunadesk.ui.screen.settings.SettingsScreen
import com.example.lunadesk.ui.components.showAppToast
import com.example.lunadesk.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@Composable
fun LunaDeskRoot(viewModel: LunaDeskViewModel) {
    val state by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val colors = LocalAppColors.current
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
                modifier = Modifier.fillMaxHeight().fillMaxWidth(0.82f),
                drawerContainerColor = colors.drawerBg
            ) {
                DrawerContent(
                    state = state,
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.gradientStart,
                            colors.gradientMiddle,
                            colors.gradientEnd
                        )
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
                    onReset = viewModel::resetConversation
                )

                AppTab.Settings -> SettingsScreen(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    state = state,
                    onBack = { viewModel.switchTab(AppTab.Chat) },
                    onBaseUrlChange = viewModel::updateBaseUrl,
                    onApiKeyChange = viewModel::updateApiKey,
                    onTemperatureChange = viewModel::updateTemperature,
                    onMaxTokensChange = viewModel::updateMaxTokens,
                    onSave = viewModel::saveSettings,
                    onTestConnection = viewModel::testConnection,
                    onRefreshModels = viewModel::refreshModels,
                    onSwitchModel = viewModel::switchModel,
                    onModelSearchChange = viewModel::updateModelSearch
                )
            }
        }
    }
}

@Composable
private fun DrawerContent(
    state: LunaDeskUiState,
    onNavigate: (AppTab) -> Unit
) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 28.dp)) {
        Text(
            text = "LunaDesk",
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 18.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.drawerCardBg)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    text = "当前模型",
                    color = colors.textTertiary
                )
                Text(
                    text = state.selectedModel.ifBlank { "未选择模型" },
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        NavigationDrawerItem(
            label = { Text("聊天") },
            selected = state.currentTab == AppTab.Chat,
            onClick = { onNavigate(AppTab.Chat) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = colors.drawerSelectedItem,
                unselectedContainerColor = Color.Transparent
            )
        )
        NavigationDrawerItem(
            label = { Text("设置") },
            selected = state.currentTab == AppTab.Settings,
            onClick = { onNavigate(AppTab.Settings) },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = colors.drawerSelectedItem,
                unselectedContainerColor = Color.Transparent
            ),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
