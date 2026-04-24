package com.povarup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.povarup.core.BusinessViewModel
import com.povarup.core.RootViewModel
import com.povarup.core.WorkerViewModel
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.DemoMarketplaceRepository
import com.povarup.data.SharedPreferencesSessionStore
import com.povarup.data.WorkerRepositorySelector
import com.povarup.domain.UserRole
import com.povarup.ui.worker.PovarUpApp

class MainActivity : ComponentActivity() {
    private val sessionStore by lazy { SharedPreferencesSessionStore(applicationContext) }
    private val repository by lazy {
        WorkerRepositorySelector(
            sessionStore = sessionStore,
            realRepository = ApiMarketplaceRepository(sessionStore = sessionStore),
            demoRepository = DemoMarketplaceRepository(sessionStore = sessionStore)
        )
    }

    private val viewModel: WorkerViewModel by viewModels {
        WorkerViewModel.Factory(repository = repository)
    }

    private val businessViewModel: BusinessViewModel by viewModels {
        BusinessViewModel.Factory(repository = repository)
    }

    private val rootViewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (shouldOpenLegacy(repository.currentSession() != null, repository.currentRole())) {
            openLegacyDashboard()
            return
        }
        setContent {
            PovarUpApp(
                viewModel = viewModel,
                businessViewModel = businessViewModel,
                rootViewModel = rootViewModel,
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
            hasSession && UserRole.from(currentRoleRaw) == UserRole.ADMIN
    }
}
