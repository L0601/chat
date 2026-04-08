package com.example.lunadesk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { tab ->
                    val selected = state.currentTab == tab
                    Button(
                        onClick = { viewModel.switchTab(tab) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Color(0xFF2A4B45) else Color(0xFFF3E5BA),
                            contentColor = if (selected) Color.White else Color(0xFF2A4B45)
                        )
                    ) {
                        Text(tab.title)
                    }
                }
            }

            when (state.currentTab) {
                AppTab.Chat -> ChatScreen(
                    state = state,
                    onInputChange = viewModel::updateChatInput,
                    onSend = viewModel::sendMessage,
                    onStop = viewModel::stopStreaming,
                    onReset = viewModel::resetConversation,
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
                    onSwitchModel = viewModel::switchModel
                )
            }
        }
    }
}
