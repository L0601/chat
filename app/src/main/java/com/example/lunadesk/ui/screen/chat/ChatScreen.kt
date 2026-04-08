package com.example.lunadesk.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
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
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ChatHeader(state = state)

        state.inlineMessage?.let {
            InlineNotice(message = it, onDismiss = onDismissMessage)
        }

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xF9FFFDF8)
        ) {
            if (state.messages.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
            onStop = onStop
        )
    }
}

@Composable
private fun ChatHeader(state: LunaDeskUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xD92A4B45), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
    onStop: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFFF9F6EC),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .onPreviewKeyEvent { event ->
                        val shouldSend = event.type == KeyEventType.KeyUp &&
                            event.key == Key.Enter &&
                            !event.isShiftPressed
                        if (shouldSend && !state.isSending && state.chatInput.isNotBlank()) {
                            onSend()
                            true
                        } else {
                            false
                        }
                    },
                value = state.chatInput,
                onValueChange = onInputChange,
                minLines = 2,
                maxLines = 6,
                shape = RoundedCornerShape(24.dp),
                label = { Text("输入消息") },
                placeholder = { Text("输入问题后直接发送") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (!state.isSending && state.chatInput.isNotBlank()) onSend()
                    }
                ),
                supportingText = {
                    Text(if (state.isSending) "正在生成回答" else "Enter 发送，Shift+Enter 换行")
                }
            )
            FilledIconButton(
                onClick = if (state.isSending) onStop else onSend,
                enabled = state.isSending || state.chatInput.isNotBlank(),
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.Transparent, CircleShape)
            ) {
                Text(if (state.isSending) "停" else "发")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
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
            shape = RoundedCornerShape(24.dp),
            color = background,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .heightIn(min = 56.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
