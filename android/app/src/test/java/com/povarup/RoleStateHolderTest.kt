package com.povarup

import com.povarup.core.RoleStateHolder
import com.povarup.data.CreateShiftRequest
import com.povarup.data.MarketplaceRepository
import com.povarup.data.ProblemCasesDto
import com.povarup.data.SessionToken
import com.povarup.domain.Application
import com.povarup.domain.Assignment
import com.povarup.domain.Payout
import com.povarup.domain.Shift
import org.junit.Assert.assertEquals
import org.junit.Test

class RoleStateHolderTest {
    @Test
    fun switchRoleUpdatesState() {
        val repo = FakeRepo()
        val holder = RoleStateHolder(repo)
        holder.switch("business")
        val state = holder.current()

        assertEquals("business", state.role)
        assertEquals("http://10.0.2.2:4000/api", state.baseUrl)
    }

    private class FakeRepo : MarketplaceRepository {
        private var role: String = "worker"
        override fun currentRole(): String = role
        override fun setRole(role: String) { this.role = role }
        override fun baseUrl(): String = "http://10.0.2.2:4000/api"
        override fun currentSession(): SessionToken? = null
        override fun login(userId: String, password: String): Result<SessionToken> = Result.failure(Exception())
        override fun logout(): Result<Unit> = Result.failure(Exception())
        override fun clearSession() {}
        override fun listShifts(): Result<List<Shift>> = Result.failure(Exception())
        override fun getShift(shiftId: String): Result<Shift> = Result.failure(Exception())
        override fun applyToShift(shiftId: String): Result<Application> = Result.failure(Exception())
        override fun listApplications(): Result<List<Application>> = Result.failure(Exception())
        override fun withdrawApplication(applicationId: String): Result<Application> = Result.failure(Exception())
        override fun rejectApplication(applicationId: String): Result<Application> = Result.failure(Exception())
        override fun listAssignments(): Result<List<Assignment>> = Result.failure(Exception())
        override fun getAssignment(assignmentId: String): Result<Assignment> = Result.failure(Exception())
        override fun acceptAssignment(assignmentId: String): Result<Assignment> = Result.failure(Exception())
        override fun checkIn(assignmentId: String): Result<Unit> = Result.failure(Exception())
        override fun checkOut(assignmentId: String): Result<Unit> = Result.failure(Exception())
        override fun createShift(input: CreateShiftRequest): Result<Shift> = Result.failure(Exception())
        override fun listBusinessShifts(): Result<List<Shift>> = Result.failure(Exception())
        override fun listShiftApplications(shiftId: String): Result<List<Application>> = Result.failure(Exception())
        override fun offerAssignment(applicationId: String): Result<Assignment> = Result.failure(Exception())
        override fun publishShift(shiftId: String): Result<Shift> = Result.failure(Exception())
        override fun closeShift(shiftId: String): Result<Shift> = Result.failure(Exception())
        override fun cancelShift(shiftId: String): Result<Shift> = Result.failure(Exception())
        override fun cancelAssignment(assignmentId: String): Result<Assignment> = Result.failure(Exception())
        override fun releasePayout(assignmentId: String): Result<Payout> = Result.failure(Exception())
        override fun listMyPayouts(): Result<List<Payout>> = Result.failure(Exception())
        override fun listAdminAssignments(): Result<List<Assignment>> = Result.failure(Exception())
        override fun listAdminPayouts(): Result<List<Payout>> = Result.failure(Exception())
        override fun updateAdminPayoutStatus(payoutId: String, status: String, note: String?): Result<Payout> = Result.failure(Exception())
        override fun getAdminProblemCases(): Result<ProblemCasesDto> = Result.failure(Exception())
    }
}
