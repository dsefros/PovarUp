package com.povarup.ui.worker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.core.ShiftCardUiModel

@Composable
fun ShiftCard(
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
            Text(text = shift.title, style = MaterialTheme.typography.titleMedium)
            shift.workTypeDetails?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = shift.dateTimeLabel)
            Text(text = shift.locationLabel)
            Text(text = shift.payLabel)
            Text(text = "Status: ${shift.statusLabel}")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = { onApply(shift.id) }, enabled = shift.canApply) {
                    Text(shift.actionLabel)
                }
            }
        }
    }
}
