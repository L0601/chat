package com.example.lunadesk.ui.screen.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.TextView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.lunadesk.data.model.ChatMessageUi
import com.example.lunadesk.ui.LunaDeskUiState
import com.example.lunadesk.ui.components.showAppToast
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

private const val AUTO_SCROLL_DURATION_MS = 250

internal data class AutoFollowState(
    val pausedGenerationId: String? = null
) {
    fun pauseFor(generationId: String?): AutoFollowState {
        return if (generationId == null) this else copy(pausedGenerationId = generationId)
    }

    fun shouldFollow(generationId: String): Boolean {
        return pausedGenerationId != generationId
    }
}

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    state: LunaDeskUiState,
    onOpenMenu: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    val listState = rememberLazyListState()
    val autoScroll = rememberAutoScrollController(listState)
    val streamingMessage = state.messages.lastOrNull { it.isStreaming }
    var followState by remember { mutableStateOf(AutoFollowState()) }
    val isAtBottom by remember(state.messages.size) {
        derivedStateOf { listState.isAtBottom(state.messages.size) }
    }

    LaunchedEffect(listState, streamingMessage?.id) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start && streamingMessage != null) {
                followState = followState.pauseFor(streamingMessage.id)
                autoScroll.cancelForUser()
            }
        }
    }

    LaunchedEffect(
        state.messages.size,
        streamingMessage?.content?.length,
        streamingMessage?.id
    ) {
        val message = streamingMessage ?: return@LaunchedEffect
        if (followState.shouldFollow(message.id)) autoScroll.request(state.messages.size)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
    ) {
        ChatHeader(state = state, onOpenMenu = onOpenMenu, onReset = onReset)
        MessageArea(
            modifier = Modifier.weight(1f),
            state = state,
            listState = listState,
            showLatestButton = state.messages.isNotEmpty() && !isAtBottom,
            onShowLatest = { autoScroll.request(state.messages.size) },
            onOpenMenu = onOpenMenu
        )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenMenu) {
            Icon(Icons.Default.Menu, contentDescription = "打开导航菜单")
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isSending) GeneratingDot()
                Text(
                    state.activeProfile?.name ?: "LunaDesk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                state.selectedModel.ifBlank { "尚未选择模型" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onReset, enabled = state.messages.isNotEmpty()) {
            Icon(Icons.Outlined.DeleteSweep, contentDescription = "重置当前对话")
        }
    }
}

@Composable
private fun MessageArea(
    modifier: Modifier,
    state: LunaDeskUiState,
    listState: LazyListState,
    showLatestButton: Boolean,
    onShowLatest: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {
        if (state.messages.isEmpty()) {
            EmptyState(onOpenMenu)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageBlock(message)
                }
                item(key = "_bottom_anchor") { Spacer(Modifier.height(1.dp)) }
            }
        }
        if (showLatestButton) {
            FilledTonalButton(
                onClick = onShowLatest,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("查看最新")
            }
        }
    }
}

@Composable
private fun EmptyState(onOpenMenu: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "有什么可以帮忙的？",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            "选择一套 API 配置和模型，然后开始对话。",
            modifier = Modifier.padding(top = 10.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onOpenMenu, modifier = Modifier.padding(top = 20.dp)) {
            Text("选择 API 配置")
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
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val submit: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (state.isSending) onStop() else {
            onSend()
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
        Unit
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.chatInput,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp),
                minLines = 1,
                maxLines = 4,
                placeholder = { Text("问问 LunaDesk") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                shape = RoundedCornerShape(22.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
            Button(
                onClick = submit,
                enabled = state.isSending || state.chatInput.isNotBlank(),
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isSending) {
                        MaterialTheme.colorScheme.error
                    } else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    if (state.isSending) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (state.isSending) "停止生成" else "发送消息",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBlock(message: ChatMessageUi) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val copy = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        copyMessage(context, message.content)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .then(if (isUser) Modifier.widthIn(max = 320.dp) else Modifier.fillMaxWidth())
                .combinedClickable(onClick = {}, onLongClick = copy)
        ) {
            if (isUser) UserMessage(message) else AssistantMessage(message)
            if (message.content.isNotBlank() && !message.isStreaming) {
                CopyButton(onClick = copy, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

@Composable
private fun UserMessage(message: ChatMessageUi) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        MessageText(
            message = message,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(start = 14.dp, top = 11.dp, end = 48.dp, bottom = 11.dp)
        )
    }
}

@Composable
private fun AssistantMessage(message: ChatMessageUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 46.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (message.isStreaming && message.content.isBlank()) {
            ThreeDotLoading()
        } else {
            MessageText(message, MaterialTheme.colorScheme.onSurface)
            if (message.isStreaming) BlinkingCursor()
        }
    }
}

@Composable
private fun MessageText(
    message: ChatMessageUi,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SelectionContainer {
            if (message.isStreaming) {
                Text(
                    message.content.ifBlank { "暂无内容" },
                    color = color,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                MarkdownText(message.content.ifBlank { "暂无内容" }, color)
            }
        }
        message.errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun CopyButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.ContentCopy,
                contentDescription = "复制消息",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MarkdownText(markdown: String, color: Color) {
    val context = LocalContext.current
    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(JLatexMathPlugin.create(44f) { it.inlinesEnabled(true) })
            .build()
    }
    val colorInt = color.toArgb()
    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(colorInt)
                textSize = 16f
                setLineSpacing(0f, 1.4f)
                includeFontPadding = false
            }
        },
        update = { view ->
            view.setTextColor(colorInt)
            markwon.setMarkdown(view, normalizeMarkdown(markdown))
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GeneratingDot() {
    val transition = rememberInfiniteTransition(label = "generating")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "generatingAlpha"
    )
    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .size(7.dp)
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
    )
}

@Composable
private fun ThreeDotLoading() {
    val transition = rememberInfiniteTransition(label = "loading")
    val count by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "loadingDots"
    )
    Text(
        "生成中${".".repeat(count.toInt().coerceIn(0, 3))}",
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun BlinkingCursor() {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursorAlpha"
    )
    Text("▌", modifier = Modifier.alpha(alpha), color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun rememberAutoScrollController(listState: LazyListState): AutoScrollController {
    val scope = rememberCoroutineScope()
    val controller = remember(listState, scope) { AutoScrollController(listState, scope) }
    DisposableEffect(controller) { onDispose(controller::dispose) }
    return controller
}

private class AutoScrollController(
    private val listState: LazyListState,
    private val scope: CoroutineScope
) {
    private val requests = Channel<Int>(capacity = Channel.CONFLATED)
    private val worker = scope.launch {
        for (targetIndex in requests) listState.animateToBottom(targetIndex)
    }

    fun request(targetIndex: Int) {
        requests.trySend(targetIndex)
    }

    fun cancelForUser() {
        while (requests.tryReceive().isSuccess) Unit
        scope.launch { listState.scroll(MutatePriority.UserInput) {} }
    }

    fun dispose() {
        requests.close()
        worker.cancel()
    }
}

private suspend fun LazyListState.animateToBottom(targetIndex: Int) {
    repeat(4) {
        val info = layoutInfo
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) return scrollToItem(targetIndex)
        val target = visible.firstOrNull { it.index == targetIndex }
        val distance = if (target != null) {
            target.offset + target.size - info.viewportEndOffset
        } else {
            estimateDistanceTo(targetIndex, visible, info.viewportEndOffset - info.viewportStartOffset)
        }
        if (abs(distance) <= 1) return
        animateScrollBy(
            value = distance.toFloat(),
            animationSpec = tween(AUTO_SCROLL_DURATION_MS, easing = FastOutSlowInEasing)
        )
    }
}

private fun estimateDistanceTo(
    targetIndex: Int,
    visible: List<androidx.compose.foundation.lazy.LazyListItemInfo>,
    viewportSize: Int
): Int {
    val averageSize = visible.map { it.size }.average().toInt().coerceAtLeast(1)
    val first = visible.first()
    val last = visible.last()
    return when {
        targetIndex > last.index -> {
            val itemsAway = targetIndex - last.index
            (itemsAway * averageSize).coerceAtLeast((viewportSize * 0.8f).toInt())
        }
        targetIndex < first.index -> -((first.index - targetIndex) * averageSize)
        else -> 0
    }
}

private fun LazyListState.isAtBottom(anchorIndex: Int): Boolean {
    if (layoutInfo.totalItemsCount == 0) return true
    val anchor = layoutInfo.visibleItemsInfo.firstOrNull { it.index == anchorIndex } ?: return false
    return anchor.offset + anchor.size <= layoutInfo.viewportEndOffset + 2
}

private fun copyMessage(context: Context, content: String) {
    if (content.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("chat_message", content))
    showAppToast(context, "已复制")
}
