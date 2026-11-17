package com.example.liftjournal.ui.progress

import com.example.liftjournal.domain.progress.ProgressView
import org.junit.Assert.assertEquals
import org.junit.Test

class ShownViewTest {
    @Test
    fun `shows the goal view by default when a goal exists`() {
        assertEquals(ProgressView.GOAL, shownView(chosen = null, hasGoals = true))
    }

    @Test
    fun `shows total volume by default without goals`() {
        assertEquals(ProgressView.TOTAL_VOLUME, shownView(chosen = null, hasGoals = false))
    }

    @Test
    fun `keeps the view the user picked`() {
        assertEquals(ProgressView.TOP_SET, shownView(chosen = ProgressView.TOP_SET, hasGoals = true))
    }

    @Test
    fun `falls back to total volume when the last goal is removed`() {
        assertEquals(ProgressView.TOTAL_VOLUME, shownView(chosen = ProgressView.GOAL, hasGoals = false))
    }
}
