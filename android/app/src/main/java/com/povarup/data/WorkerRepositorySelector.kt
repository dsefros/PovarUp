package com.povarup.data

class WorkerRepositorySelector(
    private val sessionStore: SessionStore,
    private val realRepository: MarketplaceRepository,
    private val demoRepository: MarketplaceRepository
) : MarketplaceRepository, WorkerModeSelectable {
    private var selectedMode: WorkerDataSourceMode = WorkerDataSourceMode.REAL

    override fun selectMode(mode: WorkerDataSourceMode) {
        selectedMode = mode
    }

    override fun currentRole(): String = activeRepository().currentRole()

    override fun setRole(role: String) {
        activeRepository().setRole(role)
    }

    override fun baseUrl(): String = activeRepository().baseUrl()

    override fun currentSession(): SessionToken? = sessionStore.load()

    override fun login(userId: String, password: String): Result<SessionToken> =
        activeRepository().login(userId, password)

    override fun logout(): Result<Unit> = activeRepository().logout()

    override fun clearSession() {
        realRepository.clearSession()
        demoRepository.clearSession()
        selectedMode = WorkerDataSourceMode.REAL
    }

    override fun listShifts() = activeRepository().listShifts()

    override fun getShift(shiftId: String) = activeRepository().getShift(shiftId)

    override fun applyToShift(shiftId: String) = activeRepository().applyToShift(shiftId)

    override fun listApplications() = activeRepository().listApplications()

    override fun withdrawApplication(applicationId: String) = activeRepository().withdrawApplication(applicationId)

    override fun rejectApplication(applicationId: String) = activeRepository().rejectApplication(applicationId)

    override fun listAssignments() = activeRepository().listAssignments()

    override fun getAssignment(assignmentId: String) = activeRepository().getAssignment(assignmentId)

    override fun acceptAssignment(assignmentId: String) = activeRepository().acceptAssignment(assignmentId)

    override fun checkIn(assignmentId: String) = activeRepository().checkIn(assignmentId)

    override fun checkOut(assignmentId: String) = activeRepository().checkOut(assignmentId)

    override fun createShift(input: CreateShiftRequest) = activeRepository().createShift(input)

    override fun listBusinessShifts() = activeRepository().listBusinessShifts()

    override fun listShiftApplications(shiftId: String) = activeRepository().listShiftApplications(shiftId)

    override fun offerAssignment(applicationId: String) = activeRepository().offerAssignment(applicationId)

    override fun publishShift(shiftId: String) = activeRepository().publishShift(shiftId)

    override fun closeShift(shiftId: String) = activeRepository().closeShift(shiftId)

    override fun cancelShift(shiftId: String) = activeRepository().cancelShift(shiftId)

    override fun cancelAssignment(assignmentId: String) = activeRepository().cancelAssignment(assignmentId)

    override fun releasePayout(assignmentId: String) = activeRepository().releasePayout(assignmentId)

    override fun listMyPayouts() = activeRepository().listMyPayouts()

    override fun listAdminAssignments() = activeRepository().listAdminAssignments()

    override fun listAdminPayouts() = activeRepository().listAdminPayouts()

    override fun updateAdminPayoutStatus(payoutId: String, status: String, note: String?) =
        activeRepository().updateAdminPayoutStatus(payoutId, status, note)

    override fun getAdminProblemCases() = activeRepository().getAdminProblemCases()

    private fun activeRepository(): MarketplaceRepository {
        val session = sessionStore.load()
        if (session != null) {
            return if (isDemoSession(session)) demoRepository else realRepository
        }
        return when (selectedMode) {
            WorkerDataSourceMode.REAL -> realRepository
            WorkerDataSourceMode.DEMO -> demoRepository
        }
    }

    companion object {
        fun isDemoSession(session: SessionToken): Boolean =
            session.token.startsWith(DemoMarketplaceRepository.DEMO_TOKEN_PREFIX)
    }
}
