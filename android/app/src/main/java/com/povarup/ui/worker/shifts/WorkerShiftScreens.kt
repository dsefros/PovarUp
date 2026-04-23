package com.povarup.ui.worker.shifts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.core.capabilities.CapabilitySet
import com.povarup.domain.Shift
import com.povarup.ui.shared.components.ActionButton
import com.povarup.ui.shared.components.StatusChip

@Composable
fun ShiftFeedScreen(shifts: List<Shift>, capabilities: CapabilitySet, onOpenDetail: (String) -> Unit, onApply: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Filters: [stub]")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shifts) { shift ->
                Card(Modifier.fillMaxWidth().clickable { onOpenDetail(shift.id) }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(shift.title, style = MaterialTheme.typography.titleMedium)
                        Text("${shift.startAt} → ${shift.endAt}")
                        Text("$${"%.2f".format(shift.payRateCents / 100.0)}/h")
                        Text(shift.locationId)
                        StatusChip(shift.rawStatus)
                        ActionButton(enabled = capabilities.canApply, reasonIfDisabled = "Applications disabled", text = "Apply") { onApply(shift.id) }
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftDetailScreen(shift: Shift, capabilities: CapabilitySet, onApply: () -> Unit, infoMessage: String? = null) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(shift.title, style = MaterialTheme.typography.headlineSmall)
        Text("Pay: $${"%.2f".format(shift.payRateCents / 100.0)}/h")
        Text("Schedule: ${shift.startAt} → ${shift.endAt}")
        Text("Description: Shift details placeholder")
        Text("Requirements: Relevant experience")
        Row { Text("Status: "); StatusChip(shift.rawStatus) }
        ActionButton(capabilities.canApply, "Blocked by capability", "Apply", onApply)
        infoMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
