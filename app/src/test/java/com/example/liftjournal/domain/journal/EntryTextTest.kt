package com.example.liftjournal.domain.journal

import org.junit.Assert.assertEquals
import org.junit.Test

class EntryTextTest {
    @Test
    fun `appended text goes on a new line below`() {
        assertEquals("squat 100 x 5\nbench 60 x 5", appendEntryText("squat 100 x 5\n", "\nbench 60 x 5"))
    }

    @Test
    fun `blank sides are dropped`() {
        assertEquals("bench 60 x 5", appendEntryText("  ", "bench 60 x 5"))
        assertEquals("squat 100 x 5", appendEntryText("squat 100 x 5", ""))
    }
}
