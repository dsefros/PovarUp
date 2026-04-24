package com.povarup

import com.povarup.core.BusinessDispatchers
import com.povarup.core.BusinessViewModel
import com.povarup.data.DemoMarketplaceRepository
import com.povarup.data.InMemorySessionStore
import com.povarup.data.WorkerRepositorySelector
import com.povarup.domain.ShiftStatus
import com.povarup.domain.WorkType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BusinessDemoViewModelTest {
    @Test
    fun createsDraftShiftAndKeepsBasicFields() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sessionStore = InMemorySessionStore()
        val vm = BusinessViewModel(
            repository = WorkerRepositorySelector(
                sessionStore = sessionStore,
                realRepository = DemoMarketplaceRepository(sessionStore),
                demoRepository = DemoMarketplaceRepository(sessionStore)
            ),
            dispatchers = BusinessDispatchers(io = dispatcher)
        )
        vm.enterBusiness()
        advanceUntilIdle()
        vm.onTitleChanged("Cook Shift")
        vm.onLocationIdChanged("Center")
        vm.onStartAtChanged("2026-05-10 10:00")
        vm.onEndAtChanged("2026-05-10 18:00")
        vm.onPayChanged("30")
        vm.onWorkTypeChanged(WorkType.COOK)
        vm.createDraftShift()
        advanceUntilIdle()

        val shift = vm.uiState.value.rawShifts.first()
        assertEquals(ShiftStatus.DRAFT, shift.status)
        assertEquals("Cook Shift", shift.title)
        assertEquals("Center", shift.locationId)
    }

    @Test
    fun publishCloseCancelLifecycleWorks() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sessionStore = InMemorySessionStore()
        val vm = BusinessViewModel(
            repository = WorkerRepositorySelector(
                sessionStore = sessionStore,
                realRepository = DemoMarketplaceRepository(sessionStore),
                demoRepository = DemoMarketplaceRepository(sessionStore)
            ),
            dispatchers = BusinessDispatchers(io = dispatcher)
        )
        vm.enterBusiness()
        advanceUntilIdle()

        vm.onTitleChanged("Lifecycle Shift")
        vm.onLocationIdChanged("Center")
        vm.onStartAtChanged("2026-05-10 10:00")
        vm.onEndAtChanged("2026-05-10 18:00")
        vm.onPayChanged("20")
        vm.createDraftShift()
        advanceUntilIdle()

        val createdId = vm.uiState.value.rawShifts.first().id
        vm.publishShift(createdId)
        vm.closeShift(createdId)
        vm.cancelShift(createdId)
        advanceUntilIdle()

        val updated = vm.findShift(createdId)
        assertNotNull(updated)
        assertEquals(ShiftStatus.CANCELLED, updated?.status)
        assertTrue(vm.uiState.value.isInDemoSession)
    }
}
