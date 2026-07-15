package com.example.lunadesk.ui.screen.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownUtilsTest {
    @Test
    fun `emphasis markers remain intact around section titles`() {
        val markdown = """
            **最近半年行情应重点从四方面判断**

            *1. 指数趋势*

            **3. 市场风险**
        """.trimIndent()

        assertEquals(markdown, normalizeMarkdown(markdown))
    }

    @Test
    fun `list markers remain intact without rewriting inline emphasis`() {
        val markdown = """
            A股持续行情通常需要成交额配合：

            - **上涨放量、回调缩量**：偏健康
            - 指数上涨但成交额下降：可能只是权重护盘
        """.trimIndent()

        assertEquals(markdown, normalizeMarkdown(markdown))
    }

    @Test
    fun `windows line endings are normalized`() {
        assertEquals("第一段\n\n第二段", normalizeMarkdown("第一段\r\n\r\n第二段"))
    }

    @Test
    fun `heading without a space is normalized`() {
        assertEquals("### 市场风险", normalizeMarkdown("###市场风险"))
    }

    @Test
    fun `heading joined after a sentence is moved to a new block`() {
        assertEquals(
            "上一部分结束。\n\n### 下一部分",
            normalizeMarkdown("上一部分结束。###下一部分")
        )
    }

    @Test
    fun `heading markers inside fenced code remain unchanged`() {
        val fence = 96.toChar().toString().repeat(3)
        val markdown = fence + "markdown\n###标题示例\n" + fence

        assertEquals(markdown, normalizeMarkdown(markdown))
    }

    @Test
    fun `inline math is converted for latex renderer`() {
        val dollar = '$'
        val input = "结果为 " + dollar + "x + 1" + dollar + "。"
        val delimiter = dollar.toString().repeat(4)

        assertEquals("结果为 " + delimiter + "x + 1" + delimiter + "。", normalizeMarkdown(input))
    }
}
