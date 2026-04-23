package com.povarup

import com.povarup.data.DemoMarketplaceRepository
import com.povarup.data.InMemorySessionStore
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
}
