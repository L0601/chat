package com.example.lunadesk.ui.screen.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.lunadesk.data.model.ChatMessageUi
import com.example.lunadesk.ui.LunaDeskUiState
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin

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
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
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
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onOpenMenu,
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ghostButtonColors(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(2.dp)
                            .background(Color(0xFF1E2A27), RoundedCornerShape(1.dp))
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xF6FFFFFF)
        ) {
            Text(
                text = resolveHeaderTitle(state),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                color = Color(0xFF2E6DB8),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }

        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xF6FFFFFF)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "${state.messages.count { it.content.isNotBlank() }}",
                    color = Color(0xFF1E2A27),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val submitMessage: () -> Unit = {
        if (state.isSending) {
            onStop()
        } else {
            onSend()
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
        Unit
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
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
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                value = state.chatInput,
                onValueChange = onInputChange,
                minLines = 1,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitMessage() }),
                shape = RoundedCornerShape(18.dp),
                placeholder = {
                    Text(
                        text = "问问 LunaDesk",
                        color = Color(0xFF8A938F)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    disabledContainerColor = Color(0xFFF1F3F5),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = Color(0xFF223732)
                )
            )

            Button(
                onClick = submitMessage,
                enabled = state.isSending || state.chatInput.isNotBlank(),
                modifier = Modifier
                    .width(56.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isSending) Color(0xFFB55D48) else Color(0xFF111111),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFD5D8DB),
                    disabledContentColor = Color(0xFF7E868A)
                )
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.isSending) "停" else "发",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
    val background = if (isUser) Color(0xFFD8E9F4) else Color(0xFFF8EFD6)
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = background,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.9f else 0.96f)
                .heightIn(min = 48.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isUser) "你" else "Luna",
                            color = if (isUser) Color(0xFF35546B) else Color(0xFF6A5632),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (message.content.isNotBlank()) {
                            CopyBubbleButton(
                                onClick = {
                                    copyMessage(context, message.content)
                                }
                            )
                        }
                    }
                    MarkdownText(
                        markdown = message.content.ifBlank {
                            if (message.isStreaming) "正在生成..." else "暂无内容"
                        },
                        isUser = isUser
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
private fun MarkdownText(markdown: String, isUser: Boolean) {
    val context = LocalContext.current
    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(JLatexMathPlugin.create(44f) { builder ->
                builder.inlinesEnabled(true)
            })
            .build()
    }
    val textColor = if (isUser) "#203847" else "#3A3224"

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(android.graphics.Color.parseColor(textColor))
                textSize = 16f
                setLineSpacing(0f, 1.4f)
                includeFontPadding = false
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, normalizeMarkdown(markdown))
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CopyBubbleButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(28.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xF4FFFFFF),
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .offset(x = 2.dp, y = (-2).dp)
                    .border(1.2.dp, Color(0xFF42504B), RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .offset(x = (-2).dp, y = 2.dp)
                    .border(1.2.dp, Color(0xFF42504B), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun InlineNotice(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFCECC8), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            message,
            modifier = Modifier.weight(1f),
            color = Color(0xFF5B4C1F),
            style = MaterialTheme.typography.bodyMedium
        )
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

private fun normalizeMarkdown(markdown: String): String {
    val normalizedLayout = markdown
        .replace(Regex("(?m)(?<!\\n)(#{1,6}\\s+)"), "\n\n$1")
        .replace(Regex("(?m)(?<!\\n)(---+)"), "\n\n$1")
        .replace(Regex("(?m)(?<!\\n)([-*]\\s+)"), "\n$1")
        .replace(Regex("(?m)(?<!\\n)(\\d+\\.\\s+)"), "\n$1")
        .replace(Regex("(?m)```"), "\n```")
        .replace(Regex("(?m)^>(.+)$"), "\n> $1")
        .replace(Regex("\\n{3,}"), "\n\n")

    return INLINE_MATH_REGEX.replace(normalizedLayout) { match ->
        val value = match.groupValues[1].trim()
        if (value.isEmpty()) {
            match.value
        } else {
            "$$$$${value}$$$$"
        }
    }
}

private fun copyMessage(context: Context, content: String) {
    if (content.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("chat_message", content))
    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
}

private val INLINE_MATH_REGEX = Regex("(?<!\\\\)\\$([^$\\n]+?)(?<!\\\\)\\$")

private fun resolveHeaderTitle(state: LunaDeskUiState): String {
    return when {
        state.isSending -> "生成中"
        state.selectedModel.isNotBlank() -> state.selectedModel
        state.connectionStatus?.isNotBlank() == true -> state.connectionStatus
        else -> "准备开始新对话"
    }
}
