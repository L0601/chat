package com.example.lunadesk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lunadesk.ui.screen.chat.ChatScreen
import com.example.lunadesk.ui.screen.settings.SettingsScreen

@Composable
fun LunaDeskRoot(viewModel: LunaDeskViewModel) {
    val state by viewModel.uiState.collectAsState()
    val tabs = AppTab.entries

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF7F4EA), Color(0xFFE5EEF4), Color(0xFFDDE3DB))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    tonalElevation = 0.dp,
                    containerColor = Color(0xCCFCFAF5)
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = state.currentTab == tab,
                            onClick = { viewModel.switchTab(tab) },
                            icon = { Text(tab.title.take(1)) },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (state.currentTab) {
                    AppTab.Chat -> ChatScreen(
                        state = state,
                        onInputChange = viewModel::updateChatInput,
                        onSend = viewModel::sendMessage,
                        onStop = viewModel::stopStreaming,
                        onDismissMessage = viewModel::clearInlineMessage
                    )
                    AppTab.Settings -> SettingsScreen(
                        state = state,
                        onBaseUrlChange = viewModel::updateBaseUrl,
                        onTemperatureChange = viewModel::updateTemperature,
                        onMaxTokensChange = viewModel::updateMaxTokens,
                        onSave = viewModel::saveSettings,
                        onTestConnection = viewModel::testConnection,
                        onRefreshModels = viewModel::refreshModels,
                        onSwitchModel = viewModel::switchModel,
                        onDismissMessage = viewModel::clearInlineMessage
                    )
                }
            }
        }
    }
}
