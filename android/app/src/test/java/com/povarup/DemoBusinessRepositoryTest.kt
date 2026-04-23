package com.povarup

import com.povarup.data.CreateDemoBusinessShiftRequest
import com.povarup.data.DemoBusinessRepository
import com.povarup.domain.CookCuisine
import com.povarup.domain.CookStation
import com.povarup.domain.WorkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoBusinessRepositoryTest {
    @Test
    fun createAndListShiftInsideCurrentSession() {
        val repository = DemoBusinessRepository()
        repository.enterDemoBusiness()

        val created = repository.createShift(
            CreateDemoBusinessShiftRequest(
                title = "Banquet Head Cook",
                locationId = "Downtown",
                startAt = "2026-05-10 10:00",
                endAt = "2026-05-10 18:00",
                payRateCents = 3200,
                workType = WorkType.COOK,
                cookCuisine = CookCuisine.RUSSIAN,
                cookStation = CookStation.HOT,
                isBanquet = true
            )
        )

        assertTrue(created.isSuccess)
        val list = repository.listShifts().getOrThrow()
        assertEquals(1, list.size)
        assertEquals("Banquet Head Cook", list.first().title)
    }

    @Test
    fun repositoryDoesNotPersistAcrossFreshInstance() {
        val first = DemoBusinessRepository()
        first.enterDemoBusiness()
        first.createShift(
            CreateDemoBusinessShiftRequest(
                title = "Session Shift",
                locationId = "A",
                startAt = "2026-01-01 10:00",
                endAt = "2026-01-01 18:00",
                payRateCents = 2000,
                workType = WorkType.WAITER,
                isBanquet = false
            )
        )

        val second = DemoBusinessRepository()
        second.enterDemoBusiness()
        assertTrue(second.listShifts().getOrThrow().isEmpty())
    }
}
