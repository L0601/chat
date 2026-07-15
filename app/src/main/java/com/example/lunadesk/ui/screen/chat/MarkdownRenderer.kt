package com.example.lunadesk.ui.screen.chat

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import kotlin.math.roundToInt

private data class MarkdownPalette(
    val body: Int,
    val accent: Int,
    val subtle: Int,
    val border: Int
)

@Composable
internal fun rememberMarkdownRenderer(context: Context): Markwon {
    val colors = MaterialTheme.colorScheme
    val palette = MarkdownPalette(
        body = colors.onSurface.toArgb(),
        accent = colors.secondary.toArgb(),
        subtle = colors.surfaceVariant.toArgb(),
        border = colors.outline.toArgb()
    )
    return remember(context, palette) {
        Markwon.builder(context)
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(JLatexMathPlugin.create(44f) { it.inlinesEnabled(true) })
            .usePlugin(markdownThemePlugin(context, palette))
            .build()
    }
}

internal fun TextView.applyMarkdownReadingStyle(textColor: Int) {
    setTextColor(textColor)
    textSize = 16f
    setLineSpacing(0f, 1.24f)
    includeFontPadding = false
    setHorizontallyScrolling(false)
}

private fun markdownThemePlugin(
    context: Context,
    palette: MarkdownPalette
) = object : AbstractMarkwonPlugin() {
    override fun configureTheme(builder: MarkwonTheme.Builder) {
        builder
            .linkColor(palette.accent)
            .isLinkUnderlined(false)
            .blockMargin(context.dp(16))
            .blockQuoteWidth(context.dp(3))
            .blockQuoteColor(palette.accent)
            .listItemColor(palette.body)
            .bulletWidth(context.dp(5))
            .bulletListItemStrokeWidth(context.dp(2))
            .codeTextColor(palette.body)
            .codeBlockTextColor(palette.body)
            .codeBackgroundColor(palette.subtle)
            .codeBlockBackgroundColor(palette.subtle)
            .codeBlockMargin(context.dp(12))
            .codeTypeface(Typeface.MONOSPACE)
            .codeBlockTypeface(Typeface.MONOSPACE)
            .headingTypeface(Typeface.create("sans-serif", Typeface.BOLD))
            .headingTextSizeMultipliers(floatArrayOf(1.45f, 1.30f, 1.18f, 1.08f, 1f, 1f))
            .thematicBreakColor(palette.border)
            .thematicBreakHeight(context.dp(1))
    }
}

private fun Context.dp(value: Int): Int {
    return (value * resources.displayMetrics.density).roundToInt()
}
