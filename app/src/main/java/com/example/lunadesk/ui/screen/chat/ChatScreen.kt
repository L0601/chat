package com.example.lunadesk.ui.screen.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.TextView
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.lunadesk.data.model.ChatMessageUi
import com.example.lunadesk.ui.LunaDeskUiState
import com.example.lunadesk.ui.components.showAppToast
import com.example.lunadesk.ui.theme.LocalAppColors
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
    onReset: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()

    // 使用 reverseLayout 后，index 0 = 最新消息（底部锚定）
    // 仅在用户手动滚动结束时更新标记，避免插入新消息导致误判
    var shouldAutoScroll by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (!scrolling) {
                    shouldAutoScroll = listState.firstVisibleItemIndex <= 1
                }
            }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isEmpty() || !shouldAutoScroll) return@LaunchedEffect
        listState.animateScrollToItem(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChatHeader(state = state, onOpenMenu = onOpenMenu, onReset = onReset)

        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(30.dp),
            color = colors.surfaceChat
        ) {
            if (state.messages.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    state = listState,
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.messages.asReversed(), key = { it.id }) { message ->
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
    onOpenMenu: () -> Unit,
    onReset: () -> Unit
) {
    val colors = LocalAppColors.current
    val haptic = LocalHapticFeedback.current

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
                            .background(colors.iconPrimary, RoundedCornerShape(1.dp))
                    )
                }
            }
        }

        HeaderStatusPill(
            status = resolveHeaderStatus(state),
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onReset()
            },
            modifier = Modifier.height(44.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ghostButtonColors(),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(
                text = "重置",
                color = colors.textOnButton,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
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
    val colors = LocalAppColors.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val submitMessage: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
        color = colors.surfaceComposer,
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
                        color = colors.textPlaceholder
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceInput,
                    unfocusedContainerColor = colors.surfaceInput,
                    disabledContainerColor = colors.surfaceInputDisabled,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = colors.cursor
                )
            )

            Button(
                onClick = submitMessage,
                enabled = state.isSending || state.chatInput.isNotBlank(),
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isSending) colors.buttonStop else colors.buttonSend,
                    contentColor = Color.White,
                    disabledContainerColor = colors.buttonDisabledBg,
                    disabledContentColor = colors.buttonDisabledContent
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (state.isSending) {
                        Icons.Default.Stop
                    } else {
                        Icons.AutoMirrored.Filled.Send
                    },
                    contentDescription = if (state.isSending) "停止" else "发送",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val colors = LocalAppColors.current
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
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessageUi) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val showActions = message.content.isNotBlank() && !message.isStreaming
    val onCopy = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        copyMessage(context, message.content)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isUser) {
            UserBubble(message = message)
        } else {
            AssistantContent(message = message)
        }
        if (showActions) {
            MessageActions(onCopy = onCopy)
        }
    }
}

@Composable
private fun UserBubble(message: ChatMessageUi) {
    val colors = LocalAppColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.bubbleUser,
        tonalElevation = 0.dp,
        modifier = Modifier.widthIn(max = 300.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MarkdownText(
                markdown = message.content.ifBlank { "暂无内容" },
                isUser = true
            )
            message.errorText?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AssistantContent(message: ChatMessageUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (message.isStreaming && message.content.isBlank()) {
            ThreeDotLoading()
        } else {
            MarkdownText(
                markdown = message.content.ifBlank { "暂无内容" },
                isUser = false
            )
            if (message.isStreaming && message.content.isNotBlank()) {
                BlinkingCursor()
            }
        }
        message.errorText?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun MessageActions(onCopy: () -> Unit) {
    val colors = LocalAppColors.current
    IconButton(
        onClick = onCopy,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.ContentCopy,
            contentDescription = "复制",
            modifier = Modifier.size(16.dp),
            tint = colors.iconCopyBorder
        )
    }
}

@Composable
private fun ThreeDotLoading() {
    val transition = rememberInfiniteTransition(label = "dots")
    val dotCount by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "dotCount"
    )
    val dots = ".".repeat(dotCount.toInt().coerceIn(0, 3))
    Text(
        text = "生成中$dots",
        color = LocalAppColors.current.textTertiary,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursorAlpha"
    )
    Text(
        text = "▌",
        modifier = Modifier.alpha(cursorAlpha),
        color = LocalAppColors.current.textPrimary,
        fontSize = 16.sp
    )
}

@Composable
private fun MarkdownText(markdown: String, isUser: Boolean) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(JLatexMathPlugin.create(44f) { builder ->
                builder.inlinesEnabled(true)
            })
            .build()
    }
    val textColorInt = if (isUser) colors.bubbleUserText.toArgb() else colors.bubbleAssistantText.toArgb()

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(textColorInt)
                textSize = 16f
                setLineSpacing(0f, 1.4f)
                includeFontPadding = false
            }
        },
        update = { textView ->
            textView.setTextColor(textColorInt)
            markwon.setMarkdown(textView, normalizeMarkdown(markdown))
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ghostButtonColors() = run {
    val colors = LocalAppColors.current
    ButtonDefaults.buttonColors(
        containerColor = colors.surfaceOverlay,
        contentColor = colors.textOnButton,
        disabledContainerColor = colors.surfaceOverlayDisabled,
        disabledContentColor = colors.textOnButton
    )
}

private fun copyMessage(context: Context, content: String) {
    if (content.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("chat_message", content))
    showAppToast(context, "已复制")
}

private enum class HeaderTone { Idle, Generating, Normal, Error }

private data class HeaderStatus(val text: String, val tone: HeaderTone)

private fun resolveHeaderStatus(state: LunaDeskUiState): HeaderStatus {
    val status = state.connectionStatus
    val isError = status != null && (status.contains("失败") || status.contains("错误"))
    return when {
        state.isSending -> HeaderStatus("生成中", HeaderTone.Generating)
        isError -> HeaderStatus(status!!, HeaderTone.Error)
        state.selectedModel.isNotBlank() -> HeaderStatus(state.selectedModel, HeaderTone.Normal)
        !status.isNullOrBlank() -> HeaderStatus(status, HeaderTone.Normal)
        else -> HeaderStatus("", HeaderTone.Idle)
    }
}

@Composable
private fun HeaderStatusPill(status: HeaderStatus, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val (bg, fg) = when (status.tone) {
        HeaderTone.Generating -> colors.surfaceOverlay to colors.textAccent
        HeaderTone.Error -> colors.noticeBg to colors.noticeText
        HeaderTone.Normal -> colors.surfaceOverlay to colors.textPrimary
        HeaderTone.Idle -> Color.Transparent to Color.Transparent
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status.tone == HeaderTone.Generating) {
                PulsingDot(color = fg)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = status.text,
                color = fg,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (status.tone == HeaderTone.Normal) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseScale"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .alpha(scale)
            .background(color, RoundedCornerShape(4.dp))
    )
}
