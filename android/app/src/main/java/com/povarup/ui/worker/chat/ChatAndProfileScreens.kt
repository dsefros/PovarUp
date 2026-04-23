package com.povarup.ui.worker.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.core.environment.AppMode
import com.povarup.core.environment.AppRole

@Composable
fun ChatScreen(threadId: String) {
    val text = remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Chat thread: $threadId")
        Text("Messages list (stub)")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = text.value, onValueChange = { text.value = it }, modifier = Modifier.weight(1f))
            Button(onClick = { text.value = "" }) { Text("Send") }
        }
    }
}

@Composable
fun ProfileScreen(
    mode: AppMode,
    role: AppRole,
    scenarioId: String?,
    scenarios: List<String>,
    canSelectProduction: Boolean,
    onRoleSwitch: (AppRole) -> Unit,
    onScenarioSwitch: (String) -> Unit,
    onModeSwitch: (AppMode) -> Unit,
    onLogout: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Role: ${role.label}")
        Text("Mode: ${mode.name}")
        Button(onClick = onLogout) { Text("Logout") }
        if (mode == AppMode.DEMO) {
            Text("Demo role switch")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onRoleSwitch(AppRole.Worker) }) { Text("Worker") }
                Button(onClick = { onRoleSwitch(AppRole.Business) }) { Text("Business") }
            }
            Text("Scenario: ${scenarioId ?: "none"}")
            scenarios.forEach { id -> Button(onClick = { onScenarioSwitch(id) }) { Text(id) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onModeSwitch(AppMode.DEMO) }) { Text("DEMO") }
                Button(onClick = { onModeSwitch(AppMode.PRODUCTION) }, enabled = canSelectProduction) { Text("PROD") }
            }
            if (!canSelectProduction) {
                Text("Production mode is locked until a valid worker production session exists")
            }
        }
    }
}
