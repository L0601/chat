package com.example.lunadesk.ui

import com.example.lunadesk.data.model.ModelInfo
import com.example.lunadesk.data.local.ApiProfile
import org.junit.Assert.assertNull
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

    @Test
    fun `profile validation rejects duplicate name ignoring case`() {
        val profiles = listOf(ApiProfile(id = "a", name = "Office"))
        val duplicate = ApiProfile(
            id = "b",
            name = "office",
            baseUrl = "https://example.com"
        )

        assertEquals("配置名称不能重复", validateProfile(duplicate, profiles))
    }

    @Test
    fun `profile validation accepts http local endpoint`() {
        val profile = ApiProfile(
            id = "a",
            name = "本地",
            baseUrl = "http://192.168.1.10:1234"
        )

        assertNull(validateProfile(profile, listOf(profile)))
    }

    @Test
    fun `next profile name fills first available number`() {
        val profiles = listOf(
            ApiProfile(id = "a", name = "配置 1"),
            ApiProfile(id = "b", name = "配置 3")
        )

        assertEquals("配置 2", nextProfileName(profiles))
    }
}
