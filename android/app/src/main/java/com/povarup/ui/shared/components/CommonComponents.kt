package com.povarup.ui.shared.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatusChip(status: String) {
    AssistChip(onClick = {}, label = { Text(status.uppercase()) })
}

@Composable
fun LifecycleTimeline(events: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        events.forEach { Text("• $it") }
    }
}

@Composable
fun ActionButton(enabled: Boolean, reasonIfDisabled: String? = null, text: String, onClick: () -> Unit) {
    Column {
        Button(onClick = onClick, enabled = enabled) { Text(text) }
        if (!enabled && !reasonIfDisabled.isNullOrBlank()) {
            Text(reasonIfDisabled, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun EmptyState(title: String, description: String, action: (@Composable () -> Unit)? = null) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description)
        action?.invoke()
    }
}

@Composable
fun LoadingState() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
    }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)? = null) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Error", style = MaterialTheme.typography.titleMedium)
        Text(message)
        onRetry?.let { OutlinedButton(onClick = it) { Text("Retry") } }
    }
}
