package com.povarup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.povarup.core.WorkerViewModel
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.SharedPreferencesSessionStore
import com.povarup.domain.UserRole
import com.povarup.ui.worker.WorkerApp

class MainActivity : ComponentActivity() {
    private val sessionStore by lazy { SharedPreferencesSessionStore(applicationContext) }
    private val repository by lazy { ApiMarketplaceRepository(sessionStore = sessionStore) }

    private val viewModel: WorkerViewModel by viewModels {
        WorkerViewModel.Factory(repository = repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (shouldOpenLegacy(repository.currentSession() != null, repository.currentRole())) {
            openLegacyDashboard()
            return
        }
        setContent {
            WorkerApp(
                viewModel = viewModel,
                onOpenLegacyDashboard = ::openLegacyDashboard
            )
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
