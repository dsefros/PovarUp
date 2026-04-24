package com.povarup.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.DemoMarketplaceRepository
import com.povarup.data.MarketplaceRepository
import com.povarup.data.WorkerDataSourceMode
import com.povarup.data.WorkerModeSelectable
import com.povarup.domain.Application
import com.povarup.domain.ApplicationStatus
import com.povarup.domain.Assignment
import com.povarup.domain.AssignmentStatus
import com.povarup.domain.Payout
import com.povarup.domain.PayoutStatus
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

data class WorkerAssignmentUiModel(
    val assignmentId: String,
    val shiftId: String,
    val title: String,
    val dateTimeLabel: String,
    val locationLabel: String,
    val status: AssignmentStatus,
    val statusLabel: String,
    val lifecycleText: String,
    val canCheckIn: Boolean,
    val canCheckOut: Boolean,
    val isPaid: Boolean
)

data class WorkerPayoutUiModel(
    val id: String,
    val shortId: String,
    val assignmentId: String,
    val amountLabel: String,
    val statusLabel: String,
    val note: String?
)

data class WorkerApplicationUiModel(
    val applicationId: String,
    val shortId: String,
    val shiftId: String,
    val shiftTitle: String,
    val status: ApplicationStatus,
    val statusLabel: String,
    val canWithdraw: Boolean
)

data class WorkerUiState(
    val isSessionRestoring: Boolean = true,
    val isLoggedIn: Boolean = false,
    val isLoggingIn: Boolean = false,
    val isLoadingShifts: Boolean = false,
    val loginForm: LoginFormState = LoginFormState(),
    val shifts: List<ShiftCardUiModel> = emptyList(),
    val applications: List<WorkerApplicationUiModel> = emptyList(),
    val applicationsCount: Int? = null,
    val assignmentsCount: Int? = null,
    val payoutsCount: Int? = null,
    val payouts: List<WorkerPayoutUiModel> = emptyList(),
    val activeAssignment: ActiveAssignmentUiModel? = null,
    val assignments: List<WorkerAssignmentUiModel> = emptyList(),
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

    fun checkIn(assignmentId: String) {
        if (!_uiState.value.isLoggedIn) return
        viewModelScope.launch(dispatchers.io) {
            val result = repository.checkIn(assignmentId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        message = UiMessage(
                            result.exceptionOrNull()?.message ?: "Не удалось начать смену",
                            UiMessageKind.ERROR
                        )
                    )
                }
                return@launch
            }
            loadShifts(showFullScreenLoader = false, message = UiMessage("Смена начата", UiMessageKind.INFO))
        }
    }

    fun checkOut(assignmentId: String) {
        if (!_uiState.value.isLoggedIn) return
        viewModelScope.launch(dispatchers.io) {
            val result = repository.checkOut(assignmentId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        message = UiMessage(
                            result.exceptionOrNull()?.message ?: "Не удалось завершить смену",
                            UiMessageKind.ERROR
                        )
                    )
                }
                return@launch
            }
            loadShifts(showFullScreenLoader = false, message = UiMessage("Смена завершена", UiMessageKind.INFO))
        }
    }

    fun withdrawApplication(applicationId: String) {
        if (!_uiState.value.isLoggedIn) return
        viewModelScope.launch(dispatchers.io) {
            val result = repository.withdrawApplication(applicationId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        message = UiMessage(
                            result.exceptionOrNull()?.message ?: "Не удалось отозвать отклик",
                            UiMessageKind.ERROR
                        )
                    )
                }
                return@launch
            }
            loadShifts(showFullScreenLoader = false, message = UiMessage("Отклик отозван", UiMessageKind.INFO))
        }
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

        val payoutsResult = repository.listMyPayouts()
        if (payoutsResult.isFailure) {
            _uiState.update {
                it.copy(
                    isLoggingIn = false,
                    isLoadingShifts = false,
                    message = UiMessage(payoutsResult.exceptionOrNull()?.message ?: "Failed to load payouts", UiMessageKind.ERROR)
                )
            }
            return
        }

        latestShifts = shiftsResult.getOrThrow()
        val applications = applicationsResult.getOrThrow()
        val assignments = assignmentsResult.getOrThrow()
        val payouts = payoutsResult.getOrThrow()
        latestRelatedShiftIds = (applications.map { it.shiftId } + assignments.map { it.shiftId }).toSet()

        _uiState.update {
            it.copy(
                isLoggingIn = false,
                isLoadingShifts = false,
                isLoggedIn = true,
                shifts = latestShifts.toUiModels(latestRelatedShiftIds, applyingShiftIds),
                applications = applications.toWorkerApplicationsUiModels(latestShifts),
                applicationsCount = applications.size,
                assignmentsCount = assignments.size,
                payoutsCount = payouts.size,
                payouts = payouts.toUiModels(),
                activeAssignment = assignments.toActiveAssignmentUiModel(latestShifts),
                assignments = assignments.toWorkerAssignmentsUiModels(latestShifts),
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

    private fun List<Assignment>.toWorkerAssignmentsUiModels(shifts: List<Shift>): List<WorkerAssignmentUiModel> = map { assignment ->
        val shift = shifts.firstOrNull { it.id == assignment.shiftId }
        val capability = assignment.capability(UserRole.WORKER)
        WorkerAssignmentUiModel(
            assignmentId = assignment.id,
            shiftId = assignment.shiftId,
            title = shift?.title ?: "Смена ${assignment.shiftId}",
            dateTimeLabel = shift?.let { "${it.startAt} → ${it.endAt}" } ?: "Время не указано",
            locationLabel = shift?.let { "Локация: ${it.locationId}" } ?: "Локация недоступна",
            status = assignment.status,
            statusLabel = assignment.rawStatus.replaceFirstChar { it.uppercase() },
            lifecycleText = assignment.toLifecycleText(),
            canCheckIn = capability.canCheckIn,
            canCheckOut = capability.canCheckOut,
            isPaid = assignment.status == AssignmentStatus.PAID
        )
    }

    private fun List<Application>.toWorkerApplicationsUiModels(shifts: List<Shift>): List<WorkerApplicationUiModel> = map { application ->
        val shift = shifts.firstOrNull { it.id == application.shiftId }
        val capability = application.capability(UserRole.WORKER)
        WorkerApplicationUiModel(
            applicationId = application.id,
            shortId = application.id.takeLast(8),
            shiftId = application.shiftId,
            shiftTitle = shift?.title ?: "Смена ${application.shiftId}",
            status = application.status,
            statusLabel = application.status.toWorkerLabel(),
            canWithdraw = capability.canWithdraw
        )
    }

    private fun Assignment.toLifecycleText(): String = when (status) {
        AssignmentStatus.ASSIGNED -> "Назначена"
        AssignmentStatus.IN_PROGRESS -> "В процессе"
        AssignmentStatus.COMPLETED -> "Завершена"
        AssignmentStatus.CANCELLED -> "Отменена"
        AssignmentStatus.PAID -> "Оплачена"
        AssignmentStatus.UNKNOWN -> "Статус уточняется"
    }

    private fun List<Payout>.toUiModels(): List<WorkerPayoutUiModel> = map { payout ->
        WorkerPayoutUiModel(
            id = payout.id,
            shortId = payout.id.takeLast(8),
            assignmentId = payout.assignmentId,
            amountLabel = "$${"%.2f".format(payout.amountCents / 100.0)}",
            statusLabel = payout.status.toWorkerLabel(),
            note = payout.note?.takeIf { it.isNotBlank() }
        )
    }

    private fun PayoutStatus.toWorkerLabel(): String = when (this) {
        PayoutStatus.CREATED -> "Создана"
        PayoutStatus.PENDING -> "В обработке"
        PayoutStatus.PAID -> "Выплачено"
        PayoutStatus.FAILED -> "Ошибка выплаты"
        PayoutStatus.UNKNOWN -> "Неизвестный статус"
    }

    private fun ApplicationStatus.toWorkerLabel(): String = when (this) {
        ApplicationStatus.APPLIED -> "Отклик подан"
        ApplicationStatus.ACCEPTED -> "Принят / сделан оффер"
        ApplicationStatus.REJECTED -> "Отклонен"
        ApplicationStatus.WITHDRAWN -> "Отозван"
        ApplicationStatus.UNKNOWN -> "Статус уточняется"
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
