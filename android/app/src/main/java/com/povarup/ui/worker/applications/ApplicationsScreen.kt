package com.povarup.ui.worker.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.domain.Application

@Composable
fun ApplicationsScreen(applications: List<Application>) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(applications.groupBy { it.rawStatus }.toList()) { grouped ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(grouped.first.uppercase())
                grouped.second.forEach { app ->
                    Text("Application ${app.id} | Applied: ${app.id.takeLast(4)} | Next: ${app.rawStatus}")
                }
            }
        }
    }
}
