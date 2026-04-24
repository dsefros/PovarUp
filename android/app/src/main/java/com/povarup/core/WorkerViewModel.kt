package com.povarup.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.DemoMarketplaceRepository
import com.povarup.data.MarketplaceRepository
import com.povarup.data.WorkerDataSourceMode
import com.povarup.data.WorkerModeSelectable
import com.povarup.domain.Assignment
import com.povarup.domain.AssignmentStatus
import com.povarup.domain.Shift
import com.povarup.domain.UserRole
import com.povarup.domain.capability
import com.povarup.domain.workTypeDescription
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkerDispatchers(val io: CoroutineDispatcher = Dispatchers.IO)

data class LoginFormState(
    val userId: String = "",
    val password: String = ""
) {
    val isValid: Boolean = userId.isNotBlank() && password.isNotBlank()
}

data class ShiftCardUiModel(
    val id: String,
    val title: String,
    val workTypeDetails: String?,
    val dateTimeLabel: String,
    val locationLabel: String,
    val payLabel: String,
    val statusLabel: String,
    val canApply: Boolean,
    val actionLabel: String,
    val isApplying: Boolean
)

data class ActiveAssignmentUiModel(
    val assignmentId: String,
    val title: String,
    val dateTimeLabel: String,
    val locationLabel: String,
    val statusLabel: String
)

data class WorkerUiState(
    val isSessionRestoring: Boolean = true,
    val isLoggedIn: Boolean = false,
    val isLoggingIn: Boolean = false,
    val isLoadingShifts: Boolean = false,
    val loginForm: LoginFormState = LoginFormState(),
    val shifts: List<ShiftCardUiModel> = emptyList(),
    val applicationsCount: Int? = null,
    val assignmentsCount: Int? = null,
    val payoutsCount: Int? = null,
    val activeAssignment: ActiveAssignmentUiModel? = null,
    val message: UiMessage? = null,
    val hasLoadedAtLeastOnce: Boolean = false
)

class WorkerViewModel(
    private val repository: MarketplaceRepository = ApiMarketplaceRepository(),
    private val dispatchers: WorkerDispatchers = WorkerDispatchers()
) : ViewModel() {
    private val applyingShiftIds = mutableSetOf<String>()
    private var latestShifts: List<Shift> = emptyList()
    private var latestRelatedShiftIds: Set<String> = emptySet()

    private val _uiState = MutableStateFlow(WorkerUiState())
    val uiState: StateFlow<WorkerUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    fun onUserIdChanged(value: String) {
        _uiState.update { it.copy(loginForm = it.loginForm.copy(userId = value), message = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(loginForm = it.loginForm.copy(password = value), message = null) }
    }

    fun continueAsDemoWorker() {
        if (_uiState.value.isLoggingIn) return
        (repository as? WorkerModeSelectable)?.selectMode(WorkerDataSourceMode.DEMO)
        _uiState.update { it.copy(isLoggingIn = true, message = null) }

        viewModelScope.launch(dispatchers.io) {
            performLogin(DemoMarketplaceRepository.DEMO_USER_ID, DemoMarketplaceRepository.DEMO_PASSWORD)
        }
    }

    fun login() {
        val form = _uiState.value.loginForm
        if (!form.isValid || _uiState.value.isLoggingIn) return
        (repository as? WorkerModeSelectable)?.selectMode(WorkerDataSourceMode.REAL)
        _uiState.update { it.copy(isLoggingIn = true, message = null) }

        viewModelScope.launch(dispatchers.io) {
            performLogin(form.userId.trim(), form.password.trim())
        }
    }

    private fun performLogin(userId: String, password: String) {
        val result = repository.login(userId, password)
        if (result.isFailure) {
            _uiState.update {
                it.copy(
                    isLoggingIn = false,
                    isLoggedIn = false,
                    message = UiMessage(result.exceptionOrNull()?.message ?: "Login failed", UiMessageKind.ERROR)
                )
            }
            return
        }

        if (UserRole.from(repository.currentRole()) != UserRole.WORKER) {
            repository.logout()
            _uiState.update {
                it.copy(
                    isLoggingIn = false,
                    isLoggedIn = false,
                    message = UiMessage("This app flow currently supports worker accounts only.", UiMessageKind.ERROR)
                )
            }
            return
        }

        loadShifts(showFullScreenLoader = true)
    }

    fun refresh() {
        if (!_uiState.value.isLoggedIn || _uiState.value.isLoadingShifts) return
        viewModelScope.launch(dispatchers.io) {
            loadShifts(showFullScreenLoader = !_uiState.value.hasLoadedAtLeastOnce)
        }
    }

    fun applyToShift(shiftId: String) {
        if (!_uiState.value.isLoggedIn || applyingShiftIds.contains(shiftId)) return
        applyingShiftIds += shiftId
        emitShiftModels()

        viewModelScope.launch(dispatchers.io) {
            val result = repository.applyToShift(shiftId)
            if (result.isFailure) {
                applyingShiftIds -= shiftId
                emitShiftModels(
                    message = UiMessage(
                        result.exceptionOrNull()?.message ?: "Could not apply to shift",
                        UiMessageKind.ERROR
                    )
                )
                return@launch
            }

            applyingShiftIds -= shiftId
            loadShifts(showFullScreenLoader = false, message = UiMessage("Application submitted", UiMessageKind.INFO))
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun logout() {
        repository.clearSession()
        latestShifts = emptyList()
        latestRelatedShiftIds = emptySet()
        applyingShiftIds.clear()
        _uiState.value = WorkerUiState(isSessionRestoring = false)
    }

    private fun restoreSession() {
        viewModelScope.launch(dispatchers.io) {
            val hasSession = repository.currentSession() != null
            val isWorkerSession = UserRole.from(repository.currentRole()) == UserRole.WORKER
            if (hasSession && isWorkerSession) {
                loadShifts(showFullScreenLoader = true)
            } else {
                _uiState.update { it.copy(isSessionRestoring = false, isLoggedIn = false) }
            }
        }
    }

    private fun loadShifts(showFullScreenLoader: Boolean, message: UiMessage? = null) {
        _uiState.update {
            it.copy(
                isSessionRestoring = false,
                isLoggedIn = true,
                isLoggingIn = false,
                isLoadingShifts = true,
                message = message,
                hasLoadedAtLeastOnce = it.hasLoadedAtLeastOnce || !showFullScreenLoader
            )
        }

        val shiftsResult = repository.listShifts()
        if (shiftsResult.isFailure) {
            _uiState.update {
                it.copy(
                    isLoggingIn = false,
                    isLoadingShifts = false,
                    message = UiMessage(shiftsResult.exceptionOrNull()?.message ?: "Failed to load shifts", UiMessageKind.ERROR)
                )
            }
            return
        }

        val applicationsResult = repository.listApplications()
        if (applicationsResult.isFailure) {
            _uiState.update {
                it.copy(
                    isLoggingIn = false,
                    isLoadingShifts = false,
                    message = UiMessage(applicationsResult.exceptionOrNull()?.message ?: "Failed to load applications", UiMessageKind.ERROR)
                )
            }
            return
        }

        val assignmentsResult = repository.listAssignments()
        if (assignmentsResult.isFailure) {
            _uiState.update {
                it.copy(
                    isLoggingIn = false,
                    isLoadingShifts = false,
                    message = UiMessage(assignmentsResult.exceptionOrNull()?.message ?: "Failed to load assignments", UiMessageKind.ERROR)
                )
            }
            return
        }

        latestShifts = shiftsResult.getOrThrow()
        val applications = applicationsResult.getOrThrow()
        val assignments = assignmentsResult.getOrThrow()
        latestRelatedShiftIds = (applications.map { it.shiftId } + assignments.map { it.shiftId }).toSet()

        _uiState.update {
            it.copy(
                isLoggingIn = false,
                isLoadingShifts = false,
                isLoggedIn = true,
                shifts = latestShifts.toUiModels(latestRelatedShiftIds, applyingShiftIds),
                applicationsCount = applications.size,
                assignmentsCount = assignments.size,
                activeAssignment = assignments.toActiveAssignmentUiModel(latestShifts),
                hasLoadedAtLeastOnce = true
            )
        }
    }

    private fun emitShiftModels(message: UiMessage? = null) {
        _uiState.update {
            it.copy(
                shifts = latestShifts.toUiModels(latestRelatedShiftIds, applyingShiftIds),
                message = message
            )
        }
    }

    private fun List<Shift>.toUiModels(relatedShiftIds: Set<String>, applyingShiftIds: Set<String>): List<ShiftCardUiModel> = map { shift ->
        val capability = shift.capability(UserRole.WORKER, relatedShiftIds.contains(shift.id))
        val isApplying = applyingShiftIds.contains(shift.id)
        ShiftCardUiModel(
            id = shift.id,
            title = shift.title,
            workTypeDetails = shift.workTypeDescription(),
            dateTimeLabel = "${shift.startAt} → ${shift.endAt}",
            locationLabel = "Location: ${shift.locationId}",
            payLabel = "$${"%.2f".format(shift.payRateCents / 100.0)}/hr",
            statusLabel = shift.rawStatus.replaceFirstChar { it.uppercase() },
            canApply = capability.canApply && !isApplying,
            actionLabel = when {
                isApplying -> "Applying..."
                capability.canApply -> "Apply"
                else -> "Applied / Unavailable"
            },
            isApplying = isApplying
        )
    }

    private fun List<Assignment>.toActiveAssignmentUiModel(shifts: List<Shift>): ActiveAssignmentUiModel? {
        val activeAssignment = firstOrNull { it.status == AssignmentStatus.ASSIGNED || it.status == AssignmentStatus.IN_PROGRESS } ?: return null
        val shift = shifts.firstOrNull { it.id == activeAssignment.shiftId }
        return ActiveAssignmentUiModel(
            assignmentId = activeAssignment.id,
            title = shift?.title ?: "Current assignment",
            dateTimeLabel = shift?.let { "${it.startAt} → ${it.endAt}" } ?: "Shift: ${activeAssignment.shiftId}",
            locationLabel = shift?.let { "Location: ${it.locationId}" } ?: "Location unavailable",
            statusLabel = activeAssignment.rawStatus.replaceFirstChar { it.uppercase() }
        )
    }

    class Factory(
        private val repository: MarketplaceRepository = ApiMarketplaceRepository(),
        private val dispatchers: WorkerDispatchers = WorkerDispatchers()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WorkerViewModel::class.java)) {
                return WorkerViewModel(repository, dispatchers) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
