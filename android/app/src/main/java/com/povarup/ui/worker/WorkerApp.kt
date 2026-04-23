package com.povarup.ui.worker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.povarup.core.BusinessDemoViewModel
import com.povarup.core.RootContent
import com.povarup.core.RootEntryMode
import com.povarup.core.WorkerViewModel
import com.povarup.core.resolveRootContent
import com.povarup.ui.business.BusinessDemoScreen

@Composable
fun WorkerApp(
    viewModel: WorkerViewModel,
    businessDemoViewModel: BusinessDemoViewModel,
    onOpenLegacyDashboard: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val businessState by businessDemoViewModel.uiState.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(RootEntryMode.WELCOME) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (state.isSessionRestoring) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (resolveRootContent(isWorkerLoggedIn = state.isLoggedIn, mode = mode)) {
                    RootContent.WELCOME -> LoginScreen(
                        state = state,
                        onUserIdChanged = viewModel::onUserIdChanged,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onLogin = {
                            mode = RootEntryMode.WORKER
                            viewModel.login()
                        },
                        onContinueAsDemoWorker = {
                            mode = RootEntryMode.WORKER
                            viewModel.continueAsDemoWorker()
                        },
                        onContinueAsDemoBusiness = {
                            mode = RootEntryMode.DEMO_BUSINESS
                            businessDemoViewModel.enterDemoBusiness()
                        },
                        onOpenLegacyDashboard = onOpenLegacyDashboard
                    )

                    RootContent.WORKER_SHIFTS -> WorkerShiftListScreen(
                        state = state,
                        onRetry = viewModel::refresh,
                        onRefresh = viewModel::refresh,
                        onApply = viewModel::applyToShift,
                        onDismissError = viewModel::dismissError,
                        onLogout = {
                            viewModel.logout()
                            mode = RootEntryMode.WELCOME
                        }
                    )

                    RootContent.DEMO_BUSINESS -> BusinessDemoScreen(
                        state = businessState,
                        onBackToWelcome = { mode = RootEntryMode.WELCOME },
                        onTitleChanged = businessDemoViewModel::onTitleChanged,
                        onLocationChanged = businessDemoViewModel::onLocationChanged,
                        onStartAtChanged = businessDemoViewModel::onStartAtChanged,
                        onEndAtChanged = businessDemoViewModel::onEndAtChanged,
                        onPayChanged = businessDemoViewModel::onPayChanged,
                        onWorkTypeChanged = businessDemoViewModel::onWorkTypeChanged,
                        onCookCuisineChanged = businessDemoViewModel::onCookCuisineChanged,
                        onCookStationChanged = businessDemoViewModel::onCookStationChanged,
                        onBanquetChanged = businessDemoViewModel::onBanquetChanged,
                        onDishwasherZoneChanged = businessDemoViewModel::onDishwasherZoneChanged,
                        onCreateShift = businessDemoViewModel::createShift,
                        onOpenShift = businessDemoViewModel::openShift,
                        onDismissError = businessDemoViewModel::dismissError,
                        onCloseDetails = businessDemoViewModel::closeShiftDetails
                    )
                }
            }
        }
    }
}
