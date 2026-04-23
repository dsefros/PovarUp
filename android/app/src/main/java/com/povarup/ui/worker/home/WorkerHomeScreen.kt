package com.povarup.ui.worker.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.domain.Application
import com.povarup.domain.Assignment
import com.povarup.domain.Shift
import com.povarup.ui.shared.components.EmptyState

@Composable
fun WorkerHomeScreen(
    activeAssignment: Assignment?,
    upcomingShifts: List<Shift>,
    applicationUpdates: List<Application>,
    onGoToFeed: () -> Unit,
    onOpenApplications: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillParentMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Active assignment")
                    Text(activeAssignment?.id ?: "No active assignment")
                }
            }
        }
        item { Text("Upcoming shifts") }
        if (upcomingShifts.isEmpty()) item { EmptyState("No upcoming shifts", "Browse the feed to apply") { Text("Open feed", modifier = Modifier.clickable(onClick = onGoToFeed)) } }
        items(upcomingShifts.take(3)) { shift -> Text("• ${shift.title} (${shift.startAt})") }
        item { Text("Application updates") }
        if (applicationUpdates.isEmpty()) {
            item { Text("No updates yet. Open Applications.", modifier = Modifier.clickable(onClick = onOpenApplications)) }
        }
        items(applicationUpdates.take(3)) { app -> Text("• ${app.shiftId}: ${app.rawStatus}", modifier = Modifier.clickable(onClick = onOpenApplications)) }
    }
}
