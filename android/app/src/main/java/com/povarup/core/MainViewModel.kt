package com.povarup.core

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.CreateShiftRequest
import com.povarup.data.MarketplaceRepository
import com.povarup.data.ProblemCasesDto
import com.povarup.domain.UserRole
import com.povarup.domain.capability
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppDispatchers(val io: CoroutineDispatcher = Dispatchers.IO)

enum class UiLoadState { IDLE, LOADING, CONTENT, ERROR }

data class MainCapabilities(
    val canRefresh: Boolean = true,
    val canLogin: Boolean = true,
    val canApplySelectedShift: Boolean = false,
    val canCheckInSelectedAssignment: Boolean = false,
    val canCheckOutSelectedAssignment: Boolean = false,
    val canCreateShift: Boolean = false,
    val canPublishSelectedShift: Boolean = false,
    val canOfferSelectedApplication: Boolean = false,
    val canRejectSelectedApplication: Boolean = false,
    val canWithdrawSelectedApplication: Boolean = false,
    val canCloseSelectedShift: Boolean = false,
    val canCancelSelectedShift: Boolean = false,
    val canCancelSelectedAssignment: Boolean = false,
    val canReleaseSelectedAssignmentPayout: Boolean = false,
    val canProgressAdminPayout: Boolean = false
)

data class WorkerHomeState(
    val shifts: List<com.povarup.domain.Shift> = emptyList(),
    val applications: List<com.povarup.domain.Application> = emptyList(),
    val assignments: List<com.povarup.domain.Assignment> = emptyList(),
    val payouts: List<com.povarup.domain.Payout> = emptyList(),
    val availableShifts: List<com.povarup.domain.Shift> = emptyList(),
    val activeAssignment: com.povarup.domain.Assignment? = null,
    val completedAssignments: List<com.povarup.domain.Assignment> = emptyList()
)

data class BusinessHomeState(
    val ownedShifts: List<com.povarup.domain.Shift> = emptyList(),
    val applicationsForSelectedShift: List<com.povarup.domain.Application> = emptyList(),
    val assignments: List<com.povarup.domain.Assignment> = emptyList(),
    val payoutReadyAssignments: List<com.povarup.domain.Assignment> = emptyList()
)

data class AdminHomeState(
    val assignments: List<com.povarup.domain.Assignment> = emptyList(),
    val payouts: List<com.povarup.domain.Payout> = emptyList(),
    val problemCases: ProblemCasesDto = ProblemCasesDto()
)

sealed class HomeState {
    data class Worker(val content: WorkerHomeState) : HomeState()
    data class Business(val content: BusinessHomeState) : HomeState()
    data class Admin(val content: AdminHomeState) : HomeState()
    data object None : HomeState()
}

data class MainUiState(
    val role: UserRole = UserRole.WORKER,
    val baseUrl: String = "",
    val hasSession: Boolean = false,
    val sessionUserId: String? = null,
    val loadState: UiLoadState = UiLoadState.IDLE,
    val home: HomeState = HomeState.None,
    val selectedShiftId: String? = null,
    val selectedAssignmentId: String? = null,
    val selectedApplicationId: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val inFlightActionKeys: Set<String> = emptySet(),
    val capabilities: MainCapabilities = MainCapabilities(),
    val importantEvents: List<String> = emptyList()
)

sealed class MainAction {
    data class Login(val userId: String, val password: String) : MainAction()
    data object Logout : MainAction()
    data object Refresh : MainAction()
    data class UpdateSelections(val shiftId: String? = null, val assignmentId: String? = null, val applicationId: String? = null) : MainAction()
    data class ApplyToShift(val shiftId: String) : MainAction()
    data class CheckIn(val assignmentId: String) : MainAction()
    data class CheckOut(val assignmentId: String) : MainAction()
    data class PublishShift(val shiftId: String) : MainAction()
    data class CreateShift(val title: String, val locationId: String, val payRateCents: Int) : MainAction()
    data class OfferAssignment(val applicationId: String) : MainAction()
    data class RejectApplication(val applicationId: String) : MainAction()
    data class WithdrawApplication(val applicationId: String) : MainAction()
    data class CloseShift(val shiftId: String) : MainAction()
    data class CancelShift(val shiftId: String) : MainAction()
    data class CancelAssignment(val assignmentId: String) : MainAction()
    data class ReleasePayout(val assignmentId: String) : MainAction()
    data class ProgressAdminPayout(val payoutId: String, val status: String) : MainAction()
    data object ClearSession : MainAction()
    data object DismissError : MainAction()
}

class MainViewModel(
    private val repository: MarketplaceRepository = ApiMarketplaceRepository(),
    private val dispatchers: AppDispatchers = AppDispatchers()
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        MainUiState(
            role = UserRole.from(repository.currentRole()),
            baseUrl = repository.baseUrl(),
            hasSession = repository.currentSession() != null,
            sessionUserId = repository.currentSession()?.userId
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init { applyDerivedState(_uiState.value) }

    fun onAction(action: MainAction) {
        when (action) {
            is MainAction.Login -> runAsync("login", "Logged in") { repository.login(action.userId.trim(), action.password.trim()) }
            MainAction.Logout -> runAsync("logout", "Logged out") { repository.logout() }
            MainAction.Refresh -> loadDashboard()
            is MainAction.UpdateSelections -> applyDerivedState(_uiState.value.withSelections(action.shiftId, action.assignmentId, action.applicationId))
            is MainAction.ApplyToShift -> applyToShift(action.shiftId)
            is MainAction.CheckIn -> checkIn(action.assignmentId)
            is MainAction.CheckOut -> checkOut(action.assignmentId)
            is MainAction.PublishShift -> requireRole(UserRole.BUSINESS, "Only businesses can publish shifts.") { runAsync("publish_shift:${action.shiftId.trim()}", "Shift published") { repository.publishShift(action.shiftId.trim()) } }
            is MainAction.CreateShift -> requireRole(UserRole.BUSINESS, "Only businesses can create shifts.") { runAsync("create_shift", "Shift created") { repository.createShift(CreateShiftRequest(action.locationId.trim(), action.title.trim(), Instant.now().toString(), Instant.now().plusSeconds(7200).toString(), action.payRateCents)) } }
            is MainAction.OfferAssignment -> requireRole(UserRole.BUSINESS, "Only businesses can offer assignments.") { runAsync("offer:${action.applicationId.trim()}", "Offer sent") { repository.offerAssignment(action.applicationId.trim()) } }
            is MainAction.RejectApplication -> requireRole(UserRole.BUSINESS, "Only businesses can reject applications.") { runAsync("reject:${action.applicationId.trim()}", "Application rejected") { repository.rejectApplication(action.applicationId.trim()) } }
            is MainAction.WithdrawApplication -> requireRole(UserRole.WORKER, "Only workers can withdraw applications.") { runAsync("withdraw:${action.applicationId.trim()}", "Application withdrawn") { repository.withdrawApplication(action.applicationId.trim()) } }
            is MainAction.CloseShift -> requireRole(UserRole.BUSINESS, "Only businesses can close shifts.") { runAsync("close_shift:${action.shiftId.trim()}", "Shift closed") { repository.closeShift(action.shiftId.trim()) } }
            is MainAction.CancelShift -> requireRole(UserRole.BUSINESS, "Only businesses can cancel shifts.") { runAsync("cancel_shift:${action.shiftId.trim()}", "Shift cancelled") { repository.cancelShift(action.shiftId.trim()) } }
            is MainAction.CancelAssignment -> requireRole(UserRole.BUSINESS, "Only businesses can cancel assignments.") { runAsync("cancel_assignment:${action.assignmentId.trim()}", "Assignment cancelled") { repository.cancelAssignment(action.assignmentId.trim()) } }
            is MainAction.ReleasePayout -> requireRole(UserRole.BUSINESS, "Only businesses can release payouts.") { runAsync("release:${action.assignmentId.trim()}", "Payout released") { repository.releasePayout(action.assignmentId.trim()) } }
            is MainAction.ProgressAdminPayout -> requireRole(UserRole.ADMIN, "Only admins can progress payout status.") { runAsync("admin_payout:${action.payoutId}:${action.status}", "Payout moved to ${action.status}") { repository.updateAdminPayoutStatus(action.payoutId.trim(), action.status) } }
            MainAction.ClearSession -> clearSession()
            MainAction.DismissError -> applyDerivedState(_uiState.value.copy(errorMessage = null))
        }
    }

    private fun loadDashboard() {
        if (_uiState.value.loadState == UiLoadState.LOADING) return
        applyDerivedState(_uiState.value.copy(loadState = UiLoadState.LOADING, errorMessage = null))
        viewModelScope.launch(dispatchers.io) { refreshDashboardState(keepStatusMessage = true) }
    }

    private fun applyToShift(shiftId: String) {
        requireRole(UserRole.WORKER, "Only workers can apply to shifts.") {
            val normalized = shiftId.trim()
            val state = _uiState.value.withSelections(shiftId = normalized)
            val target = state.home.allShifts().firstOrNull { it.id == normalized }
            val relatedShiftIds = (state.home.allApplications().map { it.shiftId } + state.home.allAssignments().map { it.shiftId }).toSet()
            val canApply = target?.capability(UserRole.WORKER, relatedShiftIds.contains(normalized))?.canApply ?: false
            if (!canApply) return@requireRole applyDerivedState(state.copy(errorMessage = "Shift is not available for apply.", statusMessage = null))
            applyDerivedState(state)
            runAsync("apply:$normalized", "Applied") { repository.applyToShift(normalized) }
        }
    }

    private fun checkIn(assignmentId: String) = workerAssignmentAction(
        assignmentId = assignmentId,
        actionKeyPrefix = "checkin",
        notAllowedMessage = "Check-in is only available for assigned shifts.",
        successMessage = "Check-in completed",
        predicate = { it.capability(UserRole.WORKER).canCheckIn },
        request = { repository.checkIn(it) }
    )

    private fun checkOut(assignmentId: String) = workerAssignmentAction(
        assignmentId = assignmentId,
        actionKeyPrefix = "checkout",
        notAllowedMessage = "Check-out is only available for in-progress shifts.",
        successMessage = "Check-out completed",
        predicate = { it.capability(UserRole.WORKER).canCheckOut },
        request = { repository.checkOut(it) }
    )

    private fun workerAssignmentAction(
        assignmentId: String,
        actionKeyPrefix: String,
        notAllowedMessage: String,
        successMessage: String,
        predicate: (com.povarup.domain.Assignment) -> Boolean,
        request: (String) -> Result<Unit>
    ) {
        requireRole(UserRole.WORKER, "Only workers can ${if (actionKeyPrefix == "checkin") "check in" else "check out"}.") {
            val normalized = assignmentId.trim()
            val state = _uiState.value.withSelections(assignmentId = normalized)
            val assignment = state.home.allAssignments().firstOrNull { it.id == normalized }
            if (assignment != null && !predicate(assignment)) return@requireRole applyDerivedState(state.copy(errorMessage = notAllowedMessage, statusMessage = null))
            applyDerivedState(state)
            runAsync("$actionKeyPrefix:$normalized", successMessage) { request(normalized) }
        }
    }

    private fun clearSession() {
        repository.clearSession()
        applyDerivedState(
            _uiState.value.copy(
                role = UserRole.from(repository.currentRole()),
                hasSession = false,
                sessionUserId = null,
                statusMessage = "Session cleared",
                errorMessage = null,
                loadState = UiLoadState.IDLE,
                home = HomeState.None,
                selectedShiftId = null,
                selectedAssignmentId = null,
                selectedApplicationId = null,
                importantEvents = emptyList()
            )
        )
    }

    private fun requireRole(expectedRole: UserRole, message: String, block: () -> Unit) {
        if (_uiState.value.role == expectedRole) return block()
        applyDerivedState(_uiState.value.copy(errorMessage = message, statusMessage = null))
    }

    private fun <T> runAsync(actionKey: String, successMsg: String, action: () -> Result<T>) {
        if (_uiState.value.inFlightActionKeys.contains(actionKey)) return
        applyDerivedState(_uiState.value.copy(inFlightActionKeys = _uiState.value.inFlightActionKeys + actionKey, errorMessage = null))
        viewModelScope.launch(dispatchers.io) {
            runCatching { Log.i("PovarUp", "action_start key=$actionKey") }
            val result = action()
            if (result.isFailure) {
                val message = result.exceptionOrNull()?.message ?: "Request failed"
                applyDerivedState(baseSessionState().copy(errorMessage = message, statusMessage = null, inFlightActionKeys = _uiState.value.inFlightActionKeys - actionKey))
                return@launch
            }
            applyDerivedState(baseSessionState().copy(statusMessage = successMsg, errorMessage = null, inFlightActionKeys = _uiState.value.inFlightActionKeys - actionKey))
            refreshDashboardState(keepStatusMessage = true)
        }
    }

    private fun refreshDashboardState(keepStatusMessage: Boolean) {
        val role = UserRole.from(repository.currentRole())
        val shiftsResult = if (role == UserRole.BUSINESS) repository.listBusinessShifts() else repository.listShifts()
        if (shiftsResult.isFailure) return showDashboardFailure(shiftsResult.exceptionOrNull(), keepStatusMessage)
        val shifts = shiftsResult.getOrThrow()

        val effectiveShiftId = _uiState.value.selectedShiftId?.takeIf { id -> shifts.any { it.id == id } } ?: shifts.firstOrNull()?.id
        val applicationsResult = if (role == UserRole.BUSINESS && effectiveShiftId != null) repository.listShiftApplications(effectiveShiftId) else if (role == UserRole.BUSINESS) Result.success(emptyList()) else repository.listApplications()
        if (applicationsResult.isFailure) return showDashboardFailure(applicationsResult.exceptionOrNull(), keepStatusMessage)

        val assignmentsResult = repository.listAssignments()
        if (assignmentsResult.isFailure) return showDashboardFailure(assignmentsResult.exceptionOrNull(), keepStatusMessage)

        val applications = applicationsResult.getOrThrow()
        val assignments = assignmentsResult.getOrThrow()
        val payoutsResult = if (role == UserRole.WORKER) repository.listMyPayouts() else Result.success(emptyList())
        if (payoutsResult.isFailure) return showDashboardFailure(payoutsResult.exceptionOrNull(), keepStatusMessage)
        val payouts = payoutsResult.getOrThrow()

        val adminAssignments = if (role == UserRole.ADMIN) repository.listAdminAssignments().getOrElse { return showDashboardFailure(it, keepStatusMessage) } else emptyList()
        val adminPayouts = if (role == UserRole.ADMIN) repository.listAdminPayouts().getOrElse { return showDashboardFailure(it, keepStatusMessage) } else emptyList()
        val adminProblemCases = if (role == UserRole.ADMIN) repository.getAdminProblemCases().getOrElse { return showDashboardFailure(it, keepStatusMessage) } else ProblemCasesDto()

        val snapshot = DashboardSnapshot(role, shifts, applications, assignments, payouts, adminAssignments, adminPayouts, adminProblemCases)
        val home = buildHomeState(snapshot)
        applyDerivedState(
            baseSessionState().copy(
                home = home,
                selectedShiftId = effectiveShiftId,
                selectedAssignmentId = _uiState.value.selectedAssignmentId?.takeIf { id -> assignments.any { it.id == id } } ?: assignments.firstOrNull()?.id,
                selectedApplicationId = _uiState.value.selectedApplicationId?.takeIf { id -> applications.any { it.id == id } } ?: applications.firstOrNull()?.id,
                errorMessage = null,
                statusMessage = if (keepStatusMessage) _uiState.value.statusMessage else null,
                loadState = UiLoadState.CONTENT,
                importantEvents = deriveImportantEvents(shifts, assignments, payouts)
            )
        )
    }

    private fun showDashboardFailure(err: Throwable?, keepStatusMessage: Boolean) {
        applyDerivedState(baseSessionState().copy(errorMessage = err?.message ?: "Dashboard load failed", statusMessage = if (keepStatusMessage) _uiState.value.statusMessage else null, loadState = UiLoadState.ERROR))
    }

    private fun baseSessionState(): MainUiState = _uiState.value.copy(
        role = UserRole.from(repository.currentRole()),
        baseUrl = repository.baseUrl(),
        hasSession = repository.currentSession() != null,
        sessionUserId = repository.currentSession()?.userId
    )

    private fun applyDerivedState(candidate: MainUiState) {
        _uiState.value = candidate.copy(capabilities = computeCapabilities(candidate))
    }

    private fun MainUiState.withSelections(shiftId: String? = null, assignmentId: String? = null, applicationId: String? = null): MainUiState = copy(
        selectedShiftId = shiftId.normalizeSelection(selectedShiftId),
        selectedAssignmentId = assignmentId.normalizeSelection(selectedAssignmentId),
        selectedApplicationId = applicationId.normalizeSelection(selectedApplicationId)
    )

    private fun String?.normalizeSelection(current: String?): String? =
        if (this == null) current else trim().ifBlank { null }

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
