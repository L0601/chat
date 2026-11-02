package com.example.lunadesk.ui.screen.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lunadesk.data.model.CallMetrics
import com.example.lunadesk.data.model.ChatMessageUi
import com.example.lunadesk.data.model.aggregateMetrics
import java.util.Locale

private fun seconds(ms: Double) = String.format(Locale.ROOT, "%.1f 秒", ms / 1000)
private fun rate(value: Double) = String.format(Locale.ROOT, "%.1f token/秒", value)
private fun CallMetrics.speed(): Double? = if ((outputTokens ?: 0) > 0 && (outputMs ?: 0) >= 100) outputTokens!! * 1000.0 / outputMs!! else null

@Composable
internal fun MessageMetrics(metrics: CallMetrics?) {
    if (metrics == null) return
    val parts = buildList {
        metrics.inputTokens?.let { add("输入 $it") }
        metrics.outputTokens?.let { add("输出 $it token") }
        metrics.speed()?.let { add("速度 ${rate(it)}（估算）") }
        metrics.firstTextMs?.let { add("首字 ${seconds(it.toDouble())}") }
    }
    if (parts.isNotEmpty()) Text((if (metrics.completed) "" else "未完成 · ") + parts.joinToString(" · "), modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationMetricsHeader(messages: List<ChatMessageUi>) {
    val metrics = remember(messages) { aggregateMetrics(messages) }
    if (metrics.inputTokens == null && metrics.outputTokens == null && metrics.speed == null && metrics.firstTextMs == null) return
    var expanded by remember { mutableStateOf(false) }
    val summary = buildList {
        metrics.inputTokens?.let { add("输入 $it") }
        metrics.outputTokens?.let { add("输出 $it") }
        metrics.speed?.let { add(rate(it)) }
        if (isEmpty()) metrics.firstTextMs?.let { add("首字 ${seconds(it)}") }
    }
    Text("本会话  ${summary.joinToString(" · ")}  ›", modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(start = 64.dp, end = 16.dp, bottom = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    if (expanded) ModalBottomSheet(onDismissRequest = { expanded = false }) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("会话统计", style = MaterialTheme.typography.titleLarge)
            metrics.inputTokens?.let { Text("输入 $it token（${metrics.inputCount} 次）") }
            metrics.outputTokens?.let { Text("输出 $it token（${metrics.outputCount} 次）") }
            metrics.speed?.let { Text("加权速度 ${rate(it)}（${metrics.speedCount} 次，估算）") }
            metrics.firstTextMs?.let { Text("加权首字 ${seconds(it)}（${metrics.firstTextCount} 次）") }
            if (metrics.inputCount < metrics.completedCount || metrics.outputCount < metrics.completedCount || messages.any { it.role == "assistant" && !it.isStreaming && it.metrics == null }) Text("部分调用没有返回用量数据", style = MaterialTheme.typography.bodySmall)
            if (metrics.mixedModels) Text("此会话包含多个模型", style = MaterialTheme.typography.bodySmall)
            Text("速度按输出 token 与输出时长汇总计算；首字按输入 token 加权。速度受网络分块和推理 token 影响。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
