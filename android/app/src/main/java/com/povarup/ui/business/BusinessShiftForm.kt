package com.povarup.ui.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = "Shift basics") {
                OutlinedTextField(form.title, onTitleChanged, modifier = Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
                OutlinedTextField(form.location, onLocationChanged, modifier = Modifier.fillMaxWidth(), label = { Text("Location") }, singleLine = true)
                OutlinedTextField(
                    form.startAt,
                    onStartAtChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Start time") },
                    placeholder = { Text("2026-05-10 10:00") },
                    supportingText = { Text("Format: YYYY-MM-DD HH:mm") },
                    singleLine = true
                )
                OutlinedTextField(
                    form.endAt,
                    onEndAtChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("End time") },
                    placeholder = { Text("2026-05-10 18:00") },
                    supportingText = { Text("Format: YYYY-MM-DD HH:mm") },
                    singleLine = true
                )
                OutlinedTextField(
                    form.payRatePerHour,
                    onPayChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pay / hour (USD)") },
                    placeholder = { Text("25.00") },
                    singleLine = true
                )
            }

            SectionCard(title = "Work type") {
                ChipRow(
                    options = WorkType.entries,
                    selected = form.workType,
                    labelOf = { it.humanLabel() },
                    onSelect = onWorkTypeChanged
                )
            }

            SectionCard(title = "Type-specific details") {
                when (form.workType) {
                    WorkType.COOK -> {
                        Text("Cuisine", style = MaterialTheme.typography.labelLarge)
                        ChipRow(
                            options = CookCuisine.entries,
                            selected = form.cookCuisine,
                            labelOf = { it.humanLabel() },
                            onSelect = onCookCuisineChanged
                        )
                        Text("Station", style = MaterialTheme.typography.labelLarge)
                        ChipRow(
                            options = CookStation.entries,
                            selected = form.cookStation,
                            labelOf = { it.humanLabel() },
                            onSelect = onCookStationChanged
                        )
                        Text("Banquet", style = MaterialTheme.typography.labelLarge)
                        YesNoRow(value = form.isBanquet, onChange = onBanquetChanged)
                    }

                    WorkType.WAITER,
                    WorkType.BARTENDER -> {
                        Text("Banquet", style = MaterialTheme.typography.labelLarge)
                        YesNoRow(value = form.isBanquet, onChange = onBanquetChanged)
                    }

                    WorkType.DISHWASHER -> {
                        Text("Zone", style = MaterialTheme.typography.labelLarge)
                        ChipRow(
                            options = DishwasherZone.entries,
                            selected = form.dishwasherZone,
                            labelOf = { it.humanLabel() },
                            onSelect = onDishwasherZoneChanged
                        )
                    }
                }

                Text(
                    text = "Preview: ${previewWorkType(form)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SectionCard(title = "Action") {
                Button(onClick = onCreateShift, modifier = Modifier.fillMaxWidth()) {
                    Text("Create shift")
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun YesNoRow(value: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = value, onClick = { onChange(true) }, label = { Text("Yes") })
        FilterChip(selected = !value, onClick = { onChange(false) }, label = { Text("No") })
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(labelOf(option)) })
        }
    }
}

private fun Enum<*>.humanLabel(): String =
    name.lowercase().split("_").joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }

private fun previewWorkType(form: BusinessShiftFormState): String = when (form.workType) {
    WorkType.COOK -> listOf(
        "Cook",
        form.cookCuisine.humanLabel(),
        form.cookStation.humanLabel(),
        if (form.isBanquet) "Banquet" else "Non-banquet"
    ).joinToString(" · ")
    WorkType.WAITER -> "Waiter · ${if (form.isBanquet) "Banquet" else "Non-banquet"}"
    WorkType.BARTENDER -> "Bartender · ${if (form.isBanquet) "Banquet" else "Non-banquet"}"
    WorkType.DISHWASHER -> "Dishwasher · ${form.dishwasherZone.humanLabel()}"
}
