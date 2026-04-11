package com.povarup

import com.povarup.core.AppDispatchers
import com.povarup.core.DashboardLoadState
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
            assertEquals(DashboardLoadState.ERROR, vm.uiState.value.dashboardState)
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

    @Test
    fun duplicateApplyActionIsIgnoredWhileRequestInFlight() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo()
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))

            vm.applyToShift("shift_new")
            vm.applyToShift("shift_new")
            advanceUntilIdle()

            assertEquals(1, repo.applyCalls)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun workerDashboardLoadsPayoutsAndImportantEvents() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply {
                role = "worker"
                session = SessionToken("sess", "worker.demo", "worker")
            }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.loadDashboard()
            advanceUntilIdle()

            assertEquals(1, repo.listPayoutCalls)
            assertTrue(vm.uiState.value.importantEvents.any { it.contains("asn_1") })
            assertTrue(vm.uiState.value.importantEvents.none { it.contains("Offer accepted") })
            assertTrue(vm.uiState.value.payouts.any { it.status == "created" })
        } finally { Dispatchers.resetMain() }
    }


    @Test
    fun payoutFailureIsSurfacedOnDashboardLoad() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply {
                role = "worker"
                session = SessionToken("sess", "worker.demo", "worker")
                payoutsFailure = IllegalStateException("payouts unavailable")
            }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.loadDashboard()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.errorMessage?.contains("payouts unavailable") == true)
            assertEquals(DashboardLoadState.ERROR, vm.uiState.value.dashboardState)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun roleGatingPreventsInvalidActionsWithoutRepositoryCalls() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val businessRepo = FakeRepo().apply {
                role = "business"
                session = SessionToken("sess", "business.demo", "business")
            }
            val businessVm = MainViewModel(businessRepo, AppDispatchers(io = dispatcher))
            businessVm.applyToShift("shift_w")
            advanceUntilIdle()
            assertEquals(0, businessRepo.applyCalls)
            assertTrue(businessVm.uiState.value.errorMessage?.contains("Only workers") == true)

            val workerRepo = FakeRepo().apply {
                role = "worker"
                session = SessionToken("sess", "worker.demo", "worker")
            }
            val workerVm = MainViewModel(workerRepo, AppDispatchers(io = dispatcher))
            workerVm.createShift("Prep", "loc_1", 1200)
            advanceUntilIdle()
            assertEquals(0, workerRepo.createShiftCalls)
            assertTrue(workerVm.uiState.value.errorMessage?.contains("Only businesses") == true)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun applyIsBlockedWhenWorkerAlreadyRelatedToShift() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply {
                role = "worker"
                session = SessionToken("sess", "worker.demo", "worker")
            }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.loadDashboard()
            advanceUntilIdle()

            vm.applyToShift("shift_w")
            advanceUntilIdle()

            assertEquals(0, repo.applyCalls)
            assertTrue(vm.uiState.value.errorMessage?.contains("already applied") == true)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun checkoutIsBlockedWhenAssignmentAlreadyCompleted() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply {
                role = "worker"
                session = SessionToken("sess", "worker.demo", "worker")
                assignmentStatus = "completed"
            }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.loadDashboard()
            advanceUntilIdle()

            vm.checkOut("asn_1")
            advanceUntilIdle()

            assertEquals(0, repo.checkOutCalls)
            assertTrue(vm.uiState.value.errorMessage?.contains("in-progress") == true)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun businessCanPublishDraftShift() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply {
                role = "business"
                session = SessionToken("sess", "business.demo", "business")
            }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.publishShift("shift_biz_1")
            advanceUntilIdle()

            assertEquals(1, repo.publishShiftCalls)
            assertTrue(vm.uiState.value.statusMessage?.contains("published") == true)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun publishShiftIsRoleGuarded() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply {
                role = "worker"
                session = SessionToken("sess", "worker.demo", "worker")
            }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.publishShift("shift_biz_1")
            advanceUntilIdle()

            assertEquals(0, repo.publishShiftCalls)
            assertTrue(vm.uiState.value.errorMessage?.contains("Only businesses can publish") == true)
        } finally { Dispatchers.resetMain() }
    }

    private class FakeRepo : MarketplaceRepository {
        var role: String = "worker"
        var session: SessionToken? = null
        var applyFailure: Throwable? = null
        var shiftsFailure: Throwable? = null
        var businessShiftCalls = 0
        var shiftApplicationsCalls = 0
        var applyCalls = 0
        var createShiftCalls = 0
        var publishShiftCalls = 0
        var checkOutCalls = 0
        var listPayoutCalls = 0
        var payoutsFailure: Throwable? = null
        var assignmentStatus: String = "assigned"

        override fun currentRole(): String = role
        override fun setRole(role: String) { this.role = role }
        override fun baseUrl(): String = "http://localhost"
        override fun currentSession(): SessionToken? = session
        override fun login(userId: String, password: String): Result<SessionToken> = Result.success(SessionToken("sess", userId, role).also { session = it })
        override fun clearSession() { session = null }

        override fun listShifts(): Result<List<Shift>> = shiftsFailure?.let { Result.failure(it) } ?: Result.success(
            listOf(
                Shift("shift_w", "biz", "loc", "Worker shift", "2030-01-01T10:00:00Z", "2030-01-01T14:00:00Z", 1000, "published"),
                Shift("shift_new", "biz", "loc", "Second shift", "2030-01-02T10:00:00Z", "2030-01-02T14:00:00Z", 1000, "published")
            )
        )
        override fun getShift(shiftId: String): Result<Shift> = Result.success(listShifts().getOrThrow().first())
        override fun applyToShift(shiftId: String): Result<Application> {
            applyCalls += 1
            return applyFailure?.let { Result.failure(it) } ?: Result.success(Application("app", shiftId, "worker_1", "applied"))
        }
        override fun listApplications(): Result<List<Application>> = Result.success(listOf(Application("app_worker_1", "shift_w", "worker_1", "applied")))
        override fun withdrawApplication(applicationId: String): Result<Application> = Result.success(Application(applicationId, "shift_w", "worker_1", "withdrawn"))
        override fun rejectApplication(applicationId: String): Result<Application> = Result.success(Application(applicationId, "shift_w", "worker_1", "rejected"))
        override fun listAssignments(): Result<List<Assignment>> = Result.success(listOf(Assignment("asn_1", "shift_w", "worker_1", "biz", assignmentStatus, 1000)))
        override fun getAssignment(assignmentId: String): Result<Assignment> = Result.failure(Exception())
        override fun acceptAssignment(assignmentId: String): Result<Assignment> = Result.failure(Exception())
        override fun checkIn(assignmentId: String): Result<Unit> = Result.success(Unit)
        override fun checkOut(assignmentId: String): Result<Unit> {
            checkOutCalls += 1
            return Result.success(Unit)
        }
        override fun createShift(input: CreateShiftRequest): Result<Shift> {
            createShiftCalls += 1
            return Result.success(Shift("shift_new", "biz", input.locationId, input.title, input.startAt, input.endAt, input.payRateCents, "draft"))
        }
        override fun listBusinessShifts(): Result<List<Shift>> {
            businessShiftCalls += 1
            return shiftsFailure?.let { Result.failure(it) }
                ?: Result.success(listOf(Shift("shift_biz_1", "biz_1", "loc_1", "Owned shift", "", "", 2000, "draft")))
        }

        override fun listShiftApplications(shiftId: String): Result<List<Application>> {
            shiftApplicationsCalls += 1
            return Result.success(listOf(Application("app_business_1", shiftId, "worker_1", "applied")))
        }

        override fun offerAssignment(applicationId: String): Result<Assignment> = Result.failure(Exception())
        override fun publishShift(shiftId: String): Result<Shift> {
            publishShiftCalls += 1
            return Result.success(Shift(shiftId, "biz_1", "loc_1", "Owned shift", "", "", 2000, "published"))
        }
        override fun closeShift(shiftId: String): Result<Shift> = Result.success(Shift(shiftId, "biz_1", "loc_1", "Owned shift", "", "", 2000, "closed"))
        override fun cancelShift(shiftId: String): Result<Shift> = Result.success(Shift(shiftId, "biz_1", "loc_1", "Owned shift", "", "", 2000, "cancelled"))
        override fun cancelAssignment(assignmentId: String): Result<Assignment> = Result.success(Assignment(assignmentId, "shift_w", "worker_1", "biz_1", "cancelled", 1000))
        override fun releasePayout(assignmentId: String): Result<Payout> = Result.failure(Exception())
        override fun listMyPayouts(): Result<List<Payout>> {
            listPayoutCalls += 1
            return payoutsFailure?.let { Result.failure(it) }
                ?: Result.success(listOf(Payout("pay_1", "asn_1", "worker_1", 1000, "created")))
        }
    }
}
