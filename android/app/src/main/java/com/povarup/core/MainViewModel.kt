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
            isLoading = true
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        refreshShifts()
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
