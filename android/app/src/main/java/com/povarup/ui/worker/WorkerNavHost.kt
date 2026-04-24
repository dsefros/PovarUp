package com.povarup.ui.worker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.core.WorkerAssignmentUiModel
import com.povarup.core.WorkerUiState
import com.povarup.domain.AssignmentStatus

sealed interface WorkerRoute {
    data object WorkerHome : WorkerRoute
    data object WorkerShiftList : WorkerRoute
    data class WorkerShiftDetail(val shiftId: String) : WorkerRoute
    data object WorkerApplications : WorkerRoute
    data object WorkerAssignments : WorkerRoute
    data class WorkerAssignmentDetail(val assignmentId: String) : WorkerRoute
    data object WorkerPayouts : WorkerRoute
}

@Composable
fun WorkerNavHost(
    state: WorkerUiState,
    onRefresh: () -> Unit,
    onApply: (String) -> Unit,
    onCheckIn: (String) -> Unit,
    onCheckOut: (String) -> Unit,
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
            onOpenPayouts = { backstack.push(WorkerRoute.WorkerPayouts) },
            onOpenActiveAssignment = { assignmentId -> backstack.push(WorkerRoute.WorkerAssignmentDetail(assignmentId)) }
        )

        WorkerRoute.WorkerShiftList -> WorkerShiftListScreen(
            state = state,
            onRetry = onRefresh,
            onRefresh = onRefresh,
            onOpenShiftDetail = { shiftId -> backstack.push(WorkerRoute.WorkerShiftDetail(shiftId)) },
            onDismissMessage = onDismissMessage,
            onLogout = onLogout,
            onBack = { backstack.removeLast() }
        )

        is WorkerRoute.WorkerShiftDetail -> WorkerShiftDetailScreen(
            state = state,
            shiftId = currentRoute.shiftId,
            onApply = onApply,
            onDismissMessage = onDismissMessage,
            onLogout = onLogout,
            onBack = { backstack.removeLast() }
        )

        WorkerRoute.WorkerApplications -> WorkerApplicationsScreen(onBack = { backstack.removeLast() })
        WorkerRoute.WorkerAssignments -> WorkerAssignmentsScreen(
            state = state,
            onDismissMessage = onDismissMessage,
            onBack = { backstack.removeLast() },
            onOpenAssignment = { assignmentId -> backstack.push(WorkerRoute.WorkerAssignmentDetail(assignmentId)) }
        )

        is WorkerRoute.WorkerAssignmentDetail -> WorkerAssignmentDetailScreen(
            state = state,
            assignmentId = currentRoute.assignmentId,
            onCheckIn = onCheckIn,
            onCheckOut = onCheckOut,
            onDismissMessage = onDismissMessage,
            onBack = { backstack.removeLast() }
        )

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
    onOpenPayouts: () -> Unit,
    onOpenActiveAssignment: (String) -> Unit
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
                        TextButton(onClick = { onOpenActiveAssignment(assignment.assignmentId) }) { Text("Открыть") }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerAssignmentsScreen(
    state: WorkerUiState,
    onDismissMessage: () -> Unit,
    onBack: () -> Unit,
    onOpenAssignment: (String) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it.text)
            onDismissMessage()
        }
    }

    val active = state.assignments.filter { it.status == AssignmentStatus.IN_PROGRESS }
    val upcoming = state.assignments.filter { it.status == AssignmentStatus.ASSIGNED }
    val completed = state.assignments.filter { it.status == AssignmentStatus.COMPLETED || it.status == AssignmentStatus.PAID }
    val cancelled = state.assignments.filter { it.status == AssignmentStatus.CANCELLED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои смены") },
                actions = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (state.assignments.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Назначенных смен пока нет", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                assignmentSection("Активная смена", active, onOpenAssignment)
                assignmentSection("Предстоящие", upcoming, onOpenAssignment)
                assignmentSection("Завершенные", completed, onOpenAssignment)
                if (cancelled.isNotEmpty()) {
                    assignmentSection("Отмененные", cancelled, onOpenAssignment)
                }
            }
        }
    }
}

private fun LazyListScope.assignmentSection(
    title: String,
    assignments: List<WorkerAssignmentUiModel>,
    onOpenAssignment: (String) -> Unit
) {
    if (assignments.isEmpty()) return
    item {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
    items(assignments, key = { it.assignmentId }) { assignment ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(assignment.title, style = MaterialTheme.typography.titleMedium)
                Text("Assignment: ${assignment.assignmentId}")
                Text("Shift: ${assignment.shiftId}")
                Text(assignment.dateTimeLabel)
                Text(assignment.locationLabel)
                Text("Статус: ${assignment.statusLabel}")
                TextButton(onClick = { onOpenAssignment(assignment.assignmentId) }) { Text("Открыть") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerAssignmentDetailScreen(
    state: WorkerUiState,
    assignmentId: String,
    onCheckIn: (String) -> Unit,
    onCheckOut: (String) -> Unit,
    onDismissMessage: () -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val assignment = state.assignments.firstOrNull { it.assignmentId == assignmentId }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it.text)
            onDismissMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Моя смена") },
                actions = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (assignment == null) {
                Text("Смена не найдена", style = MaterialTheme.typography.titleMedium)
                Text("Проверьте список назначений и попробуйте снова.")
                return@Column
            }

            Text(assignment.title, style = MaterialTheme.typography.titleMedium)
            Text("Assignment ID: ${assignment.assignmentId}")
            Text("Shift ID: ${assignment.shiftId}")
            Text(assignment.dateTimeLabel)
            Text(assignment.locationLabel)
            Text("Статус: ${assignment.statusLabel}")
            Text("Этап: ${assignment.lifecycleText}")

            when {
                assignment.canCheckIn -> Button(onClick = { onCheckIn(assignment.assignmentId) }) { Text("Начать смену") }
                assignment.canCheckOut -> Button(onClick = { onCheckOut(assignment.assignmentId) }) { Text("Завершить смену") }
                assignment.isPaid -> Card(modifier = Modifier.fillMaxWidth()) {
                    Text("Смена оплачена", modifier = Modifier.padding(12.dp))
                }

                assignment.status == AssignmentStatus.COMPLETED -> Card(modifier = Modifier.fillMaxWidth()) {
                    Text("Смена завершена", modifier = Modifier.padding(12.dp))
                }

                assignment.status == AssignmentStatus.CANCELLED -> Card(modifier = Modifier.fillMaxWidth()) {
                    Text("Смена отменена", modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

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
