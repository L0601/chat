package com.example.lunadesk.ui.screen.chat

internal val INLINE_MATH_REGEX = Regex("(?<!\\\\)\\$([^$\\n]+?)(?<!\\\\)\\$")

internal fun normalizeMarkdown(markdown: String): String {
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
