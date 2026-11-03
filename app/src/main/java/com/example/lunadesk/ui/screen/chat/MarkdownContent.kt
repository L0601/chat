package com.example.lunadesk.ui.screen.chat

import android.widget.TextView
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun MarkdownContent(markdown: String, color: Color) {
    val sections = remember(markdown) { splitMarkdownSections(markdown) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        sections.forEachIndexed { index, section ->
            key(index) {
                when (section) {
                    is MarkdownSection.Text -> MarkdownView(section.markdown, color)
                    is MarkdownSection.Table -> MarkdownTable(section.rows, color)
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(rows: List<List<String>>, color: Color) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val shaded = MaterialTheme.colorScheme.surfaceVariant
    val columns = rows.firstOrNull()?.size?.coerceAtLeast(1) ?: return
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val viewportWidth = maxWidth
        val tableWidth = maxOf(viewportWidth, 112.dp * columns)
        Column {
            if (tableWidth > viewportWidth) {
                Text("左右滑动查看表格", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Column(Modifier.width(tableWidth).border(1.dp, borderColor)) {
                    rows.forEachIndexed { index, row ->
                        key(index) {
                            Row(Modifier.fillMaxWidth().background(if (index == 0 || index % 2 == 0) shaded else Color.Transparent)
                                .drawBehind {
                                    val stroke = 1.dp.toPx()
                                    for (i in 1 until columns) drawLine(borderColor, androidx.compose.ui.geometry.Offset(size.width * i / columns, 0f), androidx.compose.ui.geometry.Offset(size.width * i / columns, size.height), stroke)
                                    if (index > 0) drawLine(borderColor, androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(size.width, 0f), stroke)
                                }) {
                                row.forEach { cell ->
                                    Box(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 8.dp)) {
                                        MarkdownView(cell.ifEmpty { " " }, color, isHeader = index == 0)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownView(markdown: String, color: Color, isHeader: Boolean = false) {
    val context = LocalContext.current
    val markwon = rememberMarkdownRenderer(context)
    val colorInt = color.toArgb()
    AndroidView(
        factory = { TextView(it).apply { applyMarkdownReadingStyle(colorInt) } },
        update = { view ->
            val renderKey = Triple(markdown, colorInt, isHeader)
            if (view.tag != renderKey) {
                view.setTextColor(colorInt)
                view.setTypeface(Typeface.DEFAULT, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
                markwon.setMarkdown(view, normalizeMarkdown(markdown))
                view.tag = renderKey
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
