package com.example.lunadesk.ui

import com.example.lunadesk.data.model.ModelInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class LunaDeskViewModelTest {
    @Test
    fun `refresh models keeps explicit selection when model still exists`() {
        val models = listOf(
            ModelInfo(id = "qwen-7b"),
            ModelInfo(id = "qwen-14b")
        )

        val selected = resolveSelectedModel("qwen-14b", models)

        assertEquals("qwen-14b", selected)
    }

    @Test
    fun `refresh models does not auto select first model`() {
        val models = listOf(
            ModelInfo(id = "qwen-7b"),
            ModelInfo(id = "qwen-14b")
        )

        val selected = resolveSelectedModel("", models)

        assertEquals("", selected)
    }

    @Test
    fun `next toast event starts from first id`() {
        val event = nextToastEvent("已保存", null)

        assertEquals(1L, event.id)
        assertEquals("已保存", event.message)
    }

    @Test
    fun `next toast event increments id for same message`() {
        val current = ToastEvent(id = 1L, message = "已保存")

        val next = nextToastEvent("已保存", current)

        assertEquals(2L, next.id)
        assertEquals("已保存", next.message)
    }
}
