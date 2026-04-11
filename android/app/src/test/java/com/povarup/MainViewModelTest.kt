package com.povarup

import com.povarup.core.AppDispatchers
import com.povarup.core.MainViewModel
import com.povarup.data.CreateShiftRequest
import com.povarup.data.MarketplaceRepository
import com.povarup.data.SessionToken
import com.povarup.domain.Application
import com.povarup.domain.Assignment
import com.povarup.domain.Payout
import com.povarup.domain.Shift
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @Test
    fun actionFailureRemainsVisible() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply {
                session = SessionToken("sess", "worker.demo", "worker")
                applyFailure = IllegalStateException("apply failed")
            }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.applyToShift("shift_1")
            advanceUntilIdle()

            assertTrue(vm.uiState.value.errorMessage?.contains("apply failed") == true)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun dashboardFailureIsSurfaced() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply { shiftsFailure = IllegalStateException("dashboard shifts failed") }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.loadDashboard()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.errorMessage?.contains("dashboard shifts failed") == true)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun businessDashboardUsesOwnedShiftsAndShiftApplications() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply {
                role = "business"
                session = SessionToken("sess", "business.demo", "business")
            }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.loadDashboard()
            advanceUntilIdle()

            assertEquals(1, repo.businessShiftCalls)
            assertEquals(1, repo.shiftApplicationsCalls)
            assertEquals("app_business_1", vm.uiState.value.applications.first().id)
        } finally { Dispatchers.resetMain() }
    }

    private class FakeRepo : MarketplaceRepository {
        var role: String = "worker"
        var session: SessionToken? = null
        var applyFailure: Throwable? = null
        var shiftsFailure: Throwable? = null
        var businessShiftCalls = 0
        var shiftApplicationsCalls = 0

        override fun currentRole(): String = role
        override fun setRole(role: String) { this.role = role }
        override fun baseUrl(): String = "http://localhost"
        override fun currentSession(): SessionToken? = session
        override fun login(userId: String, password: String): Result<SessionToken> = Result.success(SessionToken("sess", userId, role).also { session = it })
        override fun clearSession() { session = null }

        override fun listShifts(): Result<List<Shift>> = shiftsFailure?.let { Result.failure(it) } ?: Result.success(listOf(Shift("shift_w", "biz", "loc", "Worker shift", "", "", 1000, "open")))
        override fun getShift(shiftId: String): Result<Shift> = Result.success(listShifts().getOrThrow().first())
        override fun applyToShift(shiftId: String): Result<Application> = applyFailure?.let { Result.failure(it) } ?: Result.success(Application("app", shiftId, "worker", "applied"))
        override fun listApplications(): Result<List<Application>> = Result.success(listOf(Application("app_worker_1", "shift_w", "worker", "applied")))
        override fun listAssignments(): Result<List<Assignment>> = Result.success(emptyList())
        override fun getAssignment(assignmentId: String): Result<Assignment> = Result.failure(Exception())
        override fun acceptAssignment(assignmentId: String): Result<Assignment> = Result.failure(Exception())
        override fun checkIn(assignmentId: String): Result<Unit> = Result.success(Unit)
        override fun checkOut(assignmentId: String): Result<Unit> = Result.success(Unit)
        override fun createShift(input: CreateShiftRequest): Result<Shift> = Result.success(Shift("shift_new", "biz", input.locationId, input.title, input.startAt, input.endAt, input.payRateCents, "open"))
        override fun listBusinessShifts(): Result<List<Shift>> {
            businessShiftCalls += 1
            return shiftsFailure?.let { Result.failure(it) }
                ?: Result.success(listOf(Shift("shift_biz_1", "biz_1", "loc_1", "Owned shift", "", "", 2000, "open")))
        }

        override fun listShiftApplications(shiftId: String): Result<List<Application>> {
            shiftApplicationsCalls += 1
            return Result.success(listOf(Application("app_business_1", shiftId, "worker_1", "applied")))
        }

        override fun offerAssignment(applicationId: String): Result<Assignment> = Result.failure(Exception())
        override fun releasePayout(assignmentId: String): Result<Payout> = Result.failure(Exception())
    }
}
