package com.example.lunadesk.ui.screen.chat

internal sealed interface MarkdownSection {
    data class Text(val markdown: String) : MarkdownSection
    data class Table(val rows: List<List<String>>) : MarkdownSection
}

private val tableSeparator = Regex("^:?-{3,}:?$")

internal fun splitMarkdownSections(markdown: String): List<MarkdownSection> {
    val lines = markdown.split('\n')
    val sections = mutableListOf<MarkdownSection>()
    val text = mutableListOf<String>()
    var fence: String? = null
    var i = 0
    fun flushText() {
        val value = text.joinToString("\n").trim('\n')
        if (value.isNotEmpty()) sections.add(MarkdownSection.Text(value))
        text.clear()
    }
    while (i < lines.size) {
        val trimmed = lines[i].trimStart()
        val marker = when {
            trimmed.startsWith("```") -> "```"
            trimmed.startsWith("~~~") -> "~~~"
            else -> null
        }
        if (marker != null) {
            if (fence == null) fence = marker else if (fence == marker) fence = null
            text.add(lines[i++])
            continue
        }
        val header = splitTableCells(lines[i])
        val separator = if (i + 1 < lines.size) splitTableCells(lines[i + 1]) else emptyList()
        if (fence == null && lines[i].contains('|') && header.size >= 2 &&
            header.size == separator.size && separator.all { tableSeparator.matches(it.trim()) }) {
            flushText()
            val rows = mutableListOf(header)
            i += 2
            while (i < lines.size && lines[i].isNotBlank() && lines[i].contains('|')) {
                val cells = splitTableCells(lines[i++])
                rows.add(List(header.size) { column -> cells.getOrElse(column) { "" } })
            }
            sections.add(MarkdownSection.Table(rows))
        } else text.add(lines[i++])
    }
    flushText()
    return sections
}

internal fun splitTableCells(line: String): List<String> {
    val value = line.trim().removePrefix("|").removeSuffix("|")
    val cells = mutableListOf<String>()
    val cell = StringBuilder()
    var escaped = false
    var inCode = false
    value.forEach { c ->
        when {
            escaped -> { cell.append('\\').append(c); escaped = false }
            c == '\\' -> escaped = true
            c == '`' -> { inCode = !inCode; cell.append(c) }
            c == '|' && !inCode -> { cells.add(cell.toString().trim()); cell.clear() }
            else -> cell.append(c)
        }
    }
    if (escaped) cell.append('\\')
    cells.add(cell.toString().trim())
    return cells
}
