package com.povarup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.povarup.core.environment.AppMode
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.SharedPreferencesSessionStore
import com.povarup.data.repository.api.ProductionMarketplaceDataSource
import com.povarup.data.repository.demo.ScenarioMarketplaceRepository
import com.povarup.domain.UserRole
import com.povarup.ui.appshell.AppShell
import com.povarup.ui.appshell.AppStateViewModel

class MainActivity : ComponentActivity() {
    private val sessionStore by lazy { SharedPreferencesSessionStore(applicationContext) }
    private val apiRepository by lazy { ApiMarketplaceRepository(sessionStore = sessionStore) }

    private val appStateViewModel: AppStateViewModel by viewModels {
        val currentRole = UserRole.from(apiRepository.currentRole())
        val hasSession = apiRepository.currentSession() != null
        val canUseProduction = hasSession && currentRole == UserRole.WORKER
        val initialMode = if (canUseProduction) AppMode.PRODUCTION else AppMode.DEMO
        AppStateViewModel.Factory(
            demoRepository = ScenarioMarketplaceRepository(),
            productionRepository = ProductionMarketplaceDataSource(apiRepository),
            canUseProduction = canUseProduction,
            initialMode = initialMode
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (shouldOpenLegacy(apiRepository.currentSession() != null, apiRepository.currentRole())) {
            openLegacyDashboard()
            return
        }
        setContent {
            MaterialTheme {
                Surface { AppShell(appStateViewModel) }
            }
        }
    }

    private fun openLegacyDashboard() {
        startActivity(Intent(this, LegacyDashboardActivity::class.java))
        finish()
    }

    companion object {
        internal fun shouldOpenLegacy(hasSession: Boolean, currentRoleRaw: String): Boolean =
            hasSession && UserRole.from(currentRoleRaw) in setOf(UserRole.BUSINESS, UserRole.ADMIN)
    }
}
