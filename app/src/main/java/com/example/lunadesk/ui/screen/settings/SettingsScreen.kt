package com.example.lunadesk.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lunadesk.data.model.ModelInfo
import com.example.lunadesk.ui.LunaDeskUiState

@Composable
fun SettingsScreen(
    state: LunaDeskUiState,
    onBaseUrlChange: (String) -> Unit,
    onTemperatureChange: (String) -> Unit,
    onMaxTokensChange: (String) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onRefreshModels: () -> Unit,
    onSwitchModel: (String) -> Unit,
    onDismissMessage: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Header()

        state.inlineMessage?.let {
            InlineMessage(message = it, onDismiss = onDismissMessage)
        }

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
            state = state,
            onSwitchModel = onSwitchModel
        )
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
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = onBaseUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("LM Studio 地址") },
                placeholder = { Text("http://192.168.31.30:1234") },
                supportingText = { Text("覆盖安装后会继续保留这份配置") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.temperatureInput,
                    onValueChange = onTemperatureChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("温度") }
                )
                OutlinedTextField(
                    value = state.maxTokensInput,
                    onValueChange = onMaxTokensChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("最大输出") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Text("保存配置")
                }
                Button(
                    onClick = onTestConnection,
                    enabled = !state.isTestingConnection,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.isTestingConnection) "测试中" else "连接测试")
                }
            }
            Button(
                onClick = onRefreshModels,
                enabled = !state.isLoadingModels,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isLoadingModels) "拉取中" else "拉取模型")
            }
        }
    }
}

@Composable
private fun ModelListCard(
    state: LunaDeskUiState,
    onSwitchModel: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF7FFFDF8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("模型列表", style = MaterialTheme.typography.titleLarge)
            Text(state.connectionStatus ?: "拉取后直接展示完整模型列表")
            if (state.isSwitchingModel) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (state.models.isEmpty()) {
                Text("暂未获取到模型，请先点击“拉取模型”。")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
private fun Header() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD7E5D5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("LunaDesk 设置", style = MaterialTheme.typography.headlineSmall)
            Text("拉取模型后直接点选目标模型。")
        }
    }
}

@Composable
private fun InlineMessage(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(message, modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) {
            Text("关闭")
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        modifier = Modifier.clickable(enabled = !selected && !isSwitching, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(model.id, style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        isSwitching -> "切换中..."
                        selected -> "当前模型"
                        else -> "点击切换到该模型"
                    }
                )
            }
            Text(
                text = when {
                    isSwitching -> "..."
                    selected -> "已选"
                    else -> "选择"
                },
                color = Color(0xFF2A4B45),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
