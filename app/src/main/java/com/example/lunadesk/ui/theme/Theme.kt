package com.example.lunadesk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── AppColors ────────────────────────────────────────────────

@Immutable
data class AppColors(
    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textPlaceholder: Color,
    val textAccent: Color,
    val textOnButton: Color,
    // Bubble
    val bubbleUser: Color,
    val bubbleAssistant: Color,
    val bubbleUserLabel: Color,
    val bubbleAssistantLabel: Color,
    val bubbleUserText: Color,
    val bubbleAssistantText: Color,
    // Surface
    val surfaceChat: Color,
    val surfaceComposer: Color,
    val surfaceOverlay: Color,
    val surfaceOverlayDisabled: Color,
    val surfaceInput: Color,
    val surfaceInputDisabled: Color,
    val surfaceCopyButton: Color,
    // Notice
    val noticeBg: Color,
    val noticeText: Color,
    // Button
    val buttonSend: Color,
    val buttonStop: Color,
    val buttonDisabledBg: Color,
    val buttonDisabledContent: Color,
    // Icon
    val iconPrimary: Color,
    val iconCopyBorder: Color,
    val cursor: Color,
    // Drawer
    val drawerBg: Color,
    val drawerCardBg: Color,
    val drawerSelectedItem: Color,
    // Settings
    val configCardBg: Color,
    val modelListCardBg: Color,
    val modelRowSelected: Color,
    val modelRowUnselected: Color,
    val modelStatusText: Color,
    // Gradient
    val gradientStart: Color,
    val gradientMiddle: Color,
    val gradientEnd: Color,
)

val LightAppColors = AppColors(
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    textPlaceholder = LightTextPlaceholder,
    textAccent = LightTextAccent,
    textOnButton = LightTextOnButton,
    bubbleUser = LightBubbleUser,
    bubbleAssistant = LightBubbleAssistant,
    bubbleUserLabel = LightBubbleUserLabel,
    bubbleAssistantLabel = LightBubbleAssistantLabel,
    bubbleUserText = LightBubbleUserText,
    bubbleAssistantText = LightBubbleAssistantText,
    surfaceChat = LightSurfaceChat,
    surfaceComposer = LightSurfaceComposer,
    surfaceOverlay = LightSurfaceOverlay,
    surfaceOverlayDisabled = LightSurfaceOverlayDisabled,
    surfaceInput = LightSurfaceInput,
    surfaceInputDisabled = LightSurfaceInputDisabled,
    surfaceCopyButton = LightSurfaceCopyButton,
    noticeBg = LightNoticeBg,
    noticeText = LightNoticeText,
    buttonSend = LightButtonSend,
    buttonStop = LightButtonStop,
    buttonDisabledBg = LightButtonDisabledBg,
    buttonDisabledContent = LightButtonDisabledContent,
    iconPrimary = LightIconPrimary,
    iconCopyBorder = LightIconCopyBorder,
    cursor = LightCursor,
    drawerBg = LightDrawerBg,
    drawerCardBg = LightDrawerCardBg,
    drawerSelectedItem = LightDrawerSelectedItem,
    configCardBg = LightConfigCardBg,
    modelListCardBg = LightModelListCardBg,
    modelRowSelected = LightModelRowSelected,
    modelRowUnselected = LightModelRowUnselected,
    modelStatusText = LightModelStatusText,
    gradientStart = LightGradientStart,
    gradientMiddle = LightGradientMiddle,
    gradientEnd = LightGradientEnd,
)

val DarkAppColors = AppColors(
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    textPlaceholder = DarkTextPlaceholder,
    textAccent = DarkTextAccent,
    textOnButton = DarkTextOnButton,
    bubbleUser = DarkBubbleUser,
    bubbleAssistant = DarkBubbleAssistant,
    bubbleUserLabel = DarkBubbleUserLabel,
    bubbleAssistantLabel = DarkBubbleAssistantLabel,
    bubbleUserText = DarkBubbleUserText,
    bubbleAssistantText = DarkBubbleAssistantText,
    surfaceChat = DarkSurfaceChat,
    surfaceComposer = DarkSurfaceComposer,
    surfaceOverlay = DarkSurfaceOverlay,
    surfaceOverlayDisabled = DarkSurfaceOverlayDisabled,
    surfaceInput = DarkSurfaceInput,
    surfaceInputDisabled = DarkSurfaceInputDisabled,
    surfaceCopyButton = DarkSurfaceCopyButton,
    noticeBg = DarkNoticeBg,
    noticeText = DarkNoticeText,
    buttonSend = DarkButtonSend,
    buttonStop = DarkButtonStop,
    buttonDisabledBg = DarkButtonDisabledBg,
    buttonDisabledContent = DarkButtonDisabledContent,
    iconPrimary = DarkIconPrimary,
    iconCopyBorder = DarkIconCopyBorder,
    cursor = DarkCursor,
    drawerBg = DarkDrawerBg,
    drawerCardBg = DarkDrawerCardBg,
    drawerSelectedItem = DarkDrawerSelectedItem,
    configCardBg = DarkConfigCardBg,
    modelListCardBg = DarkModelListCardBg,
    modelRowSelected = DarkModelRowSelected,
    modelRowUnselected = DarkModelRowUnselected,
    modelStatusText = DarkModelStatusText,
    gradientStart = DarkGradientStart,
    gradientMiddle = DarkGradientMiddle,
    gradientEnd = DarkGradientEnd,
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

// ── Material ColorScheme ─────────────────────────────────────

private val LightColors = lightColorScheme(
    primary = Forest,
    secondary = Clay,
    tertiary = Moss,
    background = Paper,
    surface = Paper,
)

private val DarkColors = darkColorScheme(
    primary = Sand,
    secondary = Moss,
    tertiary = Clay,
    background = Color(0xFF121612),
    surface = Color(0xFF1A1E1A),
)

// ── Theme ────────────────────────────────────────────────────

@Composable
fun LunaDeskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val materialColors = if (darkTheme) DarkColors else LightColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = AppTypography,
            content = content,
        )
    }
}
