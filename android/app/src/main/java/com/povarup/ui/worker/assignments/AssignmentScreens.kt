package com.povarup.ui.worker.assignments

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.core.capabilities.CapabilitySet
import com.povarup.domain.Assignment
import com.povarup.ui.shared.components.ActionButton
import com.povarup.ui.shared.components.LifecycleTimeline
import com.povarup.ui.shared.components.StatusChip

@Composable
fun AssignmentsScreen(assignments: List<Assignment>, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tabs: active / history")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(assignments) { assignment ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(assignment.id) }) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Assignment ${assignment.id}")
                        Text("Countdown: starts soon")
                        Text("Status: ${assignment.rawStatus}")
                        Text("CTA: open")
                    }
                }
            }
        }
    }
}

@Composable
fun AssignmentDetailScreen(
    assignment: Assignment,
    capabilities: CapabilitySet,
    canCheckIn: Boolean,
    canCheckOut: Boolean,
    message: String?,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onChat: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row { Text("Status: "); StatusChip(assignment.rawStatus) }
        LifecycleTimeline(events = listOf("Assigned", "Check-in", "Check-out", "Payout"))
        ActionButton(capabilities.canCheckIn && canCheckIn, "Check-in requires assigned status", "Check-in", onCheckIn)
        ActionButton(capabilities.canCheckOut && canCheckOut, "Check-out requires in-progress status", "Check-out", onCheckOut)
        ActionButton(true, null, "Open chat", onChat)
        message?.let { Text("Info: $it") }
    }
}
