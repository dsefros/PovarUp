package com.povarup.ui.business.applicants

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
import com.povarup.domain.Application

@Composable
fun ApplicantsScreen(applicants: List<Application>, onAssign: (String) -> Unit, onReject: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(applicants) { app ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Name: ${app.workerId}")
                    Text("Rating: 4.7 (stub)")
                    Button(onClick = { onAssign(app.id) }) { Text("Assign") }
                    Button(onClick = { onReject(app.id) }) { Text("Reject") }
                }
            }
        }
    }
}
