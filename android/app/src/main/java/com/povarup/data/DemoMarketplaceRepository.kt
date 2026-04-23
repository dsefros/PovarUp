package com.povarup.data

import com.povarup.domain.Application
import com.povarup.domain.ApplicationStatus
import com.povarup.domain.Assignment
import com.povarup.domain.Payout
import com.povarup.domain.Shift
import com.povarup.domain.ShiftStatus
import com.povarup.domain.UserRole

class DemoMarketplaceRepository(
    private val sessionStore: SessionStore = InMemorySessionStore()
) : MarketplaceRepository {
    private var role: String = sessionStore.load()?.role ?: UserRole.WORKER.asApiValue()
    private val applicationsByWorker = mutableMapOf(
        DEMO_USER_ID to mutableListOf(
            Application(
                id = "demo-app-1",
                shiftId = "demo-shift-2",
                workerId = DEMO_USER_ID,
                status = ApplicationStatus.APPLIED,
                rawStatus = "applied"
            )
        )
    )

    override fun currentRole(): String = role

    override fun setRole(role: String) {
        this.role = role
    }

    override fun baseUrl(): String = "demo://worker"

    override fun currentSession(): SessionToken? = sessionStore.load()

    override fun login(userId: String, password: String): Result<SessionToken> {
        if (userId != DEMO_USER_ID || password != DEMO_PASSWORD) {
            return Result.failure(IllegalArgumentException("Invalid demo credentials"))
        }

        return Result.success(
            SessionToken(token = DEMO_TOKEN, userId = DEMO_USER_ID, role = UserRole.WORKER.asApiValue()).also {
                sessionStore.save(it)
                setRole(it.role)
            }
        )
    }

    override fun logout(): Result<Unit> {
        sessionStore.clear()
        role = UserRole.WORKER.asApiValue()
        return Result.success(Unit)
    }

    override fun clearSession() {
        sessionStore.clear()
        role = UserRole.WORKER.asApiValue()
    }

    override fun listShifts(): Result<List<Shift>> {
        if (currentSession() == null) return notAuthenticated()
        return Result.success(DEMO_SHIFTS)
    }

    override fun getShift(shiftId: String): Result<Shift> = unsupported()

    override fun applyToShift(shiftId: String): Result<Application> {
        val session = currentSession() ?: return notAuthenticated()
        val shift = DEMO_SHIFTS.firstOrNull { it.id == shiftId }
            ?: return Result.failure(IllegalArgumentException("Shift not found"))

        if (shift.status != ShiftStatus.PUBLISHED) {
            return Result.failure(IllegalStateException("Shift is not accepting applications"))
        }

        val workerApplications = applicationsForWorker(session.userId)
        if (workerApplications.any { it.shiftId == shiftId }) {
            return Result.failure(IllegalStateException("Application already exists"))
        }

        val created = Application(
            id = "demo-app-${workerApplications.size + 1}",
            shiftId = shiftId,
            workerId = session.userId,
            status = ApplicationStatus.APPLIED,
            rawStatus = "applied"
        )
        workerApplications += created
        return Result.success(created)
    }

    override fun listApplications(): Result<List<Application>> {
        val session = currentSession() ?: return notAuthenticated()
        return Result.success(applicationsForWorker(session.userId).toList())
    }

    override fun withdrawApplication(applicationId: String): Result<Application> = unsupported()

    override fun rejectApplication(applicationId: String): Result<Application> = unsupported()

    override fun listAssignments(): Result<List<Assignment>> = Result.success(emptyList())

    override fun getAssignment(assignmentId: String): Result<Assignment> = unsupported()

    override fun acceptAssignment(assignmentId: String): Result<Assignment> = unsupported()

    override fun checkIn(assignmentId: String): Result<Unit> = unsupported()

    override fun checkOut(assignmentId: String): Result<Unit> = unsupported()

    override fun createShift(input: CreateShiftRequest): Result<Shift> = unsupported()

    override fun listBusinessShifts(): Result<List<Shift>> = unsupported()

    override fun listShiftApplications(shiftId: String): Result<List<Application>> = unsupported()

    override fun offerAssignment(applicationId: String): Result<Assignment> = unsupported()

    override fun publishShift(shiftId: String): Result<Shift> = unsupported()

    override fun closeShift(shiftId: String): Result<Shift> = unsupported()

    override fun cancelShift(shiftId: String): Result<Shift> = unsupported()

    override fun cancelAssignment(assignmentId: String): Result<Assignment> = unsupported()

    override fun releasePayout(assignmentId: String): Result<Payout> = unsupported()

    override fun listMyPayouts(): Result<List<Payout>> = unsupported()

    override fun listAdminAssignments(): Result<List<Assignment>> = unsupported()

    override fun listAdminPayouts(): Result<List<Payout>> = unsupported()

    override fun updateAdminPayoutStatus(payoutId: String, status: String, note: String?): Result<Payout> = unsupported()

    override fun getAdminProblemCases(): Result<ProblemCasesDto> = unsupported()

    private fun applicationsForWorker(workerId: String): MutableList<Application> =
        applicationsByWorker.getOrPut(workerId) { mutableListOf() }

    private fun <T> unsupported(): Result<T> =
        Result.failure(NotImplementedError("Not implemented in worker demo repository"))

    private fun <T> notAuthenticated(): Result<T> =
        Result.failure(IllegalStateException("Not authenticated"))

    companion object {
        const val DEMO_USER_ID = "worker.demo"
        const val DEMO_PASSWORD = "workerpass"
        const val DEMO_TOKEN_PREFIX = "demo-worker-session"
        private const val DEMO_TOKEN = "$DEMO_TOKEN_PREFIX-v1"

        private val DEMO_SHIFTS = listOf(
            Shift(
                id = "demo-shift-1",
                businessId = "biz-brooklyn-bistro",
                locationId = "Brooklyn Bistro · Downtown",
                title = "Prep Cook",
                startAt = "2026-05-10 08:00",
                endAt = "2026-05-10 14:00",
                payRateCents = 2200,
                status = ShiftStatus.PUBLISHED,
                rawStatus = "published"
            ),
            Shift(
                id = "demo-shift-2",
                businessId = "biz-harbor-grill",
                locationId = "Harbor Grill · Pier 3",
                title = "Line Cook",
                startAt = "2026-05-11 15:00",
                endAt = "2026-05-11 22:00",
                payRateCents = 2600,
                status = ShiftStatus.PUBLISHED,
                rawStatus = "published"
            ),
            Shift(
                id = "demo-shift-3",
                businessId = "biz-market-table",
                locationId = "Market Table · Midtown",
                title = "Dishwasher",
                startAt = "2026-05-12 09:00",
                endAt = "2026-05-12 15:00",
                payRateCents = 1900,
                status = ShiftStatus.CLOSED,
                rawStatus = "closed"
            ),
            Shift(
                id = "demo-shift-4",
                businessId = "biz-sunset-kitchen",
                locationId = "Sunset Kitchen · West End",
                title = "Grill Cook",
                startAt = "2026-05-13 16:00",
                endAt = "2026-05-13 23:00",
                payRateCents = 2800,
                status = ShiftStatus.PUBLISHED,
                rawStatus = "published"
            )
        )
    }
}
