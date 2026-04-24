package com.povarup

import com.povarup.core.WorkerDispatchers
import com.povarup.core.WorkerViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerViewModelTest {
    @Test
    fun workerLoginSuccessLoadsShifts() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeWorkerRepo()
            val vm = WorkerViewModel(repo, WorkerDispatchers(dispatcher))
            vm.onUserIdChanged("worker.demo")
            vm.onPasswordChanged("workerpass")
            vm.login()
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isLoggedIn)
            assertFalse(vm.uiState.value.isLoggingIn)
            assertEquals(2, vm.uiState.value.shifts.size)
            assertEquals(1, vm.uiState.value.payoutsCount)
            assertEquals("В обработке", vm.uiState.value.payouts.firstOrNull()?.statusLabel)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun restoresWorkerSessionAndLoadsShifts() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeWorkerRepo().apply { session = SessionToken("t", "worker.demo", "worker") }
            val vm = WorkerViewModel(repo, WorkerDispatchers(dispatcher))
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isLoggedIn)
            assertEquals(2, vm.uiState.value.shifts.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun roleGuardRejectsBusinessLogin() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeWorkerRepo().apply { loginRole = "business" }
            val vm = WorkerViewModel(repo, WorkerDispatchers(dispatcher))
            vm.onUserIdChanged("business.demo")
            vm.onPasswordChanged("businesspass")
            vm.login()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoggedIn)
            assertTrue(vm.uiState.value.errorMessage?.contains("worker accounts only") == true)
            assertEquals(1, repo.logoutCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun applyDisablesButtonAndReloadsState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeWorkerRepo().apply { session = SessionToken("t", "worker.demo", "worker") }
            val vm = WorkerViewModel(repo, WorkerDispatchers(dispatcher))
            advanceUntilIdle()

            vm.applyToShift("shift_2")
            val inFlight = vm.uiState.value.shifts.first { it.id == "shift_2" }
            assertFalse(inFlight.canApply)
            assertTrue(inFlight.isApplying)

            advanceUntilIdle()
            val after = vm.uiState.value.shifts.first { it.id == "shift_2" }
            assertFalse(after.canApply)
            assertFalse(after.isApplying)
            assertEquals(1, repo.applyCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeWorkerRepo : MarketplaceRepository {
        var session: SessionToken? = null
        var loginFailure: Throwable? = null
        var loginRole: String = "worker"
        var applyCalls = 0
        var logoutCalls = 0
        private val applications = mutableListOf(Application("app_1", "shift_1", "worker.demo", ApplicationStatus.APPLIED, "applied"))

        override fun currentRole(): String = session?.role ?: "worker"
        override fun setRole(role: String) { session = session?.copy(role = role) }
        override fun baseUrl(): String = "http://localhost"
        override fun currentSession(): SessionToken? = session
        override fun login(userId: String, password: String): Result<SessionToken> =
            loginFailure?.let { Result.failure(it) } ?: Result.success(SessionToken("token", userId, loginRole).also { session = it })
        override fun logout(): Result<Unit> { logoutCalls += 1; session = null; return Result.success(Unit) }
        override fun clearSession() { session = null }
        override fun listShifts(): Result<List<Shift>> = Result.success(
            listOf(
                Shift("shift_1", "biz", "loc_1", "Prep Cook", "2030-01-01T10:00:00Z", "2030-01-01T14:00:00Z", 2200, ShiftStatus.PUBLISHED, "published"),
                Shift("shift_2", "biz", "loc_2", "Line Cook", "2030-01-02T10:00:00Z", "2030-01-02T14:00:00Z", 2600, ShiftStatus.PUBLISHED, "published")
            )
        )

        override fun getShift(shiftId: String): Result<Shift> = Result.failure(NotImplementedError())
        override fun applyToShift(shiftId: String): Result<Application> {
            applyCalls += 1
            val app = Application("app_new", shiftId, "worker.demo", ApplicationStatus.APPLIED, "applied")
            applications += app
            return Result.success(app)
        }

        override fun listApplications(): Result<List<Application>> = Result.success(applications.toList())
        override fun withdrawApplication(applicationId: String): Result<Application> = Result.failure(NotImplementedError())
        override fun rejectApplication(applicationId: String): Result<Application> = Result.failure(NotImplementedError())
        override fun listAssignments(): Result<List<Assignment>> = Result.success(emptyList())
        override fun getAssignment(assignmentId: String): Result<Assignment> = Result.failure(NotImplementedError())
        override fun acceptAssignment(assignmentId: String): Result<Assignment> = Result.failure(NotImplementedError())
        override fun checkIn(assignmentId: String): Result<Unit> = Result.failure(NotImplementedError())
        override fun checkOut(assignmentId: String): Result<Unit> = Result.failure(NotImplementedError())
        override fun createShift(input: CreateShiftRequest): Result<Shift> = Result.failure(NotImplementedError())
        override fun listBusinessShifts(): Result<List<Shift>> = Result.failure(NotImplementedError())
        override fun listShiftApplications(shiftId: String): Result<List<Application>> = Result.failure(NotImplementedError())
        override fun offerAssignment(applicationId: String): Result<Assignment> = Result.failure(NotImplementedError())
        override fun publishShift(shiftId: String): Result<Shift> = Result.failure(NotImplementedError())
        override fun closeShift(shiftId: String): Result<Shift> = Result.failure(NotImplementedError())
        override fun cancelShift(shiftId: String): Result<Shift> = Result.failure(NotImplementedError())
        override fun cancelAssignment(assignmentId: String): Result<Assignment> = Result.failure(NotImplementedError())
        override fun releasePayout(assignmentId: String): Result<Payout> = Result.failure(NotImplementedError())
        override fun listMyPayouts(): Result<List<Payout>> = Result.success(
            listOf(
                Payout("payout_12345678", "assignment_1", "worker.demo", 12345, PayoutStatus.PENDING, "pending")
            )
        )
        override fun listAdminAssignments(): Result<List<Assignment>> = Result.success(emptyList())
        override fun listAdminPayouts(): Result<List<Payout>> = Result.success(listOf(Payout("p", "a", "w", 1, PayoutStatus.CREATED, "created")))
        override fun updateAdminPayoutStatus(payoutId: String, status: String, note: String?): Result<Payout> = Result.failure(NotImplementedError())
        override fun getAdminProblemCases(): Result<ProblemCasesDto> = Result.success(ProblemCasesDto())
    }
}
