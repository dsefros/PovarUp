package com.povarup.ui.worker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.povarup.core.WorkerUiState

@Composable
fun LoginScreen(
    state: WorkerUiState,
    onUserIdChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: () -> Unit,
    onContinueAsDemoWorker: () -> Unit,
    onContinueAsDemoBusiness: () -> Unit,
    onOpenLegacyDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Welcome", style = MaterialTheme.typography.headlineSmall)

                Text(text = "Real access", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = state.loginForm.userId,
                    onValueChange = onUserIdChanged,
                    label = { Text("User ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.loginForm.password,
                    onValueChange = onPasswordChanged,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                state.errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = onLogin,
                    enabled = state.loginForm.isValid && !state.isLoggingIn,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isLoggingIn) "Signing in..." else "Sign in")
                }

                Text(text = "Demo access", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Demo mode is for current-session product walkthroughs and does not affect real account data.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = onContinueAsDemoWorker,
                    enabled = !state.isLoggingIn,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue as Demo Worker")
                }
                Button(
                    onClick = onContinueAsDemoBusiness,
                    enabled = !state.isLoggingIn,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue as Demo Business")
                }

                Text(text = "Internal / legacy", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Business/admin legacy access for internal workflows.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = onOpenLegacyDashboard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Legacy Dashboard")
                }
            }
        }
    }
}
