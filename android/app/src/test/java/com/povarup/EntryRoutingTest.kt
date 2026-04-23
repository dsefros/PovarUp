package com.povarup

import com.povarup.core.RootContent
import com.povarup.core.RootEntryMode
import com.povarup.core.resolveRootContent
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryRoutingTest {
    @Test
    fun showsWelcomeByDefaultWhenLoggedOut() {
        assertEquals(RootContent.WELCOME, resolveRootContent(false, RootEntryMode.WELCOME))
    }

    @Test
    fun showsBusinessDemoWhenModeIsBusiness() {
        assertEquals(RootContent.DEMO_BUSINESS, resolveRootContent(false, RootEntryMode.DEMO_BUSINESS))
    }

    @Test
    fun loggedInWorkerAlwaysGoesToWorkerShifts() {
        assertEquals(RootContent.WORKER_SHIFTS, resolveRootContent(true, RootEntryMode.DEMO_BUSINESS))
    }
}
