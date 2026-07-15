package com.example.lunadesk.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenAiEndpointTest {
    @Test
    fun `base url without version appends v1 once`() {
        assertEquals(
            "https://example.com/v1/models",
            buildOpenAiEndpoint("https://example.com/", "models")
        )
    }

    @Test
    fun `base url containing v1 does not duplicate version`() {
        assertEquals(
            "https://example.com/v1/chat/completions",
            buildOpenAiEndpoint("https://example.com/v1", "/chat/completions")
        )
    }
}
