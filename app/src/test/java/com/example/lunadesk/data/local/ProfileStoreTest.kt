package com.example.lunadesk.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileStoreTest {
    @Test
    fun `missing or damaged settings fall back to one default profile`() {
        val missing = decodeSettings(null)
        val damaged = decodeSettings("not-json")

        assertEquals("default", missing.activeProfileId)
        assertEquals(listOf("默认配置"), missing.profiles.map { it.name })
        assertEquals(missing, damaged)
    }

    @Test
    fun `upsert can add profile and make it active`() {
        val original = defaultProfileSettings()
        val added = ApiProfile(id = "office", name = "公司")

        val updated = upsertProfile(original, added, makeActive = true)

        assertEquals("office", updated.activeProfileId)
        assertEquals(listOf("default", "office"), updated.profiles.map { it.id })
    }

    @Test
    fun `upsert updates matching profile without changing order`() {
        val original = ApiProfileSettings(
            activeProfileId = "a",
            profiles = listOf(
                ApiProfile(id = "a", name = "A"),
                ApiProfile(id = "b", name = "B")
            )
        )

        val updated = upsertProfile(
            original,
            ApiProfile(id = "a", name = "A2"),
            makeActive = false
        )

        assertEquals(listOf("A2", "B"), updated.profiles.map { it.name })
        assertEquals("a", updated.activeProfileId)
    }

    @Test
    fun `deleting active profile selects first remaining profile`() {
        val original = ApiProfileSettings(
            activeProfileId = "b",
            profiles = listOf(
                ApiProfile(id = "a", name = "A"),
                ApiProfile(id = "b", name = "B")
            )
        )

        val updated = deleteProfile(original, "b")

        assertEquals("a", updated.activeProfileId)
        assertEquals(listOf("a"), updated.profiles.map { it.id })
    }

    @Test
    fun `last profile cannot be deleted`() {
        val original = defaultProfileSettings()

        assertEquals(original, deleteProfile(original, "default"))
    }

    @Test
    fun `profile json preserves separate api keys`() {
        val settings = ApiProfileSettings(
            activeProfileId = "a",
            profiles = listOf(
                ApiProfile(id = "a", name = "A", apiKey = "key-a"),
                ApiProfile(id = "b", name = "B", apiKey = "key-b")
            )
        )
        val raw = profileJson.encodeToString(ApiProfileSettings.serializer(), settings)

        val restored = decodeSettings(raw)

        assertEquals(listOf("key-a", "key-b"), restored.profiles.map { it.apiKey })
        assertTrue(raw.contains("key-a"))
        assertFalse(raw.contains("api_key"))
    }
}
