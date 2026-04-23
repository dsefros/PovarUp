package com.povarup.ui.business.operations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.domain.Assignment

@Composable
fun OperationsScreen(assignments: List<Assignment>, onOpenDetail: (String) -> Unit, onOpenPayoutRelease: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onOpenPayoutRelease) { Text("Open payout release") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(assignments) { assignment ->
                Card(Modifier.fillMaxWidth().clickable { onOpenDetail(assignment.id) }) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${assignment.id} - ${assignment.rawStatus}")
                        Text("CTA: open detail")
                    }
                }
            }
        }
    }
}

@Composable
fun PayoutReleaseScreen(
    assignments: List<Assignment>,
    canRelease: (Assignment) -> Boolean,
    onRelease: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(assignments) { assignment ->
            val enabled = canRelease(assignment)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${assignment.id} ${assignment.rawStatus}")
                    Button(onClick = { onRelease(assignment.id) }, enabled = enabled) { Text("Release payout") }
                    if (!enabled) Text("Release blocked: payout can be released only for completed assignments")
                }
            }
        }
    }
}
