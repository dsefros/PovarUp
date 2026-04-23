package com.povarup.ui.appshell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.povarup.core.capabilities.CapabilitySet
import com.povarup.core.environment.AppEnvironment
import com.povarup.core.environment.AppMode
import com.povarup.core.environment.AppRole
import com.povarup.data.repository.MarketplaceDataSource
import com.povarup.data.repository.demo.ScenarioMarketplaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppStateViewModel(
    val demoRepository: ScenarioMarketplaceRepository,
    val productionRepository: MarketplaceDataSource,
    private val canUseProduction: Boolean,
    initialMode: AppMode = AppMode.DEMO
) : ViewModel() {
    private val _environment = MutableStateFlow(
        AppEnvironment(
            mode = initialMode,
            role = AppRole.Worker,
            capabilities = CapabilitySet.workerDefaults(),
            scenarioId = ScenarioMarketplaceRepository.SCENARIO_WORKER_BROWSING
        )
    )
    val environment: StateFlow<AppEnvironment> = _environment.asStateFlow()

    fun canEnterProduction(): Boolean = canUseProduction

    fun setRole(role: AppRole) = _environment.update {
        it.copy(role = role, capabilities = if (role is AppRole.Worker) CapabilitySet.workerDefaults() else CapabilitySet.businessDefaults())
    }

    fun setMode(mode: AppMode) {
        if (mode == AppMode.PRODUCTION && !canUseProduction) return
        _environment.update { it.copy(mode = mode) }
    }

    fun setScenario(scenarioId: String) {
        viewModelScope.launch {
            demoRepository.switchScenario(scenarioId)
            _environment.update { it.copy(scenarioId = scenarioId) }
        }
    }

    fun dataSource(): MarketplaceDataSource =
        if (_environment.value.mode == AppMode.DEMO) demoRepository else productionRepository

    class Factory(
        private val demoRepository: ScenarioMarketplaceRepository,
        private val productionRepository: MarketplaceDataSource,
        private val canUseProduction: Boolean,
        private val initialMode: AppMode
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppStateViewModel(demoRepository, productionRepository, canUseProduction, initialMode) as T
        }
    }
}
