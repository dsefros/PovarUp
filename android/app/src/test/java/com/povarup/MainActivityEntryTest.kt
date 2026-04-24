package com.povarup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityEntryTest {
    @Test
    fun routesOnlyAdminSessionToLegacy() {
        assertFalse(MainActivity.shouldOpenLegacy(hasSession = true, currentRoleRaw = "business"))
        assertTrue(MainActivity.shouldOpenLegacy(hasSession = true, currentRoleRaw = "admin"))
    }

    @Test
    fun keepsWorkerOrAnonymousInComposePath() {
        assertFalse(MainActivity.shouldOpenLegacy(hasSession = true, currentRoleRaw = "worker"))
        assertFalse(MainActivity.shouldOpenLegacy(hasSession = false, currentRoleRaw = "admin"))
    }
}
