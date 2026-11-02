package com.example.lunadesk.ui.screen.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownSectionsTest {
    @Test fun `table is separated from surrounding paragraphs`() {
        val sections = splitMarkdownSections("Intro\n\n| Name | Value |\n| --- | :---: |\n| A | **B** |\n\nEnd")
        assertEquals(MarkdownSection.Text("Intro"), sections[0])
        assertEquals(MarkdownSection.Table(listOf(listOf("Name", "Value"), listOf("A", "**B**"))), sections[1])
        assertEquals(MarkdownSection.Text("End"), sections[2])
    }
    @Test fun `fenced code stays text and escaped pipes remain inside cells`() {
        val code = "```\n| a | b |\n| --- | --- |\n```"
        assertEquals(listOf(MarkdownSection.Text(code)), splitMarkdownSections(code))
        assertEquals(listOf("a\\|b", "`x|y`"), splitTableCells("| a\\|b | `x|y` |"))
    }
    @Test fun `incomplete table stays markdown text`() {
        assertTrue(splitMarkdownSections("| A | B |\n| --- | --").single() is MarkdownSection.Text)
    }
}
