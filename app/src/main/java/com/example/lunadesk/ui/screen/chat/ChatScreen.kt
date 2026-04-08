package com.example.lunadesk.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lunadesk.data.model.ChatMessageUi
import com.example.lunadesk.ui.LunaDeskUiState

@Composable
fun ChatScreen(
    state: LunaDeskUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onDismissMessage: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeaderCard(
            title = state.selectedModel.ifBlank { "未选择模型" },
            subtitle = state.connectionStatus ?: "流式响应优先，非流式自动降级"
        )

        StatusSummary(state = state)

        state.inlineMessage?.let {
            InlineNotice(message = it, onDismiss = onDismissMessage)
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF7FFFDF8))
        ) {
            if (state.messages.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(message = message)
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F6EC))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.chatInput,
                    onValueChange = onInputChange,
                    minLines = 3,
                    maxLines = 6,
                    label = { Text("输入消息") },
                    placeholder = { Text("例如：请总结一下当前模型服务的状态") },
                    supportingText = {
                        Text("当前消息数：${state.messages.count { it.content.isNotBlank() }}")
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSend,
                        enabled = !state.isSending && state.chatInput.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (state.isSending) "生成中" else "发送")
                    }
                    TextButton(
                        onClick = onStop,
                        enabled = state.isSending,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("停止")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusSummary(state: LunaDeskUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0EEE4), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "消息 ${state.messages.count { it.content.isNotBlank() }}",
            color = Color(0xFF31433D)
        )
        Text(
            text = if (state.isSending) "生成中" else "空闲",
            color = if (state.isSending) Color(0xFF8E5C4A) else Color(0xFF39534B)
        )
    }
}

@Composable
private fun HeaderCard(title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xE52A4B45))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text(subtitle, color = Color(0xFFE8F5F2))
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("LunaDesk", style = MaterialTheme.typography.headlineMedium)
        Text("先在设置页配置服务地址和模型，再开始流式对话。")
    }
}

@Composable
private fun MessageBubble(message: ChatMessageUi) {
    val isUser = message.role == "user"
    val background = if (isUser) Color(0xFFDDECF1) else Color(0xFFF5E8C7)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = background,
            tonalElevation = 0.dp,
            modifier = Modifier.heightIn(min = 56.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isUser) "你" else "模型",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF31433D)
                )
                Text(
                    text = message.content.ifBlank {
                        if (message.isStreaming) "正在生成..." else "暂无内容"
                    },
                    color = Color(0xFF21302D)
                )
                message.errorText?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun InlineNotice(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFCECC8), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) {
            Text("关闭")
        }
    }
}
