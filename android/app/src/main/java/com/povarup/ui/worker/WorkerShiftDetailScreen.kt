package com.povarup.ui.worker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.core.ShiftCardUiModel
import com.povarup.core.WorkerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerShiftDetailScreen(
    state: WorkerUiState,
    shiftId: String,
    onApply: (String) -> Unit,
    onDismissMessage: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val shift = state.shifts.firstOrNull { it.id == shiftId }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it.text)
            onDismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали смены") },
                actions = {
                    TextButton(onClick = onBack) { Text("Back") }
                    TextButton(onClick = onLogout) { Text("Logout") }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (shift == null) {
                Text(
                    text = "Смена не найдена или больше недоступна.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                ShiftDetailCard(
                    shift = shift,
                    onApply = onApply
                )
            }
        }
    }
}

@Composable
private fun ShiftDetailCard(
    shift: ShiftCardUiModel,
    onApply: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = shift.title, style = MaterialTheme.typography.titleLarge)
            shift.workTypeDetails?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = shift.dateTimeLabel)
            Text(text = shift.locationLabel)
            Text(text = shift.payLabel)
            Text(text = "Status: ${shift.statusLabel}")
            Button(
                onClick = { onApply(shift.id) },
                enabled = shift.canApply,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = shift.actionLabel)
            }
        }
    }
}
