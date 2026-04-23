package com.povarup.ui.business

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.povarup.core.BusinessDemoUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDemoScreen(
    state: BusinessDemoUiState,
    onBackToWelcome: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onStartAtChanged: (String) -> Unit,
    onEndAtChanged: (String) -> Unit,
    onPayChanged: (String) -> Unit,
    onWorkTypeChanged: (com.povarup.domain.WorkType) -> Unit,
    onCookCuisineChanged: (com.povarup.domain.CookCuisine) -> Unit,
    onCookStationChanged: (com.povarup.domain.CookStation) -> Unit,
    onBanquetChanged: (Boolean) -> Unit,
    onDishwasherZoneChanged: (com.povarup.domain.DishwasherZone) -> Unit,
    onCreateShift: () -> Unit,
    onOpenShift: (String) -> Unit,
    onDismissError: () -> Unit,
    onCloseDetails: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Demo Business Cabinet") },
                actions = { TextButton(onClick = onBackToWelcome) { Text("Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Create shifts for this app session only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                BusinessShiftForm(
                    form = state.form,
                    onTitleChanged = onTitleChanged,
                    onLocationChanged = onLocationChanged,
                    onStartAtChanged = onStartAtChanged,
                    onEndAtChanged = onEndAtChanged,
                    onPayChanged = onPayChanged,
                    onWorkTypeChanged = onWorkTypeChanged,
                    onCookCuisineChanged = onCookCuisineChanged,
                    onCookStationChanged = onCookStationChanged,
                    onBanquetChanged = onBanquetChanged,
                    onDishwasherZoneChanged = onDishwasherZoneChanged,
                    onCreateShift = onCreateShift
                )
            }
            item {
                Text("My shifts", style = MaterialTheme.typography.titleMedium)
            }
            if (state.shifts.isEmpty()) {
                item {
                    Text("No shifts created yet.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(state.shifts, key = { it.id }) { shift ->
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenShift(shift.id) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(shift.title, style = MaterialTheme.typography.titleSmall)
                            Text(shift.workTypeDetails)
                            Text("Location: ${shift.locationLabel}")
                            Text("Time: ${shift.timeLabel}")
                            Text("Pay: ${shift.payLabel}")
                            Text("Status: ${shift.statusLabel}")
                        }
                    }
                }
            }
        }

        state.selectedShift?.let { selected ->
            BusinessShiftDetails(shift = selected, onClose = onCloseDetails)
        }
    }
}
