package com.example.lunadesk.ui.screen.chat

import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.lunadesk.data.model.ChatMessageUi
import com.example.lunadesk.ui.LunaDeskUiState
import io.noties.markwon.Markwon

@Composable
fun ChatScreen(
    state: LunaDeskUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onDismissMessage: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChatHeader(state = state)

        state.inlineMessage?.let {
            InlineNotice(message = it, onDismiss = onDismissMessage)
        }

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF9FFFDF8)
        ) {
            if (state.messages.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(message = message)
                    }
                }
            }
        }

        ComposerBar(
            state = state,
            onInputChange = onInputChange,
            onSend = onSend,
            onStop = onStop,
            onReset = onReset
        )
    }
}

@Composable
private fun ChatHeader(state: LunaDeskUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xD92A4B45), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = state.selectedModel.ifBlank { "未选择模型" },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = state.connectionStatus ?: "准备开始新对话",
                color = Color(0xFFE4F2EE),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = "${state.messages.count { it.content.isNotBlank() }} 条",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun ComposerBar(
    state: LunaDeskUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFF9F6EC),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onReset,
                enabled = state.messages.isNotEmpty() || state.chatInput.isNotBlank(),
                modifier = Modifier
                    .width(68.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE8D8A9),
                    contentColor = Color(0xFF2A4B45)
                )
            ) {
                Text("重置")
            }

            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = state.chatInput,
                onValueChange = onInputChange,
                minLines = 1,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                label = { Text("输入消息") },
                placeholder = { Text("输入问题后直接发送") }
            )

            Button(
                onClick = if (state.isSending) onStop else onSend,
                enabled = state.isSending || state.chatInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isSending) Color(0xFFB55D48) else Color(0xFF2A4B45),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .width(72.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (state.isSending) "停止" else "发送")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("LunaDesk", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "先在设置页连接局域网模型服务，然后在这里开始对话。",
            textAlign = TextAlign.Center,
            color = Color(0xFF50625C)
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessageUi) {
    val isUser = message.role == "user"
    val background = if (isUser) Color(0xFFDCECF6) else Color(0xFFF6EBCF)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = background,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(min = 48.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MarkdownText(
                        markdown = message.content.ifBlank {
                            if (message.isStreaming) "正在生成..." else "暂无内容"
                        }
                    )
                    message.errorText?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownText(markdown: String) {
    val context = LocalContext.current
    val markwon = remember(context) { Markwon.create(context) }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(android.graphics.Color.parseColor("#21302D"))
                textSize = 15f
                setLineSpacing(0f, 1.2f)
                includeFontPadding = false
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, markdown)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InlineNotice(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFCECC8), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) {
            Text("关闭")
        }
    }
}
