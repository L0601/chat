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
}
