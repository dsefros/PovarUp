package com.povarup.data

import com.povarup.domain.Application
import com.povarup.domain.ApplicationStatus
import com.povarup.domain.Assignment
import com.povarup.domain.CookCuisine
import com.povarup.domain.CookStation
import com.povarup.domain.DishwasherZone
import com.povarup.domain.Payout
import com.povarup.domain.Shift
import com.povarup.domain.ShiftStatus
import com.povarup.domain.UserRole
import com.povarup.domain.WorkType

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
    private val businessShifts = mutableListOf(
        Shift(
            id = "demo-business-shift-1",
            businessId = DEMO_BUSINESS_USER_ID,
            locationId = "Nevsky Garden · Manhattan",
            title = "Banquet Head Cook",
            startAt = "2026-05-10 10:00",
            endAt = "2026-05-10 18:00",
            payRateCents = 3400,
            status = ShiftStatus.DRAFT,
            rawStatus = "draft",
            workType = WorkType.COOK,
            cookCuisine = CookCuisine.RUSSIAN,
            cookStation = CookStation.HOT,
            isBanquet = true
        ),
        Shift(
            id = "demo-business-shift-2",
            businessId = DEMO_BUSINESS_USER_ID,
            locationId = "Belle Époque · SoHo",
            title = "Floor Waiter",
            startAt = "2026-05-12 12:00",
            endAt = "2026-05-12 20:00",
            payRateCents = 2500,
            status = ShiftStatus.PUBLISHED,
            rawStatus = "published",
            workType = WorkType.WAITER,
            isBanquet = false
        )
    )

    override fun currentRole(): String = role

    override fun setRole(role: String) {
        this.role = role
    }

    override fun baseUrl(): String = "demo://worker"

    override fun currentSession(): SessionToken? = sessionStore.load()

    override fun login(userId: String, password: String): Result<SessionToken> {
        val token = when {
            userId == DEMO_USER_ID && password == DEMO_PASSWORD -> SessionToken(token = DEMO_TOKEN, userId = DEMO_USER_ID, role = UserRole.WORKER.asApiValue())
            userId == DEMO_BUSINESS_USER_ID && password == DEMO_BUSINESS_PASSWORD -> SessionToken(token = DEMO_BUSINESS_TOKEN, userId = DEMO_BUSINESS_USER_ID, role = UserRole.BUSINESS.asApiValue())
            else -> return Result.failure(IllegalArgumentException("Invalid demo credentials"))
        }

        return Result.success(token.also {
            sessionStore.save(it)
            setRole(it.role)
        })
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

    override fun createShift(input: CreateShiftRequest): Result<Shift> {
        val session = currentSession() ?: return notAuthenticated()
        if (UserRole.from(session.role) != UserRole.BUSINESS) return Result.failure(IllegalStateException("Business role required"))
        if (input.title.isBlank()) return Result.failure(IllegalArgumentException("Title is required"))
        if (input.locationId.isBlank()) return Result.failure(IllegalArgumentException("Location is required"))
        if (input.payRateCents <= 0) return Result.failure(IllegalArgumentException("Pay must be positive"))

        val created = Shift(
            id = "demo-business-shift-${businessShifts.size + 1}",
            businessId = session.userId,
            locationId = input.locationId,
            title = input.title,
            startAt = input.startAt,
            endAt = input.endAt,
            payRateCents = input.payRateCents,
            status = ShiftStatus.DRAFT,
            rawStatus = "draft"
        )
        businessShifts.add(0, created)
        return Result.success(created)
    }

    override fun listBusinessShifts(): Result<List<Shift>> {
        val session = currentSession() ?: return notAuthenticated()
        if (UserRole.from(session.role) != UserRole.BUSINESS) return Result.failure(IllegalStateException("Business role required"))
        return Result.success(businessShifts.filter { it.businessId == session.userId })
    }

    override fun listShiftApplications(shiftId: String): Result<List<Application>> = unsupported()

    override fun offerAssignment(applicationId: String): Result<Assignment> = unsupported()

    override fun publishShift(shiftId: String): Result<Shift> = transitionShift(
        shiftId = shiftId,
        from = setOf(ShiftStatus.DRAFT),
        to = ShiftStatus.PUBLISHED
    )

    override fun closeShift(shiftId: String): Result<Shift> = transitionShift(
        shiftId = shiftId,
        from = setOf(ShiftStatus.PUBLISHED),
        to = ShiftStatus.CLOSED
    )

    override fun cancelShift(shiftId: String): Result<Shift> = transitionShift(
        shiftId = shiftId,
        from = setOf(ShiftStatus.DRAFT, ShiftStatus.PUBLISHED, ShiftStatus.CLOSED),
        to = ShiftStatus.CANCELLED
    )

    override fun cancelAssignment(assignmentId: String): Result<Assignment> = unsupported()

    override fun releasePayout(assignmentId: String): Result<Payout> = unsupported()

    override fun listMyPayouts(): Result<List<Payout>> = unsupported()

    override fun listAdminAssignments(): Result<List<Assignment>> = unsupported()

    override fun listAdminPayouts(): Result<List<Payout>> = unsupported()

    override fun updateAdminPayoutStatus(payoutId: String, status: String, note: String?): Result<Payout> = unsupported()

    override fun getAdminProblemCases(): Result<ProblemCasesDto> = unsupported()

    private fun applicationsForWorker(workerId: String): MutableList<Application> =
        applicationsByWorker.getOrPut(workerId) { mutableListOf() }

    private fun transitionShift(shiftId: String, from: Set<ShiftStatus>, to: ShiftStatus): Result<Shift> {
        val session = currentSession() ?: return notAuthenticated()
        if (UserRole.from(session.role) != UserRole.BUSINESS) return Result.failure(IllegalStateException("Business role required"))
        val index = businessShifts.indexOfFirst { it.id == shiftId && it.businessId == session.userId }
        if (index == -1) return Result.failure(IllegalArgumentException("Shift not found"))
        val current = businessShifts[index]
        if (current.status !in from) return Result.failure(IllegalStateException("Shift transition is not allowed"))

        val updated = current.copy(status = to, rawStatus = to.name.lowercase())
        businessShifts[index] = updated
        return Result.success(updated)
    }

    private fun <T> unsupported(): Result<T> =
        Result.failure(NotImplementedError("Not implemented in worker demo repository"))

    private fun <T> notAuthenticated(): Result<T> =
        Result.failure(IllegalStateException("Not authenticated"))

    companion object {
        const val DEMO_USER_ID = "worker.demo"
        const val DEMO_PASSWORD = "workerpass"
        const val DEMO_TOKEN_PREFIX = "demo-worker-session"
        private const val DEMO_TOKEN = "$DEMO_TOKEN_PREFIX-v1"
        const val DEMO_BUSINESS_USER_ID = "business.demo"
        const val DEMO_BUSINESS_PASSWORD = "businesspass"
        const val DEMO_BUSINESS_TOKEN_PREFIX = "demo-business-session"
        private const val DEMO_BUSINESS_TOKEN = "$DEMO_BUSINESS_TOKEN_PREFIX-v1"

        private val DEMO_SHIFTS = listOf(
            Shift(
                id = "demo-shift-1",
                businessId = "biz-nevsky-garden",
                locationId = "Nevsky Garden · Manhattan",
                title = "Banquet Head Cook",
                startAt = "2026-05-10 10:00",
                endAt = "2026-05-10 18:00",
                payRateCents = 3400,
                status = ShiftStatus.PUBLISHED,
                rawStatus = "published",
                workType = WorkType.COOK,
                cookCuisine = CookCuisine.RUSSIAN,
                cookStation = CookStation.HOT,
                isBanquet = true
            ),
            Shift(
                id = "demo-shift-2",
                businessId = "biz-nevsky-garden",
                locationId = "Nevsky Garden · Manhattan",
                title = "Garde Manger Cook",
                startAt = "2026-05-11 08:00",
                endAt = "2026-05-11 15:00",
                payRateCents = 2800,
                status = ShiftStatus.PUBLISHED,
                rawStatus = "published",
                workType = WorkType.COOK,
                cookCuisine = CookCuisine.RUSSIAN,
                cookStation = CookStation.COLD,
                isBanquet = false
            ),
            Shift(
                id = "demo-shift-3",
                businessId = "biz-belle-epoque",
                locationId = "Belle Époque · SoHo",
                title = "French Kitchen Cook",
                startAt = "2026-05-12 12:00",
                endAt = "2026-05-12 20:00",
                payRateCents = 3100,
                status = ShiftStatus.PUBLISHED,
                rawStatus = "published",
                workType = WorkType.COOK,
                cookCuisine = CookCuisine.FRENCH,
                cookStation = CookStation.UNIVERSAL,
                isBanquet = false
            ),
            Shift(
                id = "demo-shift-4",
                businessId = "biz-trattoria-via",
                locationId = "Trattoria Via Roma · Brooklyn",
                title = "Hot Line Italian Cook",
                startAt = "2026-05-13 14:00",
                endAt = "2026-05-13 22:00",
                payRateCents = 2950,
                status = ShiftStatus.PUBLISHED,
                rawStatus = "published",
                workType = WorkType.COOK,
                cookCuisine = CookCuisine.ITALIAN,
                cookStation = CookStation.HOT,
                isBanquet = false
            ),
            Shift(
                id = "demo-shift-5",
                businessId = "biz-harbor-club",
                locationId = "Harbor Club · Pier 7",
                title = "Floor Waiter",
                startAt = "2026-05-14 11:00",
                endAt = "2026-05-14 19:00",
                payRateCents = 2300,
                status = ShiftStatus.PUBLISHED,
                rawStatus = "published",
                workType = WorkType.WAITER,
                isBanquet = false
            ),
            Shift(
                id = "demo-shift-6",
                businessId = "biz-harbor-club",
                locationId = "Harbor Club · Pier 7",
                title = "Banquet Waiter",
                startAt = "2026-05-15 16:00",
                endAt = "2026-05-15 23:30",
                payRateCents = 2600,
                status = ShiftStatus.PUBLISHED,
                rawStatus = "published",
                workType = WorkType.WAITER,
                isBanquet = true
            ),
            Shift(
                id = "demo-shift-7",
                businessId = "biz-velvet-lounge",
                locationId = "Velvet Lounge · Midtown",
                title = "Service Bartender",
                startAt = "2026-05-16 17:00",
                endAt = "2026-05-17 01:00",
                payRateCents = 3000,
                status = ShiftStatus.PUBLISHED,
                rawStatus = "published",
                workType = WorkType.BARTENDER,
                isBanquet = false
            ),
            Shift(
                id = "demo-shift-8",
                businessId = "biz-velvet-lounge",
                locationId = "Velvet Lounge · Midtown",
                title = "Banquet Bar Bartender",
                startAt = "2026-05-17 15:00",
                endAt = "2026-05-17 23:00",
                payRateCents = 3200,
                status = ShiftStatus.PUBLISHED,
                rawStatus = "published",
                workType = WorkType.BARTENDER,
                isBanquet = true
            ),
            Shift(
                id = "demo-shift-9",
                businessId = "biz-city-hotel",
                locationId = "City Hotel Kitchen · Uptown",
                title = "Dishwasher (White Zone)",
                startAt = "2026-05-18 07:00",
                endAt = "2026-05-18 15:00",
                payRateCents = 2000,
                status = ShiftStatus.PUBLISHED,
                rawStatus = "published",
                workType = WorkType.DISHWASHER,
                dishwasherZone = DishwasherZone.WHITE
            ),
            Shift(
                id = "demo-shift-10",
                businessId = "biz-city-hotel",
                locationId = "City Hotel Kitchen · Uptown",
                title = "Dishwasher (Black Zone)",
                startAt = "2026-05-18 15:00",
                endAt = "2026-05-18 23:00",
                payRateCents = 2100,
                status = ShiftStatus.PUBLISHED,
                rawStatus = "published",
                workType = WorkType.DISHWASHER,
                dishwasherZone = DishwasherZone.BLACK
            )
        )
    }
}
