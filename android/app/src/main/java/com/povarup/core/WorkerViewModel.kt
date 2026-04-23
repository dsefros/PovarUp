package com.povarup.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.MarketplaceRepository
import com.povarup.domain.Shift
import com.povarup.domain.UserRole
import com.povarup.domain.capability
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
    val dateTimeLabel: String,
    val locationLabel: String,
    val payLabel: String,
    val statusLabel: String,
    val canApply: Boolean,
    val actionLabel: String,
    val isApplying: Boolean
)

data class WorkerUiState(
    val isSessionRestoring: Boolean = true,
    val isLoggedIn: Boolean = false,
    val isLoggingIn: Boolean = false,
    val isLoadingShifts: Boolean = false,
    val loginForm: LoginFormState = LoginFormState(),
    val shifts: List<ShiftCardUiModel> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
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
        _uiState.update { it.copy(loginForm = it.loginForm.copy(userId = value), errorMessage = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(loginForm = it.loginForm.copy(password = value), errorMessage = null) }
    }

    fun login() {
        val form = _uiState.value.loginForm
        if (!form.isValid || _uiState.value.isLoggingIn) return
        _uiState.update { it.copy(isLoggingIn = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch(dispatchers.io) {
            val result = repository.login(form.userId.trim(), form.password.trim())
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        isLoggedIn = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                    )
                }
                return@launch
            }

            if (UserRole.from(repository.currentRole()) != UserRole.WORKER) {
                repository.logout()
                _uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        isLoggedIn = false,
                        errorMessage = "This app flow currently supports worker accounts only."
                    )
                }
                return@launch
            }

            loadShifts(showFullScreenLoader = true)
        }
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
        emitShiftModels(errorMessage = null)

        viewModelScope.launch(dispatchers.io) {
            val result = repository.applyToShift(shiftId)
            if (result.isFailure) {
                applyingShiftIds -= shiftId
                emitShiftModels(errorMessage = result.exceptionOrNull()?.message ?: "Could not apply to shift")
                return@launch
            }

            applyingShiftIds -= shiftId
            loadShifts(showFullScreenLoader = false, infoMessage = "Application submitted")
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
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

    private fun loadShifts(showFullScreenLoader: Boolean, infoMessage: String? = null) {
        _uiState.update {
            it.copy(
                isSessionRestoring = false,
                isLoggedIn = true,
                isLoggingIn = false,
                isLoadingShifts = true,
                errorMessage = null,
                infoMessage = infoMessage,
                hasLoadedAtLeastOnce = it.hasLoadedAtLeastOnce || !showFullScreenLoader
            )
        }

        val shiftsResult = repository.listShifts()
        if (shiftsResult.isFailure) {
            _uiState.update {
                it.copy(
                    isLoggingIn = false,
                    isLoadingShifts = false,
                    errorMessage = shiftsResult.exceptionOrNull()?.message ?: "Failed to load shifts"
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
                    errorMessage = applicationsResult.exceptionOrNull()?.message ?: "Failed to load applications"
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
                    errorMessage = assignmentsResult.exceptionOrNull()?.message ?: "Failed to load assignments"
                )
            }
            return
        }

        latestShifts = shiftsResult.getOrThrow()
        latestRelatedShiftIds = (applicationsResult.getOrThrow().map { it.shiftId } + assignmentsResult.getOrThrow().map { it.shiftId }).toSet()

        _uiState.update {
            it.copy(
                isLoggingIn = false,
                isLoadingShifts = false,
                isLoggedIn = true,
                shifts = latestShifts.toUiModels(latestRelatedShiftIds, applyingShiftIds),
                hasLoadedAtLeastOnce = true
            )
        }
    }

    private fun emitShiftModels(errorMessage: String?) {
        _uiState.update {
            it.copy(
                shifts = latestShifts.toUiModels(latestRelatedShiftIds, applyingShiftIds),
                errorMessage = errorMessage
            )
        }
    }

    private fun List<Shift>.toUiModels(relatedShiftIds: Set<String>, applyingShiftIds: Set<String>): List<ShiftCardUiModel> = map { shift ->
        val capability = shift.capability(UserRole.WORKER, relatedShiftIds.contains(shift.id))
        val isApplying = applyingShiftIds.contains(shift.id)
        ShiftCardUiModel(
            id = shift.id,
            title = shift.title,
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
