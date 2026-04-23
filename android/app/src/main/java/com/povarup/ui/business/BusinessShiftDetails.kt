package com.povarup.ui.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.domain.Shift
import com.povarup.domain.workTypeDescription

@Composable
fun BusinessShiftDetails(
    shift: Shift,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(shift.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Work type: ${shift.workTypeDescription() ?: "General"}")
                Text("Location: ${shift.locationId}")
                Text("Time: ${shift.startAt} → ${shift.endAt}")
                Text("Pay: $${"%.2f".format(shift.payRateCents / 100.0)}/hr")
                Text("Status: ${shift.rawStatus.replaceFirstChar { it.uppercase() }}")
            }
        },
        confirmButton = {
            TextButton(onClick = onClose, modifier = Modifier.padding(8.dp)) {
                Text("Close", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
