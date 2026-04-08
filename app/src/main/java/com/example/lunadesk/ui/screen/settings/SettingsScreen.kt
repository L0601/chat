package com.example.lunadesk.ui.screen.settings

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
import com.example.lunadesk.ui.ModelFilter

@Composable
fun SettingsScreen(
    state: LunaDeskUiState,
    onBaseUrlChange: (String) -> Unit,
    onModelSearchQueryChange: (String) -> Unit,
    onModelFilterChange: (ModelFilter) -> Unit,
    onTemperatureChange: (String) -> Unit,
    onMaxTokensChange: (String) -> Unit,
    onSave: () -> Unit,
    onTestConnection: () -> Unit,
    onRefreshModels: () -> Unit,
    onSwitchModel: (String) -> Unit,
    onDismissMessage: () -> Unit
) {
    val visibleModels = visibleModels(
        models = state.models,
        selectedModel = state.selectedModel,
        query = state.modelSearchQuery,
        filter = state.modelFilter
    )

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
            visibleModels = visibleModels,
            onModelSearchQueryChange = onModelSearchQueryChange,
            onModelFilterChange = onModelFilterChange,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onRefreshModels,
                    enabled = !state.isLoadingModels,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.isLoadingModels) "刷新中" else "拉取模型")
                }
                Text(
                    text = state.connectionStatus ?: "尚未测试连接",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF39534B)
                )
            }
        }
    }
}

@Composable
private fun ModelListCard(
    state: LunaDeskUiState,
    visibleModels: List<ModelInfo>,
    onModelSearchQueryChange: (String) -> Unit,
    onModelFilterChange: (ModelFilter) -> Unit,
    onSwitchModel: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize(),
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
            Text("共 ${state.models.size} 个模型，当前显示 ${visibleModels.size} 个。")
            if (state.isSwitchingModel) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                value = state.modelSearchQuery,
                onValueChange = onModelSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("搜索模型") },
                placeholder = { Text("按模型名筛选") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModelFilter.entries.forEach { filter ->
                    Button(
                        onClick = { onModelFilterChange(filter) },
                        modifier = Modifier.weight(1f),
                        enabled = state.modelFilter != filter
                    ) {
                        Text(filter.title)
                    }
                }
            }
            if (visibleModels.isEmpty()) {
                Text(
                    if (state.models.isEmpty()) "暂未获取到模型，请先点击“拉取模型”。" else "没有匹配的模型。"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleModels, key = { it.id }) { model ->
                        ModelRow(
                            model = model,
                            selected = model.id == state.selectedModel,
                            isSwitching = state.switchingModelId == model.id,
                            onSwitchModel = onSwitchModel
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
            Text("连接局域网模型服务、筛选模型并完成切换。")
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
    onSwitchModel: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFE0ECE5) else Color(0xFFF7F1E0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(model.id, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = when {
                        selected -> "当前模型"
                        model.ownedBy != null -> "来源：${model.ownedBy}"
                        else -> "可切换模型"
                    }
                )
            }
            Button(
                onClick = { onSwitchModel(model.id) },
                enabled = !isSwitching && !selected
            ) {
                Text(
                    when {
                        isSwitching -> "切换中"
                        selected -> "已选中"
                        else -> "切换"
                    }
                )
            }
        }
    }
}

private fun visibleModels(
    models: List<ModelInfo>,
    selectedModel: String,
    query: String,
    filter: ModelFilter
): List<ModelInfo> {
    val keyword = query.trim()
    val filtered = models.filter { model ->
        val matchesKeyword = keyword.isBlank() || model.id.contains(keyword, ignoreCase = true)
        val matchesFilter = when (filter) {
            ModelFilter.All -> true
            ModelFilter.Current -> model.id == selectedModel
            ModelFilter.Switchable -> model.id != selectedModel
        }
        matchesKeyword && matchesFilter
    }
    val selected = filtered.firstOrNull { it.id == selectedModel }
    val others = filtered.filterNot { it.id == selectedModel }
    return listOfNotNull(selected) + others
}
