package com.povarup.ui.worker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.core.WorkerUiState

sealed interface WorkerRoute {
    data object WorkerHome : WorkerRoute
    data object WorkerShiftList : WorkerRoute
    data object WorkerApplications : WorkerRoute
    data object WorkerAssignments : WorkerRoute
    data object WorkerPayouts : WorkerRoute
}

@Composable
fun WorkerNavHost(
    state: WorkerUiState,
    onRefresh: () -> Unit,
    onApply: (String) -> Unit,
    onDismissMessage: () -> Unit,
    onLogout: () -> Unit
) {
    val backstack = remember { mutableStateListOf<WorkerRoute>(WorkerRoute.WorkerHome) }
    val currentRoute = backstack.last()

    BackHandler(enabled = backstack.size > 1) {
        backstack.removeLast()
    }

    when (currentRoute) {
        WorkerRoute.WorkerHome -> WorkerHomeScreen(
            state = state,
            onRefresh = onRefresh,
            onLogout = onLogout,
            onOpenShiftList = { backstack.push(WorkerRoute.WorkerShiftList) },
            onOpenApplications = { backstack.push(WorkerRoute.WorkerApplications) },
            onOpenAssignments = { backstack.push(WorkerRoute.WorkerAssignments) },
            onOpenPayouts = { backstack.push(WorkerRoute.WorkerPayouts) }
        )

        WorkerRoute.WorkerShiftList -> WorkerShiftListScreen(
            state = state,
            onRetry = onRefresh,
            onRefresh = onRefresh,
            onApply = onApply,
            onDismissMessage = onDismissMessage,
            onLogout = onLogout,
            onBack = { backstack.removeLast() }
        )

        WorkerRoute.WorkerApplications -> WorkerApplicationsScreen(onBack = { backstack.removeLast() })
        WorkerRoute.WorkerAssignments -> WorkerAssignmentsScreen(onBack = { backstack.removeLast() })
        WorkerRoute.WorkerPayouts -> WorkerPayoutsScreen(onBack = { backstack.removeLast() })
    }
}

private fun SnapshotStateList<WorkerRoute>.push(route: WorkerRoute) {
    add(route)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerHomeScreen(
    state: WorkerUiState,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onOpenShiftList: () -> Unit,
    onOpenApplications: () -> Unit,
    onOpenAssignments: () -> Unit,
    onOpenPayouts: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PovarUp") },
                actions = {
                    TextButton(onClick = onRefresh, enabled = !state.isLoadingShifts) { Text("Refresh") }
                    TextButton(onClick = onLogout) { Text("Logout") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Быстрые действия", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenShiftList, modifier = Modifier.weight(1f)) { Text("Найти смены") }
                Button(onClick = onOpenApplications, modifier = Modifier.weight(1f)) { Text("Мои отклики") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenAssignments, modifier = Modifier.weight(1f)) { Text("Мои смены") }
                Button(onClick = onOpenPayouts, modifier = Modifier.weight(1f)) { Text("Выплаты") }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Сводка", style = MaterialTheme.typography.titleMedium)
                    Text("Доступные смены: ${state.shifts.size}")
                    state.applicationsCount?.let { Text("Отклики: $it") }
                    state.assignmentsCount?.let { Text("Смены: $it") }
                    state.payoutsCount?.let { Text("Выплаты: $it") }
                }
            }

            state.activeAssignment?.let { assignment ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Активная смена", style = MaterialTheme.typography.titleMedium)
                        Text(assignment.title)
                        Text(assignment.dateTimeLabel)
                        Text(assignment.locationLabel)
                        Text("Статус: ${assignment.statusLabel}")
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerApplicationsScreen(onBack: () -> Unit) = WorkerPlaceholderScreen(
    title = "Мои отклики",
    emptyStateText = "Отклики пока не добавлены в этом разделе.",
    onBack = onBack
)

@Composable
fun WorkerAssignmentsScreen(onBack: () -> Unit) = WorkerPlaceholderScreen(
    title = "Мои смены",
    emptyStateText = "Смены пока не добавлены в этом разделе.",
    onBack = onBack
)

@Composable
fun WorkerPayoutsScreen(onBack: () -> Unit) = WorkerPlaceholderScreen(
    title = "Выплаты",
    emptyStateText = "Выплаты пока не добавлены в этом разделе.",
    onBack = onBack
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerPlaceholderScreen(
    title: String,
    emptyStateText: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(emptyStateText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
