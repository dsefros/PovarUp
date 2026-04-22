package com.povarup

import com.povarup.core.AppDispatchers
import com.povarup.core.HomeState
import com.povarup.core.MainAction
import com.povarup.core.MainViewModel
import com.povarup.core.UiLoadState
import com.povarup.data.CreateShiftRequest
import com.povarup.data.MarketplaceRepository
import com.povarup.data.ProblemCasesDto
import com.povarup.data.SessionToken
import com.povarup.domain.Application
import com.povarup.domain.ApplicationStatus
import com.povarup.domain.Assignment
import com.povarup.domain.AssignmentStatus
import com.povarup.domain.Payout
import com.povarup.domain.PayoutStatus
import com.povarup.domain.Shift
import com.povarup.domain.ShiftStatus
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
    fun duplicateInFlightApplyIsIgnored() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply { role = "worker"; session = SessionToken("sess", "worker", "worker") }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.onAction(MainAction.Refresh)
            advanceUntilIdle()

            vm.onAction(MainAction.ApplyToShift("shift_new"))
            vm.onAction(MainAction.ApplyToShift("shift_new"))
            advanceUntilIdle()

            assertEquals(1, repo.applyCalls)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun roleGatingBlocksInvalidActions() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val workerRepo = FakeRepo().apply { role = "worker"; session = SessionToken("sess", "worker", "worker") }
            val workerVm = MainViewModel(workerRepo, AppDispatchers(io = dispatcher))
            workerVm.onAction(MainAction.PublishShift("shift_w"))
            advanceUntilIdle()
            assertEquals(0, workerRepo.publishShiftCalls)
            assertTrue(workerVm.uiState.value.errorMessage?.contains("Only businesses") == true)

            val businessRepo = FakeRepo().apply { role = "business"; session = SessionToken("sess", "biz", "business") }
            val businessVm = MainViewModel(businessRepo, AppDispatchers(io = dispatcher))
            businessVm.onAction(MainAction.ApplyToShift("shift_new"))
            advanceUntilIdle()
            assertEquals(0, businessRepo.applyCalls)
            assertTrue(businessVm.uiState.value.errorMessage?.contains("Only workers") == true)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun workerDashboardLoadsPayoutsEventsAndCapabilities() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply { role = "worker"; session = SessionToken("sess", "worker", "worker") }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.onAction(MainAction.Refresh)
            advanceUntilIdle()

            assertEquals(1, repo.listPayoutCalls)
            assertTrue(vm.uiState.value.home is HomeState.Worker)
            assertTrue(vm.uiState.value.importantEvents.any { it.contains("Assigned") })
            assertTrue(vm.uiState.value.capabilities.canCreateShift.not())
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun businessDashboardUsesOwnedShiftsAndApplicationsPath() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply { role = "business"; session = SessionToken("sess", "biz", "business") }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.onAction(MainAction.Refresh)
            advanceUntilIdle()

            assertEquals(1, repo.businessShiftCalls)
            assertEquals(1, repo.shiftApplicationsCalls)
            assertTrue(vm.uiState.value.home is HomeState.Business)
            assertTrue(vm.uiState.value.capabilities.canCreateShift)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun payoutFailureSurfacesDashboardError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply {
                role = "worker"
                session = SessionToken("sess", "worker", "worker")
                payoutsFailure = IllegalStateException("payouts unavailable")
            }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.onAction(MainAction.Refresh)
            advanceUntilIdle()

            assertEquals(UiLoadState.ERROR, vm.uiState.value.loadState)
            assertTrue(vm.uiState.value.errorMessage?.contains("payouts unavailable") == true)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun capabilityBlockedActionsDoNotCallRepository() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply {
                role = "worker"
                session = SessionToken("sess", "worker", "worker")
                assignmentStatus = AssignmentStatus.COMPLETED
            }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.onAction(MainAction.Refresh)
            advanceUntilIdle()

            vm.onAction(MainAction.CheckOut("asn_1"))
            vm.onAction(MainAction.ApplyToShift("shift_w"))
            advanceUntilIdle()

            assertEquals(0, repo.checkOutCalls)
            assertEquals(0, repo.applyCalls)
            assertTrue(vm.uiState.value.errorMessage?.contains("available") == true || vm.uiState.value.errorMessage?.contains("Check-out") == true)
        } finally { Dispatchers.resetMain() }
    }


    @Test
    fun clearingSelectionsRemovesIdsAndDropsCapabilities() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeRepo().apply {
                role = "worker"
                session = SessionToken("sess", "worker", "worker")
                assignmentStatus = AssignmentStatus.ASSIGNED
            }
            val vm = MainViewModel(repo, AppDispatchers(io = dispatcher))
            vm.onAction(MainAction.Refresh)
            advanceUntilIdle()

            vm.onAction(MainAction.UpdateSelections(shiftId = "shift_new", assignmentId = "asn_1", applicationId = "app_worker_1"))
            assertEquals("shift_new", vm.uiState.value.selectedShiftId)
            assertEquals("asn_1", vm.uiState.value.selectedAssignmentId)
            assertEquals("app_worker_1", vm.uiState.value.selectedApplicationId)
            assertTrue(vm.uiState.value.capabilities.canApplySelectedShift)
            assertTrue(vm.uiState.value.capabilities.canCheckInSelectedAssignment)
            assertTrue(vm.uiState.value.capabilities.canWithdrawSelectedApplication)

            vm.onAction(MainAction.UpdateSelections(shiftId = "", assignmentId = "", applicationId = ""))

            assertEquals(null, vm.uiState.value.selectedShiftId)
            assertEquals(null, vm.uiState.value.selectedAssignmentId)
            assertEquals(null, vm.uiState.value.selectedApplicationId)
            assertTrue(!vm.uiState.value.capabilities.canApplySelectedShift)
            assertTrue(!vm.uiState.value.capabilities.canCheckInSelectedAssignment)
            assertTrue(!vm.uiState.value.capabilities.canWithdrawSelectedApplication)
        } finally { Dispatchers.resetMain() }
    }

    private class FakeRepo : MarketplaceRepository {
        var role: String = "worker"
        var session: SessionToken? = null
        var shiftsFailure: Throwable? = null
        var payoutsFailure: Throwable? = null
        var assignmentStatus: AssignmentStatus = AssignmentStatus.ASSIGNED
        var applyCalls = 0
        var checkOutCalls = 0
        var createShiftCalls = 0
        var publishShiftCalls = 0
        var listPayoutCalls = 0
        var businessShiftCalls = 0
        var shiftApplicationsCalls = 0

        override fun currentRole(): String = role
        override fun setRole(role: String) { this.role = role }
        override fun baseUrl(): String = "http://localhost"
        override fun currentSession(): SessionToken? = session
        override fun login(userId: String, password: String): Result<SessionToken> = Result.success(SessionToken("sess", userId, role).also { session = it })
        override fun logout(): Result<Unit> = Result.success(Unit)
        override fun clearSession() { session = null }
        override fun listShifts(): Result<List<Shift>> = shiftsFailure?.let { Result.failure(it) } ?: Result.success(
            listOf(
                Shift("shift_w", "biz", "loc", "Worker shift", "2030-01-01T10:00:00Z", "2030-01-01T14:00:00Z", 1000, ShiftStatus.PUBLISHED, "published"),
                Shift("shift_new", "biz", "loc", "New shift", "2030-01-02T10:00:00Z", "2030-01-02T14:00:00Z", 1200, ShiftStatus.PUBLISHED, "published")
            )
        )
        override fun getShift(shiftId: String): Result<Shift> = Result.success(listShifts().getOrThrow().first())
        override fun applyToShift(shiftId: String): Result<Application> { applyCalls += 1; return Result.success(Application("app", shiftId, "worker_1", ApplicationStatus.APPLIED, "applied")) }
        override fun listApplications(): Result<List<Application>> = Result.success(listOf(Application("app_worker_1", "shift_w", "worker_1", ApplicationStatus.APPLIED, "applied")))
        override fun withdrawApplication(applicationId: String): Result<Application> = Result.success(Application(applicationId, "shift_w", "worker_1", ApplicationStatus.WITHDRAWN, "withdrawn"))
        override fun rejectApplication(applicationId: String): Result<Application> = Result.success(Application(applicationId, "shift_w", "worker_1", ApplicationStatus.REJECTED, "rejected"))
        override fun listAssignments(): Result<List<Assignment>> = Result.success(listOf(Assignment("asn_1", "shift_w", "worker_1", "biz", assignmentStatus, assignmentStatus.name.lowercase(), 1000)))
        override fun getAssignment(assignmentId: String): Result<Assignment> = Result.failure(Exception())
        override fun acceptAssignment(assignmentId: String): Result<Assignment> = Result.failure(Exception())
        override fun checkIn(assignmentId: String): Result<Unit> = Result.success(Unit)
        override fun checkOut(assignmentId: String): Result<Unit> { checkOutCalls += 1; return Result.success(Unit) }
        override fun createShift(input: CreateShiftRequest): Result<Shift> { createShiftCalls += 1; return Result.success(Shift("shift_new", "biz", input.locationId, input.title, input.startAt, input.endAt, input.payRateCents, ShiftStatus.DRAFT, "draft")) }
        override fun listBusinessShifts(): Result<List<Shift>> { businessShiftCalls += 1; return Result.success(listOf(Shift("shift_biz_1", "biz_1", "loc_1", "Owned shift", "", "", 2000, ShiftStatus.DRAFT, "draft"))) }
        override fun listShiftApplications(shiftId: String): Result<List<Application>> { shiftApplicationsCalls += 1; return Result.success(listOf(Application("app_business_1", shiftId, "worker_1", ApplicationStatus.APPLIED, "applied"))) }
        override fun offerAssignment(applicationId: String): Result<Assignment> = Result.failure(Exception())
        override fun publishShift(shiftId: String): Result<Shift> { publishShiftCalls += 1; return Result.success(Shift(shiftId, "biz", "loc", "Owned", "", "", 2000, ShiftStatus.PUBLISHED, "published")) }
        override fun closeShift(shiftId: String): Result<Shift> = Result.failure(Exception())
        override fun cancelShift(shiftId: String): Result<Shift> = Result.failure(Exception())
        override fun cancelAssignment(assignmentId: String): Result<Assignment> = Result.failure(Exception())
        override fun releasePayout(assignmentId: String): Result<Payout> = Result.failure(Exception())
        override fun listMyPayouts(): Result<List<Payout>> {
            listPayoutCalls += 1
            return payoutsFailure?.let { Result.failure(it) } ?: Result.success(listOf(Payout("pay_1", "asn_1", "worker_1", 1000, PayoutStatus.CREATED, "created")))
        }
        override fun listAdminAssignments(): Result<List<Assignment>> = Result.success(emptyList())
        override fun listAdminPayouts(): Result<List<Payout>> = Result.success(emptyList())
        override fun updateAdminPayoutStatus(payoutId: String, status: String, note: String?): Result<Payout> = Result.failure(Exception())
        override fun getAdminProblemCases(): Result<ProblemCasesDto> = Result.success(ProblemCasesDto())
    }
}
