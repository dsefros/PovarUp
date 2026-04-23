package com.povarup.ui.business.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BusinessHomeScreen(activeShifts: Int, waitingApplicants: Int, onCreateShift: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Needs attention: ${waitingApplicants} applicants")
        Text("Active shifts: $activeShifts")
        Text("Applicants waiting: $waitingApplicants")
        Button(onClick = onCreateShift) { Text("Create shift") }
    }
}
