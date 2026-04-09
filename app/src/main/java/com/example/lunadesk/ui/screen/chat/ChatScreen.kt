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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.material3.TextFieldDefaults
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
    modifier: Modifier = Modifier,
    state: LunaDeskUiState,
    onOpenMenu: () -> Unit,
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
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChatHeader(state = state, onOpenMenu = onOpenMenu)

        state.inlineMessage?.let {
            InlineNotice(message = it, onDismiss = onDismissMessage)
        }

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xF7FFFDF9)
        ) {
            if (state.messages.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
private fun ChatHeader(
    state: LunaDeskUiState,
    onOpenMenu: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onOpenMenu,
            modifier = Modifier
                .width(54.dp)
                .height(46.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ghostButtonColors()
        ) {
            Text("≡", style = MaterialTheme.typography.titleMedium)
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xF6FFFFFF)
        ) {
            Text(
                text = state.connectionStatus ?: "准备开始新对话",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color(0xFF2E6DB8),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .width(54.dp)
                .height(46.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ghostButtonColors()
        ) {
            Text("${state.messages.count { it.content.isNotBlank() }}")
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xF5FFFDF8),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .width(44.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ghostButtonColors()
            ) {
                Text("+")
            }

            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                value = state.chatInput,
                onValueChange = onInputChange,
                minLines = 1,
                maxLines = 4,
                shape = RoundedCornerShape(18.dp),
                placeholder = { Text("问问 LunaDesk") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Button(
                onClick = if (state.isSending) onStop else onSend,
                enabled = state.isSending || state.chatInput.isNotBlank(),
                modifier = Modifier
                    .width(52.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isSending) Color(0xFFB55D48) else Color(0xFF111111),
                    contentColor = Color.White
                )
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
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("有什么可以帮忙的？", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "左上角打开侧边栏进入设置，连接局域网模型服务后即可开始对话。",
            textAlign = TextAlign.Center,
            color = Color(0xFF50625C),
            modifier = Modifier.padding(top = 10.dp)
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
            shape = RoundedCornerShape(22.dp),
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

@Composable
private fun ghostButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Color(0xF6FFFFFF),
    contentColor = Color(0xFF1E2A27),
    disabledContainerColor = Color(0xE8FFFFFF),
    disabledContentColor = Color(0xFF1E2A27)
)
