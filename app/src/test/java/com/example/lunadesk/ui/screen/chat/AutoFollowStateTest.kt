package com.example.lunadesk.ui.screen.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoFollowStateTest {
    @Test
    fun `drag pauses every later chunk from current generation`() {
        val state = AutoFollowState().pauseFor("reply-1")

        assertFalse(state.shouldFollow("reply-1"))
        assertFalse(state.shouldFollow("reply-1"))
    }

    @Test
    fun `new generation resumes automatic following`() {
        val state = AutoFollowState().pauseFor("reply-1")

        assertTrue(state.shouldFollow("reply-2"))
    }

    @Test
    fun `drag without active generation does not change policy`() {
        val state = AutoFollowState()

        assertEquals(state, state.pauseFor(null))
    }
}
