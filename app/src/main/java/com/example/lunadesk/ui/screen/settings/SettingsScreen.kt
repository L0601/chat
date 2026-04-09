package com.example.lunadesk.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lunadesk.data.model.ModelInfo
import com.example.lunadesk.ui.LunaDeskUiState

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: LunaDeskUiState,
    onBack: () -> Unit,
    onResetConversation: () -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onTemperatureChange: (String) -> Unit,
    onMaxTokensChange: (String) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onRefreshModels: () -> Unit,
    onSwitchModel: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Header(onBack = onBack)
        ActionCard(
            hasMessages = state.messages.isNotEmpty() || state.chatInput.isNotBlank(),
            onResetConversation = onResetConversation
        )
        ConfigCard(
            state = state,
            onBaseUrlChange = onBaseUrlChange,
            onTemperatureChange = onTemperatureChange,
            onMaxTokensChange = onMaxTokensChange,
            onSave = onSave,
            onTestConnection = onTestConnection,
            onRefreshModels = onRefreshModels
        )
        ModelListCard(
            modifier = Modifier.weight(1f),
            state = state,
            onSwitchModel = onSwitchModel
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(18.dp),
            colors = lightButtonColors()
        ) {
            Text("<")
        }
        Text("设置", style = MaterialTheme.typography.titleLarge, color = Color(0xFF223732))
        Button(
            onClick = {},
            enabled = false,
            shape = RoundedCornerShape(18.dp),
            colors = lightButtonColors()
        ) {
            Text(" ")
        }
    }
}

@Composable
private fun ActionCard(
    hasMessages: Boolean,
    onResetConversation: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("当前会话", style = MaterialTheme.typography.titleMedium)
                Text("清空聊天记录与输入框内容。", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onResetConversation, enabled = hasMessages) {
                Text("重置上下文")
            }
        }
    }
}

@Composable
private fun ConfigCard(
    state: LunaDeskUiState,
    onBaseUrlChange: (String) -> Unit,
    onTemperatureChange: (String) -> Unit,
    onMaxTokensChange: (String) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onRefreshModels: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("LM Studio 地址") },
                placeholder = { Text("http://192.168.31.30:1234") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.temperatureInput,
                    onValueChange = onTemperatureChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("温度") }
                )
                OutlinedTextField(
                    value = state.maxTokensInput,
                    onValueChange = onMaxTokensChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("最大输出") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Text("保存")
                }
                Button(
                    onClick = onTestConnection,
                    enabled = !state.isTestingConnection,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.isTestingConnection) "测试中" else "测试")
                }
                Button(
                    onClick = onRefreshModels,
                    enabled = !state.isLoadingModels,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.isLoadingModels) "拉取中" else "拉取")
                }
            }
        }
    }
}

@Composable
private fun ModelListCard(
    modifier: Modifier = Modifier,
    state: LunaDeskUiState,
    onSwitchModel: (String) -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF7FFFDF8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("模型列表", style = MaterialTheme.typography.titleMedium)
                Text("${state.models.size} 个", style = MaterialTheme.typography.bodySmall)
            }
            if (state.isSwitchingModel) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (state.models.isEmpty()) {
                Text("暂未获取到模型，请先点击“拉取”。")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.models, key = { it.id }) { model ->
                        ModelRow(
                            model = model,
                            selected = model.id == state.selectedModel,
                            isSwitching = state.switchingModelId == model.id,
                            onClick = { onSwitchModel(model.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelRow(
    model: ModelInfo,
    selected: Boolean,
    isSwitching: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        selected -> Color(0xFFE0ECE5)
        isSwitching -> Color(0xFFF7E8C8)
        else -> Color(0xFFF7F1E0)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        modifier = Modifier.clickable(enabled = !selected && !isSwitching, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = model.id,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = when {
                    isSwitching -> "切换中"
                    selected -> "当前"
                    else -> "待选"
                },
                color = Color(0xFF2A4B45),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun lightButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color(0xF6FFFFFF),
    contentColor = Color(0xFF1E2A27),
    disabledContainerColor = Color(0xE8FFFFFF),
    disabledContentColor = Color(0xFF1E2A27)
)
