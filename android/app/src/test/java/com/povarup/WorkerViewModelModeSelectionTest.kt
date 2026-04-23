package com.povarup

import com.povarup.core.WorkerDispatchers
import com.povarup.core.WorkerViewModel
import com.povarup.data.CreateShiftRequest
import com.povarup.data.MarketplaceRepository
import com.povarup.data.ProblemCasesDto
import com.povarup.data.SessionToken
import com.povarup.data.WorkerDataSourceMode
import com.povarup.data.WorkerModeSelectable
import com.povarup.domain.Application
import com.povarup.domain.ApplicationStatus
import com.povarup.domain.Assignment
import com.povarup.domain.Payout
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
class WorkerViewModelModeSelectionTest {
    @Test
    fun continueAsDemoWorkerSelectsDemoModeAndLoadsShifts() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = ModeAwareFakeRepository()
            val viewModel = WorkerViewModel(repository, WorkerDispatchers(dispatcher))
            advanceUntilIdle()

            viewModel.continueAsDemoWorker()
            advanceUntilIdle()

            assertEquals(WorkerDataSourceMode.DEMO, repository.selectedMode)
            assertTrue(viewModel.uiState.value.isLoggedIn)
            assertEquals(1, viewModel.uiState.value.shifts.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun loginSelectsRealModeBeforeAuthenticating() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = ModeAwareFakeRepository()
            val viewModel = WorkerViewModel(repository, WorkerDispatchers(dispatcher))
            advanceUntilIdle()

            viewModel.onUserIdChanged("real.user")
            viewModel.onPasswordChanged("secret")
            viewModel.login()
            advanceUntilIdle()

            assertEquals(WorkerDataSourceMode.REAL, repository.selectedMode)
            assertTrue(viewModel.uiState.value.isLoggedIn)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class ModeAwareFakeRepository : MarketplaceRepository, WorkerModeSelectable {
        var selectedMode: WorkerDataSourceMode = WorkerDataSourceMode.REAL
        private var session: SessionToken? = null

        override fun selectMode(mode: WorkerDataSourceMode) {
            selectedMode = mode
        }

        override fun currentRole(): String = session?.role ?: "worker"
        override fun setRole(role: String) = Unit
        override fun baseUrl(): String = "fake"
        override fun currentSession(): SessionToken? = session
        override fun login(userId: String, password: String): Result<SessionToken> {
            val token = if (selectedMode == WorkerDataSourceMode.DEMO) "demo-worker-session-x" else "real-token"
            return Result.success(SessionToken(token = token, userId = userId, role = "worker").also { session = it })
        }

        override fun logout(): Result<Unit> = Result.success(Unit)
        override fun clearSession() { session = null }
        override fun listShifts(): Result<List<Shift>> = Result.success(
            listOf(Shift("s1", "biz", "loc", "Cook", "2030-01-01", "2030-01-01", 2000, ShiftStatus.PUBLISHED, "published"))
        )

        override fun getShift(shiftId: String): Result<Shift> = Result.failure(NotImplementedError())
        override fun applyToShift(shiftId: String): Result<Application> =
            Result.success(Application("a1", shiftId, "w", ApplicationStatus.APPLIED, "applied"))

        override fun listApplications(): Result<List<Application>> = Result.success(emptyList())
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
        override fun listMyPayouts(): Result<List<Payout>> = Result.failure(NotImplementedError())
        override fun listAdminAssignments(): Result<List<Assignment>> = Result.failure(NotImplementedError())
        override fun listAdminPayouts(): Result<List<Payout>> = Result.failure(NotImplementedError())
        override fun updateAdminPayoutStatus(payoutId: String, status: String, note: String?): Result<Payout> = Result.failure(NotImplementedError())
        override fun getAdminProblemCases(): Result<ProblemCasesDto> = Result.failure(NotImplementedError())
    }
}
