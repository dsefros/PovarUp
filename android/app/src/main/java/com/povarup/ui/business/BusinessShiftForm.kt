package com.povarup.ui.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.core.BusinessShiftFormState
import com.povarup.domain.CookCuisine
import com.povarup.domain.CookStation
import com.povarup.domain.DishwasherZone
import com.povarup.domain.WorkType

@Composable
fun BusinessShiftForm(
    form: BusinessShiftFormState,
    onTitleChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onStartAtChanged: (String) -> Unit,
    onEndAtChanged: (String) -> Unit,
    onPayChanged: (String) -> Unit,
    onWorkTypeChanged: (WorkType) -> Unit,
    onCookCuisineChanged: (CookCuisine) -> Unit,
    onCookStationChanged: (CookStation) -> Unit,
    onBanquetChanged: (Boolean) -> Unit,
    onDishwasherZoneChanged: (DishwasherZone) -> Unit,
    onCreateShift: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Create shift", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(form.title, onTitleChanged, modifier = Modifier.fillMaxWidth(), label = { Text("Title") })
            OutlinedTextField(form.location, onLocationChanged, modifier = Modifier.fillMaxWidth(), label = { Text("Location") })
            OutlinedTextField(form.startAt, onStartAtChanged, modifier = Modifier.fillMaxWidth(), label = { Text("Start time") }, placeholder = { Text("2026-05-10 10:00") })
            OutlinedTextField(form.endAt, onEndAtChanged, modifier = Modifier.fillMaxWidth(), label = { Text("End time") }, placeholder = { Text("2026-05-10 18:00") })
            OutlinedTextField(form.payRatePerHour, onPayChanged, modifier = Modifier.fillMaxWidth(), label = { Text("Pay / hour (USD)") }, placeholder = { Text("25.00") })

            Text("Work type", style = MaterialTheme.typography.labelLarge)
            ChipRow(options = WorkType.entries.map { it.name }, selected = form.workType.name) {
                onWorkTypeChanged(WorkType.valueOf(it))
            }

            when (form.workType) {
                WorkType.COOK -> {
                    Text("Cuisine", style = MaterialTheme.typography.labelLarge)
                    ChipRow(options = CookCuisine.entries.map { it.name }, selected = form.cookCuisine.name) {
                        onCookCuisineChanged(CookCuisine.valueOf(it))
                    }
                    Text("Station", style = MaterialTheme.typography.labelLarge)
                    ChipRow(options = CookStation.entries.map { it.name }, selected = form.cookStation.name) {
                        onCookStationChanged(CookStation.valueOf(it))
                    }
                    Text("Banquet", style = MaterialTheme.typography.labelLarge)
                    ChipRow(options = listOf("YES", "NO"), selected = if (form.isBanquet) "YES" else "NO") {
                        onBanquetChanged(it == "YES")
                    }
                }

                WorkType.WAITER,
                WorkType.BARTENDER -> {
                    Text("Banquet", style = MaterialTheme.typography.labelLarge)
                    ChipRow(options = listOf("YES", "NO"), selected = if (form.isBanquet) "YES" else "NO") {
                        onBanquetChanged(it == "YES")
                    }
                }

                WorkType.DISHWASHER -> {
                    Text("Zone", style = MaterialTheme.typography.labelLarge)
                    ChipRow(options = DishwasherZone.entries.map { it.name }, selected = form.dishwasherZone.name) {
                        onDishwasherZoneChanged(DishwasherZone.valueOf(it))
                    }
                }
            }

            Button(onClick = onCreateShift, modifier = Modifier.fillMaxWidth()) {
                Text("Create shift")
            }
        }
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(option) })
        }
    }
}
