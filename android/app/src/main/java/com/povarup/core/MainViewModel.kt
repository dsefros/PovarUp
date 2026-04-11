package com.povarup.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.CreateShiftRequest
import com.povarup.data.MarketplaceRepository
import com.povarup.domain.Application
import com.povarup.domain.Assignment
import com.povarup.domain.Shift
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppDispatchers(val io: CoroutineDispatcher = Dispatchers.IO)

data class MainUiState(
    val role: String,
    val baseUrl: String,
    val hasSession: Boolean = false,
    val sessionUserId: String? = null,
    val shifts: List<Shift> = emptyList(),
    val applications: List<Application> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val selectedShiftId: String? = null,
    val selectedAssignmentId: String? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
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
        runAsync("Logged in") { repository.login(userId.trim(), password.trim()) }
    }

    fun setSelectedShift(shiftId: String) {
        _uiState.value = _uiState.value.copy(selectedShiftId = shiftId.trim().ifBlank { null })
    }

    fun loadDashboard() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch(dispatchers.io) {
            refreshDashboardState(keepStatusMessage = true)
        }
    }

    fun applyToShift(shiftId: String) = runAsync("Applied") { repository.applyToShift(shiftId.trim()) }
    fun acceptAssignment(assignmentId: String) = runAsync("Offer accepted") { repository.acceptAssignment(assignmentId.trim()) }
    fun checkIn(assignmentId: String) = runAsync("Checked in") { repository.checkIn(assignmentId.trim()) }
    fun checkOut(assignmentId: String) = runAsync("Checked out") { repository.checkOut(assignmentId.trim()) }
    fun offerAssignment(applicationId: String) = runAsync("Offer sent") { repository.offerAssignment(applicationId.trim()) }
    fun releasePayout(assignmentId: String) = runAsync("Payout released") { repository.releasePayout(assignmentId.trim()) }

    fun createShift(title: String, locationId: String, payRateCents: Int) = runAsync("Shift created") {
        repository.createShift(
            CreateShiftRequest(
                locationId = locationId.trim(),
                title = title.trim(),
                startAt = java.time.Instant.now().toString(),
                endAt = java.time.Instant.now().plusSeconds(7200).toString(),
                payRateCents = payRateCents
            )
        )
    }

    fun clearSession() {
        repository.clearSession()
        _uiState.value = _uiState.value.copy(hasSession = false, sessionUserId = null, statusMessage = "Session cleared", errorMessage = null)
    }

    private fun <T> runAsync(successMsg: String, action: () -> Result<T>) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch(dispatchers.io) {
            val result = action()
            if (result.isFailure) {
                val message = result.exceptionOrNull()?.message ?: "Request failed"
                _uiState.value = _uiState.value.copy(errorMessage = message, statusMessage = null, isLoading = false)
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                hasSession = repository.currentSession() != null,
                sessionUserId = repository.currentSession()?.userId,
                statusMessage = successMsg,
                errorMessage = null,
                isLoading = false
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

        _uiState.value = _uiState.value.copy(
            role = role,
            hasSession = repository.currentSession() != null,
            sessionUserId = repository.currentSession()?.userId,
            shifts = shifts,
            applications = applicationsResult.getOrThrow(),
            assignments = assignmentsResult.getOrThrow(),
            selectedShiftId = effectiveShiftId,
            selectedAssignmentId = assignmentsResult.getOrThrow().firstOrNull()?.id,
            errorMessage = null,
            statusMessage = if (keepStatusMessage) _uiState.value.statusMessage else null,
            isLoading = false
        )
    }

    private fun showDashboardFailure(err: Throwable?, keepStatusMessage: Boolean) {
        _uiState.value = _uiState.value.copy(
            errorMessage = err?.message ?: "Dashboard load failed",
            statusMessage = if (keepStatusMessage) _uiState.value.statusMessage else null,
            isLoading = false
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
