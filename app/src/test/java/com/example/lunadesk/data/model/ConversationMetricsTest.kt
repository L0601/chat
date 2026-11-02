package com.example.lunadesk.data.model

import org.junit.Assert.*
import org.junit.Test

class ConversationMetricsTest {
    @Test fun `weighted aggregates use only matching completed calls`() {
        val messages = listOf(
            CallMetrics(1000, 100, 1000, 2000, "a", true),
            CallMetrics(3000, 300, 2000, 3000, "b", true),
            CallMetrics(null, 90, 100, null, "a", true),
            CallMetrics(9999, 9999, 50, 200, "a", false)
        ).mapIndexed { i, m -> ChatMessageUi("$i", "assistant", "", metrics = m) }
        val result = aggregateMetrics(messages)
        assertEquals(4000L, result.inputTokens)
        assertEquals(490L, result.outputTokens)
        assertEquals(80.0, result.speed!!, 0.001)
        assertEquals(1750.0, result.firstTextMs!!, 0.001)
        assertEquals(3, result.completedCount)
        assertTrue(result.mixedModels)
    }
    @Test fun `missing and short durations do not produce a speed`() {
        val result = aggregateMetrics(listOf(ChatMessageUi("1", "assistant", "", metrics = CallMetrics(outputTokens = 1, outputMs = 50, completed = true))))
        assertNull(result.speed)
        assertNull(result.firstTextMs)
    }
}
