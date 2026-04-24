package com.povarup

import com.povarup.data.DemoMarketplaceRepository
import com.povarup.data.InMemorySessionStore
import com.povarup.domain.CookCuisine
import com.povarup.domain.CookStation
import com.povarup.domain.DishwasherZone
import com.povarup.domain.WorkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoMarketplaceRepositoryTest {
    @Test
    fun loginSucceedsForDemoCredentials() {
        val repository = DemoMarketplaceRepository(InMemorySessionStore())

        val result = repository.login(
            DemoMarketplaceRepository.DEMO_USER_ID,
            DemoMarketplaceRepository.DEMO_PASSWORD
        )

        assertTrue(result.isSuccess)
        assertNotNull(repository.currentSession())
        assertEquals("worker", repository.currentRole())
    }

    @Test
    fun loginFailsForWrongCredentials() {
        val repository = DemoMarketplaceRepository(InMemorySessionStore())

        val result = repository.login("worker.demo", "wrong")

        assertTrue(result.isFailure)
        assertNull(repository.currentSession())
    }

    @Test
    fun applyFlowPersistsAsAppliedState() {
        val repository = DemoMarketplaceRepository(InMemorySessionStore())
        repository.login(DemoMarketplaceRepository.DEMO_USER_ID, DemoMarketplaceRepository.DEMO_PASSWORD)

        val before = repository.listApplications().getOrThrow().map { it.shiftId }.toSet()
        assertFalse(before.contains("demo-shift-4"))

        val applyResult = repository.applyToShift("demo-shift-4")

        assertTrue(applyResult.isSuccess)
        val after = repository.listApplications().getOrThrow().map { it.shiftId }.toSet()
        assertTrue(after.contains("demo-shift-4"))
    }

    @Test
    fun sessionRestoresWithSharedSessionStore() {
        val store = InMemorySessionStore()
        val firstRepo = DemoMarketplaceRepository(store)
        firstRepo.login(DemoMarketplaceRepository.DEMO_USER_ID, DemoMarketplaceRepository.DEMO_PASSWORD)

        val secondRepo = DemoMarketplaceRepository(store)

        assertNotNull(secondRepo.currentSession())
        assertTrue(secondRepo.listShifts().isSuccess)
    }

    @Test
    fun demoShiftsExposeStructuredWorkTypes() {
        val repository = DemoMarketplaceRepository(InMemorySessionStore())
        repository.login(DemoMarketplaceRepository.DEMO_USER_ID, DemoMarketplaceRepository.DEMO_PASSWORD)

        val shifts = repository.listShifts().getOrThrow()

        assertEquals(10, shifts.size)

        val cookShift = shifts.first { it.id == "demo-shift-1" }
        assertEquals(WorkType.COOK, cookShift.workType)
        assertEquals(CookCuisine.RUSSIAN, cookShift.cookCuisine)
        assertEquals(CookStation.HOT, cookShift.cookStation)
        assertEquals(true, cookShift.isBanquet)

        val waiterShifts = shifts.filter { it.workType == WorkType.WAITER }.sortedBy { it.id }
        assertEquals(setOf(false, true), waiterShifts.map { it.isBanquet }.toSet())

        val bartenderShifts = shifts.filter { it.workType == WorkType.BARTENDER }.sortedBy { it.id }
        assertEquals(setOf(false, true), bartenderShifts.map { it.isBanquet }.toSet())

        val dishwasherShifts = shifts.filter { it.workType == WorkType.DISHWASHER }.sortedBy { it.id }
        assertEquals(setOf(DishwasherZone.WHITE, DishwasherZone.BLACK), dishwasherShifts.map { it.dishwasherZone }.toSet())
    }
}
