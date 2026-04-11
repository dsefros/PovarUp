package com.povarup.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.CreateShiftRequest
import com.povarup.data.MarketplaceRepository
import com.povarup.domain.Application
import com.povarup.domain.Assignment
import com.povarup.domain.Payout
import com.povarup.domain.Shift
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

data class AppDispatchers(val io: CoroutineDispatcher = Dispatchers.IO)

enum class DashboardLoadState { IDLE, LOADING, READY, ERROR }

data class MainUiState(
    val role: String,
    val baseUrl: String,
    val hasSession: Boolean = false,
    val sessionUserId: String? = null,
    val shifts: List<Shift> = emptyList(),
    val applications: List<Application> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val payouts: List<Payout> = emptyList(),
    val selectedShiftId: String? = null,
    val selectedAssignmentId: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val dashboardState: DashboardLoadState = DashboardLoadState.IDLE,
    val inFlightActionKeys: Set<String> = emptySet(),
    val importantEvents: List<String> = emptyList()
)

class MainViewModel(
    private val repository: MarketplaceRepository = ApiMarketplaceRepository(),
    private val dispatchers: AppDispatchers = AppDispatchers()
) : ViewModel() {
    private val roleState = RoleStateHolder(repository).current()
    private val _uiState = MutableStateFlow(
        MainUiState(
            role = roleState.role,
            baseUrl = roleState.baseUrl,
            hasSession = repository.currentSession() != null,
            sessionUserId = repository.currentSession()?.userId
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun login(userId: String, password: String) {
        runAsync("login", "Logged in") { repository.login(userId.trim(), password.trim()) }
    }

    fun setSelectedShift(shiftId: String) {
        _uiState.value = _uiState.value.copy(selectedShiftId = shiftId.trim().ifBlank { null })
    }

    fun loadDashboard() {
        if (_uiState.value.dashboardState == DashboardLoadState.LOADING) return
        _uiState.value = _uiState.value.copy(dashboardState = DashboardLoadState.LOADING, errorMessage = null)
        viewModelScope.launch(dispatchers.io) {
            refreshDashboardState(keepStatusMessage = true)
        }
    }

    fun applyToShift(shiftId: String) {
        if (!guardRole("worker", "Only workers can apply to shifts.")) return
        val normalizedShiftId = shiftId.trim()
        val alreadyRelated = _uiState.value.applications.any { it.shiftId == normalizedShiftId } ||
            _uiState.value.assignments.any { it.shiftId == normalizedShiftId }
        if (alreadyRelated) {
            _uiState.value = _uiState.value.copy(errorMessage = "You already applied or were assigned to this shift.", statusMessage = null)
            return
        }
        runAsync("apply:${normalizedShiftId}", "Applied") { repository.applyToShift(normalizedShiftId) }
    }

    fun acceptAssignment(assignmentId: String) {
        if (!guardRole("worker", "Only workers can accept offers.")) return
        runAsync("accept:${assignmentId.trim()}", "Offer accepted") { repository.acceptAssignment(assignmentId.trim()) }
    }

    fun checkIn(assignmentId: String) {
        if (!guardRole("worker", "Only workers can check in.")) return
        runAsync("checkin:${assignmentId.trim()}", "Check-in completed") { repository.checkIn(assignmentId.trim()) }
    }

    fun checkOut(assignmentId: String) {
        if (!guardRole("worker", "Only workers can check out.")) return
        val normalizedAssignmentId = assignmentId.trim()
        val assignment = _uiState.value.assignments.firstOrNull { it.id == normalizedAssignmentId }
        if (assignment != null && assignment.status != "in_progress") {
            _uiState.value = _uiState.value.copy(errorMessage = "Check-out is only available for in-progress shifts.", statusMessage = null)
            return
        }
        runAsync("checkout:${normalizedAssignmentId}", "Check-out completed") { repository.checkOut(normalizedAssignmentId) }
    }

    fun offerAssignment(applicationId: String) {
        if (!guardRole("business", "Only businesses can offer assignments.")) return
        runAsync("offer:${applicationId.trim()}", "Offer sent") { repository.offerAssignment(applicationId.trim()) }
    }

    fun releasePayout(assignmentId: String) {
        if (!guardRole("business", "Only businesses can release payouts.")) return
        runAsync("release:${assignmentId.trim()}", "Payout released") { repository.releasePayout(assignmentId.trim()) }
    }

    fun createShift(title: String, locationId: String, payRateCents: Int) {
        if (!guardRole("business", "Only businesses can create shifts.")) return
        runAsync("create_shift", "Shift created") {
            repository.createShift(
                CreateShiftRequest(
                    locationId = locationId.trim(),
                    title = title.trim(),
                    startAt = Instant.now().toString(),
                    endAt = Instant.now().plusSeconds(7200).toString(),
                    payRateCents = payRateCents
                )
            )
        }
    }

    fun clearSession() {
        repository.clearSession()
        _uiState.value = _uiState.value.copy(
            hasSession = false,
            sessionUserId = null,
            statusMessage = "Session cleared",
            errorMessage = null,
            dashboardState = DashboardLoadState.IDLE,
            shifts = emptyList(),
            applications = emptyList(),
            assignments = emptyList(),
            payouts = emptyList(),
            importantEvents = emptyList()
        )
    }

    fun isActionInFlight(actionKey: String): Boolean = _uiState.value.inFlightActionKeys.contains(actionKey)

    private fun guardRole(expectedRole: String, message: String): Boolean {
        if (repository.currentRole() == expectedRole) return true
        _uiState.value = _uiState.value.copy(errorMessage = message, statusMessage = null)
        return false
    }

    private fun <T> runAsync(actionKey: String, successMsg: String, action: () -> Result<T>) {
        if (_uiState.value.inFlightActionKeys.contains(actionKey)) return
        _uiState.value = _uiState.value.copy(inFlightActionKeys = _uiState.value.inFlightActionKeys + actionKey, errorMessage = null)
        viewModelScope.launch(dispatchers.io) {
            val result = action()
            if (result.isFailure) {
                val message = result.exceptionOrNull()?.message ?: "Request failed"
                _uiState.value = _uiState.value.copy(
                    errorMessage = message,
                    statusMessage = null,
                    inFlightActionKeys = _uiState.value.inFlightActionKeys - actionKey
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                hasSession = repository.currentSession() != null,
                sessionUserId = repository.currentSession()?.userId,
                statusMessage = successMsg,
                errorMessage = null,
                inFlightActionKeys = _uiState.value.inFlightActionKeys - actionKey
            )
            refreshDashboardState(keepStatusMessage = true)
        }
    }

    private fun refreshDashboardState(keepStatusMessage: Boolean) {
        val role = repository.currentRole()
        val selectedShiftId = _uiState.value.selectedShiftId

        val shiftsResult = if (role == "business") repository.listBusinessShifts() else repository.listShifts()
        if (shiftsResult.isFailure) {
            showDashboardFailure(shiftsResult.exceptionOrNull(), keepStatusMessage)
            return
        }

        val shifts = shiftsResult.getOrThrow()
        val effectiveShiftId = selectedShiftId?.takeIf { id -> shifts.any { it.id == id } } ?: shifts.firstOrNull()?.id

        val applicationsResult = if (role == "business" && effectiveShiftId != null) {
            repository.listShiftApplications(effectiveShiftId)
        } else if (role == "business") {
            Result.success(emptyList())
        } else {
            repository.listApplications()
        }
        if (applicationsResult.isFailure) {
            showDashboardFailure(applicationsResult.exceptionOrNull(), keepStatusMessage)
            return
        }

        val assignmentsResult = repository.listAssignments()
        if (assignmentsResult.isFailure) {
            showDashboardFailure(assignmentsResult.exceptionOrNull(), keepStatusMessage)
            return
        }

        val applications = applicationsResult.getOrThrow()
        val assignments = assignmentsResult.getOrThrow()
        val payoutsResult = loadPayouts(role)
        if (payoutsResult.isFailure) {
            showDashboardFailure(payoutsResult.exceptionOrNull(), keepStatusMessage)
            return
        }
        val payouts = payoutsResult.getOrThrow()

        _uiState.value = _uiState.value.copy(
            role = role,
            hasSession = repository.currentSession() != null,
            sessionUserId = repository.currentSession()?.userId,
            shifts = shifts,
            applications = applications,
            assignments = assignments,
            payouts = payouts,
            selectedShiftId = effectiveShiftId,
            selectedAssignmentId = assignments.firstOrNull()?.id,
            errorMessage = null,
            statusMessage = if (keepStatusMessage) _uiState.value.statusMessage else null,
            dashboardState = DashboardLoadState.READY,
            importantEvents = deriveImportantEvents(shifts, assignments, payouts)
        )
    }

    private fun loadPayouts(role: String): Result<List<Payout>> {
        if (role != "worker") return Result.success(emptyList())
        return repository.listMyPayouts()
    }

    private fun deriveImportantEvents(shifts: List<Shift>, assignments: List<Assignment>, payouts: List<Payout>): List<String> {
        val shiftById = shifts.associateBy { it.id }
        val events = mutableListOf<String>()

        assignments.filter { it.status == "offered" }.forEach { assignment ->
            val shiftTitle = shiftById[assignment.shiftId]?.title ?: assignment.shiftId
            events += "Offer received: $shiftTitle (${assignment.id})."
        }

        assignments.filter { it.status == "active" }.forEach { assignment ->
            val shiftTitle = shiftById[assignment.shiftId]?.title ?: assignment.shiftId
            events += "Offer accepted: $shiftTitle (${assignment.id})."
        }

        assignments.filter { it.status == "in_progress" }.forEach { assignment ->
            val shiftTitle = shiftById[assignment.shiftId]?.title ?: assignment.shiftId
            events += "Checked in: $shiftTitle (${assignment.id}) is in progress."
        }

        assignments.filter { it.status == "completed_pending_rating" || it.status == "completed_rated" }.forEach { assignment ->
            val shiftTitle = shiftById[assignment.shiftId]?.title ?: assignment.shiftId
            events += "Checked out: $shiftTitle (${assignment.id}) completed."
        }

        payouts.filter { it.status == "created" }.forEach { payout ->
            events += "Payout created for assignment ${payout.assignmentId}."
        }

        payouts.filter { it.status == "released" }.forEach { payout ->
            events += "Payout released for assignment ${payout.assignmentId}."
        }

        val soon = assignments.firstOrNull { assignment ->
            if (assignment.status !in setOf("offered", "active")) return@firstOrNull false
            val shift = shiftById[assignment.shiftId] ?: return@firstOrNull false
            val start = runCatching { Instant.parse(shift.startAt) }.getOrNull() ?: return@firstOrNull false
            val minutes = Duration.between(Instant.now(), start).toMinutes()
            minutes in 0..120
        }
        if (soon != null) {
            val shiftTitle = shiftById[soon.shiftId]?.title ?: soon.shiftId
            events += "Upcoming soon: $shiftTitle (${soon.id}) starts within 2 hours."
        }

        return events.distinct()
    }

    private fun showDashboardFailure(err: Throwable?, keepStatusMessage: Boolean) {
        _uiState.value = _uiState.value.copy(
            errorMessage = err?.message ?: "Dashboard load failed",
            statusMessage = if (keepStatusMessage) _uiState.value.statusMessage else null,
            dashboardState = DashboardLoadState.ERROR
        )
    }

    class Factory(
        private val repository: MarketplaceRepository = ApiMarketplaceRepository(),
        private val dispatchers: AppDispatchers = AppDispatchers()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) return MainViewModel(repository, dispatchers) as T
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
