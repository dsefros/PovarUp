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
import com.povarup.core.WorkerViewModel

@Composable
fun WorkerApp(
    viewModel: WorkerViewModel,
    onOpenLegacyDashboard: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when {
                state.isSessionRestoring -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                !state.isLoggedIn -> LoginScreen(
                    state = state,
                    onUserIdChanged = viewModel::onUserIdChanged,
                    onPasswordChanged = viewModel::onPasswordChanged,
                    onLogin = viewModel::login,
                    onOpenLegacyDashboard = onOpenLegacyDashboard
                )

                else -> WorkerShiftListScreen(
                    state = state,
                    onRetry = viewModel::refresh,
                    onRefresh = viewModel::refresh,
                    onApply = viewModel::applyToShift,
                    onDismissError = viewModel::dismissError,
                    onLogout = viewModel::logout
                )
            }
        }
    }
}
