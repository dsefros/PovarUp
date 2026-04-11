package com.povarup.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.MarketplaceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppDispatchers(
    val io: CoroutineDispatcher = Dispatchers.IO
)

data class MainUiState(
    val role: String,
    val baseUrl: String,
    val sessionUserId: String? = null,
    val hasSession: Boolean = false,
    val sessionStatusMessage: String? = null,
    val shiftsCount: Int? = null,
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
            sessionUserId = repository.currentSession()?.userId,
            hasSession = repository.currentSession() != null,
            isLoading = true
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refreshShifts()
    }

    fun setRole(role: String) {
        repository.setRole(role)
        _uiState.value = _uiState.value.copy(role = role)
    }

    fun createSessionForRole(userId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, sessionStatusMessage = null)
        viewModelScope.launch(dispatchers.io) {
            val result = repository.createSession(userId = userId, role = _uiState.value.role)
            _uiState.value = result.fold(
                onSuccess = { session ->
                    _uiState.value.copy(
                        hasSession = true,
                        sessionUserId = session.userId,
                        sessionStatusMessage = "Session created for ${session.role}",
                        isLoading = false
                    )
                },
                onFailure = { err ->
                    _uiState.value.copy(
                        hasSession = false,
                        sessionUserId = null,
                        sessionStatusMessage = "Session failed",
                        errorMessage = err.message ?: "Failed to create session",
                        isLoading = false
                    )
                }
            )
        }
    }

    fun clearSession() {
        repository.clearSession()
        _uiState.value = _uiState.value.copy(hasSession = false, sessionUserId = null, sessionStatusMessage = "Session cleared")
    }

    fun refreshShifts() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch(dispatchers.io) {
            val result = repository.listShifts()
            _uiState.value = result.fold(
                onSuccess = { shifts ->
                    _uiState.value.copy(shiftsCount = shifts.size, isLoading = false)
                },
                onFailure = { err ->
                    _uiState.value.copy(errorMessage = err.message ?: "Failed to load shifts", isLoading = false)
                }
            )
        }
    }

    class Factory(
        private val repository: MarketplaceRepository = ApiMarketplaceRepository(),
        private val dispatchers: AppDispatchers = AppDispatchers()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository, dispatchers) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
