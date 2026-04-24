package com.povarup

import com.povarup.core.RootContent
import com.povarup.core.RootEntryMode
import com.povarup.core.resolveRootContent
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryRoutingTest {
    @Test
    fun showsWelcomeByDefaultWhenLoggedOut() {
        assertEquals(
            RootContent.WELCOME,
            resolveRootContent(
                isWorkerLoggedIn = false,
                isBusinessDemoSessionActive = false,
                mode = RootEntryMode.WELCOME
            )
        )
    }

    @Test
    fun showsBusinessDemoWhenModeIsBusinessAndDemoSessionIsActive() {
        assertEquals(
            RootContent.DEMO_BUSINESS,
            resolveRootContent(
                isWorkerLoggedIn = false,
                isBusinessDemoSessionActive = true,
                mode = RootEntryMode.DEMO_BUSINESS
            )
        )
    }

    @Test
    fun loggedInWorkerAlwaysGoesToWorkerShifts() {
        assertEquals(
            RootContent.WORKER_SHIFTS,
            resolveRootContent(
                isWorkerLoggedIn = true,
                isBusinessDemoSessionActive = true,
                mode = RootEntryMode.DEMO_BUSINESS
            )
        )
    }
}
