package com.povarup.ui.worker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.povarup.core.BusinessViewModel
import com.povarup.core.RootContent
import com.povarup.core.RootEntryMode
import com.povarup.core.RootViewModel
import com.povarup.core.WorkerViewModel
import com.povarup.core.resolveRootContent
import com.povarup.ui.business.BusinessNavHost

@Composable
fun PovarUpApp(
    viewModel: WorkerViewModel,
    businessViewModel: BusinessViewModel,
    rootViewModel: RootViewModel,
    onOpenLegacyDashboard: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val businessState by businessViewModel.uiState.collectAsStateWithLifecycle()
    val mode by rootViewModel.mode.collectAsStateWithLifecycle()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (state.isSessionRestoring) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (resolveRootContent(
                    isWorkerLoggedIn = state.isLoggedIn,
                    isBusinessDemoSessionActive = businessState.isInDemoSession,
                    mode = mode
                )) {
                    RootContent.WELCOME -> LoginScreen(
                        state = state,
                        onUserIdChanged = viewModel::onUserIdChanged,
                        onPasswordChanged = viewModel::onPasswordChanged,
                        onLogin = {
                            rootViewModel.setMode(RootEntryMode.WORKER)
                            viewModel.login()
                        },
                        onContinueAsDemoWorker = {
                            rootViewModel.setMode(RootEntryMode.WORKER)
                            viewModel.continueAsDemoWorker()
                        },
                        onContinueAsDemoBusiness = {
                            rootViewModel.setMode(RootEntryMode.DEMO_BUSINESS)
                            businessViewModel.enterBusiness()
                        },
                        onOpenLegacyDashboard = onOpenLegacyDashboard
                    )

                    RootContent.WORKER_SHIFTS -> WorkerNavHost(
                        state = state,
                        onRefresh = viewModel::refresh,
                        onApply = viewModel::applyToShift,
                        onWithdrawApplication = viewModel::withdrawApplication,
                        onCheckIn = viewModel::checkIn,
                        onCheckOut = viewModel::checkOut,
                        onDismissMessage = viewModel::dismissMessage,
                        onLogout = {
                            viewModel.logout()
                            rootViewModel.setMode(RootEntryMode.WELCOME)
                        }
                    )

                    RootContent.DEMO_BUSINESS -> BusinessNavHost(
                        state = businessState,
                        statusLabel = businessViewModel::statusLabel,
                        workTypeDetails = businessViewModel::workTypeDetails,
                        canPublish = businessViewModel::canPublish,
                        canClose = businessViewModel::canClose,
                        canCancel = businessViewModel::canCancel,
                        onRefresh = businessViewModel::refreshShifts,
                        onCreateShift = businessViewModel::createDraftShift,
                        onPublish = businessViewModel::publishShift,
                        onClose = businessViewModel::closeShift,
                        onCancel = businessViewModel::cancelShift,
                        onDismissMessage = businessViewModel::dismissMessage,
                        onBackToWelcome = {
                            businessViewModel.logout()
                            rootViewModel.setMode(RootEntryMode.WELCOME)
                        },
                        onTitleChanged = businessViewModel::onTitleChanged,
                        onLocationChanged = businessViewModel::onLocationIdChanged,
                        onStartAtChanged = businessViewModel::onStartAtChanged,
                        onEndAtChanged = businessViewModel::onEndAtChanged,
                        onPayChanged = businessViewModel::onPayChanged,
                        onWorkTypeChanged = businessViewModel::onWorkTypeChanged,
                        onCookCuisineChanged = businessViewModel::onCookCuisineChanged,
                        onCookStationChanged = businessViewModel::onCookStationChanged,
                        onBanquetChanged = businessViewModel::onBanquetChanged,
                        onDishwasherZoneChanged = businessViewModel::onDishwasherZoneChanged
                    )
                }
            }
        }
    }
}
