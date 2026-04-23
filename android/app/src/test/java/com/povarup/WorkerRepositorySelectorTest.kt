package com.povarup

import com.povarup.data.CreateShiftRequest
import com.povarup.data.DemoMarketplaceRepository
import com.povarup.data.InMemorySessionStore
import com.povarup.data.MarketplaceRepository
import com.povarup.data.ProblemCasesDto
import com.povarup.data.SessionToken
import com.povarup.data.WorkerDataSourceMode
import com.povarup.data.WorkerRepositorySelector
import com.povarup.domain.Application
import com.povarup.domain.Assignment
import com.povarup.domain.Payout
import com.povarup.domain.Shift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkerRepositorySelectorTest {
    @Test
    fun selectsDemoModeExplicitlyWhenNoSession() {
        val store = InMemorySessionStore()
        val realRepo = CountingRepo(name = "real")
        val demoRepo = CountingRepo(name = "demo")
        val selector = WorkerRepositorySelector(store, realRepo, demoRepo)

        selector.selectMode(WorkerDataSourceMode.DEMO)
        selector.login("worker.demo", "workerpass")

        assertEquals(0, realRepo.loginCalls)
        assertEquals(1, demoRepo.loginCalls)
    }

    @Test
    fun defaultsToRealAfterClearWithoutPersistedModeFlag() {
        val store = InMemorySessionStore()
        val realRepo = CountingRepo(name = "real")
        val demoRepo = CountingRepo(name = "demo")
        val selector = WorkerRepositorySelector(store, realRepo, demoRepo)

        selector.selectMode(WorkerDataSourceMode.DEMO)
        selector.clearSession()
        selector.login("real.user", "real-pass")

        assertEquals(1, realRepo.loginCalls)
        assertEquals(0, demoRepo.loginCalls)
    }

    @Test
    fun restoresDemoModeFromSessionTokenWithoutExtraFlag() {
        val store = InMemorySessionStore().apply {
            save(SessionToken("${DemoMarketplaceRepository.DEMO_TOKEN_PREFIX}-restored", "worker.demo", "worker"))
        }
        val realRepo = CountingRepo(name = "real")
        val demoRepo = CountingRepo(name = "demo")
        val selector = WorkerRepositorySelector(store, realRepo, demoRepo)

        selector.listShifts()

        assertEquals(0, realRepo.listShiftsCalls)
        assertEquals(1, demoRepo.listShiftsCalls)
    }

    @Test
    fun demoSessionDetectionUsesTokenPrefixOnly() {
        val demoSession = SessionToken("${DemoMarketplaceRepository.DEMO_TOKEN_PREFIX}-xyz", "worker.demo", "worker")
        val realSession = SessionToken("jwt-token", "worker.real", "worker")

        assertTrue(WorkerRepositorySelector.isDemoSession(demoSession))
        assertFalse(WorkerRepositorySelector.isDemoSession(realSession))
    }

    private class CountingRepo(private val name: String) : MarketplaceRepository {
        var loginCalls = 0
        var listShiftsCalls = 0

        override fun currentRole(): String = "worker"
        override fun setRole(role: String) = Unit
        override fun baseUrl(): String = name
        override fun currentSession(): SessionToken? = null
        override fun login(userId: String, password: String): Result<SessionToken> {
            loginCalls += 1
            return Result.success(SessionToken("token-$name", userId, "worker"))
        }

        override fun logout(): Result<Unit> = Result.success(Unit)
        override fun clearSession() = Unit
        override fun listShifts(): Result<List<Shift>> { listShiftsCalls += 1; return Result.success(emptyList()) }
        override fun getShift(shiftId: String): Result<Shift> = Result.failure(NotImplementedError())
        override fun applyToShift(shiftId: String): Result<Application> = Result.failure(NotImplementedError())
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
