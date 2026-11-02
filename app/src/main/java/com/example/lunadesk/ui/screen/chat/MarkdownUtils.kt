package com.example.lunadesk.ui.screen.chat

internal val INLINE_MATH_REGEX = Regex("""(?<!\\)\$([^$\n]+?)(?<!\\)\$""")
private val COMPACT_HEADING_REGEX = Regex("""^([ \t]{0,3})(#{1,6})(?!#)[ \t]*(\S.*)$""")
private val JOINED_HEADING_REGEX = Regex("""([。！？!?；;：:])[ \t]*(#{1,6})(?!#)[ \t]*(?=\S)""")
private val FENCE_PREFIXES = listOf("~~~", 96.toChar().toString().repeat(3))

internal fun normalizeMarkdown(markdown: String): String {
    val normalizedNewlines = markdown
        .replace("\r\n", "\n")
        .replace('\r', '\n')
    val normalizedLayout = normalizeHeadingLayout(normalizedNewlines)

    return INLINE_MATH_REGEX.replace(normalizedLayout) { match ->
        val value = match.groupValues[1].trim()
        if (value.isEmpty()) {
            match.value
        } else {
            "$$$$" + value + "$$$$"
        }
    }
}

private fun normalizeHeadingLayout(markdown: String): String {
    var insideCodeFence = false
    return markdown.lines().flatMap { line ->
        if (FENCE_PREFIXES.any(line.trimStart()::startsWith)) {
            insideCodeFence = !insideCodeFence
            listOf(line)
        } else if (insideCodeFence) {
            listOf(line)
        } else {
            splitJoinedHeadings(line).map(::normalizeHeadingLine)
        }
    }.joinToString("\n")
}

private fun splitJoinedHeadings(line: String): List<String> {
    val separated = JOINED_HEADING_REGEX.replace(line) { match ->
        match.groupValues[1] + "\n\n" + match.groupValues[2] + " "
    }
    return separated.lines()
}

private fun normalizeHeadingLine(line: String): String {
    return COMPACT_HEADING_REGEX.replace(line) { match ->
        match.groupValues[1] + match.groupValues[2] + " " + match.groupValues[3]
    }
}

// Keep a table row stable while its cells are still arriving.
internal fun stableStreamingMarkdown(markdown: String): String {
    val lastBreak = markdown.lastIndexOf('\n')
    if (lastBreak < 0 || lastBreak == markdown.lastIndex) return markdown
    val lastLine = markdown.substring(lastBreak + 1)
    if (!lastLine.trimStart().startsWith("|")) return markdown
    val preceding = markdown.substring(0, lastBreak).lines()
    if (preceding.count { line -> FENCE_PREFIXES.any(line.trimStart()::startsWith) } % 2 != 0) return markdown
    val tableLines = preceding.asReversed().takeWhile { it.isNotBlank() && it.contains('|') }
    val separator = Regex("""^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$""")
    return if (tableLines.any { separator.matches(it) }) markdown.substring(0, lastBreak + 1) else markdown
}
