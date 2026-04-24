package com.povarup.ui.business

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.core.BusinessShiftFormState
import com.povarup.core.BusinessUiState
import com.povarup.domain.CookCuisine
import com.povarup.domain.CookStation
import com.povarup.domain.DishwasherZone
import com.povarup.domain.WorkType

sealed interface BusinessRoute {
    data object BusinessHome : BusinessRoute
    data object BusinessShiftList : BusinessRoute
    data class BusinessShiftDetail(val shiftId: String) : BusinessRoute
    data object BusinessCreateShift : BusinessRoute
}

@Composable
fun BusinessNavHost(
    state: BusinessUiState,
    statusLabel: (com.povarup.domain.Shift) -> String,
    workTypeDetails: (com.povarup.domain.Shift) -> String,
    canPublish: (com.povarup.domain.Shift) -> Boolean,
    canClose: (com.povarup.domain.Shift) -> Boolean,
    canCancel: (com.povarup.domain.Shift) -> Boolean,
    onRefresh: () -> Unit,
    onCreateShift: (onCreated: (String) -> Unit) -> Unit,
    onPublish: (String) -> Unit,
    onClose: (String) -> Unit,
    onCancel: (String) -> Unit,
    onDismissMessage: () -> Unit,
    onBackToWelcome: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onStartAtChanged: (String) -> Unit,
    onEndAtChanged: (String) -> Unit,
    onPayChanged: (String) -> Unit,
    onWorkTypeChanged: (WorkType) -> Unit,
    onCookCuisineChanged: (CookCuisine) -> Unit,
    onCookStationChanged: (CookStation) -> Unit,
    onBanquetChanged: (Boolean) -> Unit,
    onDishwasherZoneChanged: (DishwasherZone) -> Unit
) {
    val backstack = remember { mutableStateListOf<BusinessRoute>(BusinessRoute.BusinessHome) }
    val current = backstack.last()

    BackHandler(enabled = backstack.size > 1) {
        backstack.removeLast()
    }

    when (current) {
        BusinessRoute.BusinessHome -> BusinessHomeScreen(
            state = state,
            onRefresh = onRefresh,
            onLogout = onBackToWelcome,
            onOpenShiftList = { backstack += BusinessRoute.BusinessShiftList },
            onOpenCreate = { backstack += BusinessRoute.BusinessCreateShift }
        )

        BusinessRoute.BusinessShiftList -> BusinessShiftListScreen(
            state = state,
            onBack = { backstack.removeLast() },
            onRefresh = onRefresh,
            onCreate = { backstack += BusinessRoute.BusinessCreateShift },
            onOpenShift = { backstack += BusinessRoute.BusinessShiftDetail(it) },
            onDismissMessage = onDismissMessage
        )

        is BusinessRoute.BusinessShiftDetail -> BusinessShiftDetailScreen(
            state = state,
            shiftId = current.shiftId,
            statusLabel = statusLabel,
            workTypeDetails = workTypeDetails,
            canPublish = canPublish,
            canClose = canClose,
            canCancel = canCancel,
            onPublish = onPublish,
            onClose = onClose,
            onCancel = onCancel,
            onBack = { backstack.removeLast() },
            onDismissMessage = onDismissMessage
        )

        BusinessRoute.BusinessCreateShift -> BusinessCreateShiftScreen(
            state = state,
            onBack = { backstack.removeLast() },
            onCreate = {
                onCreateShift { createdShiftId ->
                    backstack.removeLast()
                    backstack += BusinessRoute.BusinessShiftDetail(createdShiftId)
                }
            },
            onDismissMessage = onDismissMessage,
            onTitleChanged = onTitleChanged,
            onLocationChanged = onLocationChanged,
            onStartAtChanged = onStartAtChanged,
            onEndAtChanged = onEndAtChanged,
            onPayChanged = onPayChanged,
            onWorkTypeChanged = onWorkTypeChanged,
            onCookCuisineChanged = onCookCuisineChanged,
            onCookStationChanged = onCookStationChanged,
            onBanquetChanged = onBanquetChanged,
            onDishwasherZoneChanged = onDishwasherZoneChanged
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessHomeScreen(
    state: BusinessUiState,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onOpenShiftList: () -> Unit,
    onOpenCreate: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("PovarUp Business") },
            actions = {
                TextButton(onClick = onRefresh) { Text("Refresh") }
                TextButton(onClick = onLogout) { Text("Logout") }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Быстрые действия", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenShiftList, modifier = Modifier.weight(1f)) { Text("Мои смены") }
                Button(onClick = onOpenCreate, modifier = Modifier.weight(1f)) { Text("Создать смену") }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Сводка", style = MaterialTheme.typography.titleMedium)
                    Text("Всего смен: ${state.summary.total}")
                    Text("Черновики: ${state.summary.draft}")
                    Text("Опубликованные: ${state.summary.published}")
                    Text("Закрытые: ${state.summary.closed}")
                    Text("Отменённые: ${state.summary.cancelled}")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessShiftListScreen(
    state: BusinessUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCreate: () -> Unit,
    onOpenShift: (String) -> Unit,
    onDismissMessage: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it.text); onDismissMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои смены") },
                actions = {
                    TextButton(onClick = onBack) { Text("Back") }
                    TextButton(onClick = onRefresh) { Text("Refresh") }
                    TextButton(onClick = onCreate) { Text("Создать") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.shifts.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.Center) {
                Text("Смен пока нет", style = MaterialTheme.typography.titleLarge)
                Text("Создайте первую смену для поиска исполнителей.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.shifts, key = { it.id }) { shift ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(shift.title, style = MaterialTheme.typography.titleMedium)
                            Text(shift.dateTimeLabel)
                            Text(shift.locationLabel)
                            Text(shift.payLabel)
                            Text("Статус: ${shift.statusLabel}")
                            TextButton(onClick = { onOpenShift(shift.id) }) { Text("Открыть") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessShiftDetailScreen(
    state: BusinessUiState,
    shiftId: String,
    statusLabel: (com.povarup.domain.Shift) -> String,
    workTypeDetails: (com.povarup.domain.Shift) -> String,
    canPublish: (com.povarup.domain.Shift) -> Boolean,
    canClose: (com.povarup.domain.Shift) -> Boolean,
    canCancel: (com.povarup.domain.Shift) -> Boolean,
    onPublish: (String) -> Unit,
    onClose: (String) -> Unit,
    onCancel: (String) -> Unit,
    onBack: () -> Unit,
    onDismissMessage: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val shift = state.rawShifts.firstOrNull { it.id == shiftId }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it.text); onDismissMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Детали смены") }, actions = { TextButton(onClick = onBack) { Text("Back") } })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (shift == null) {
                Text("Смена не найдена")
                return@Column
            }

            Text("ID: ${shift.id}")
            Text("Название: ${shift.title}", style = MaterialTheme.typography.titleMedium)
            Text("Тип работ: ${workTypeDetails(shift)}")
            Text("Время: ${shift.startAt} → ${shift.endAt}")
            Text("Локация: ${shift.locationId}")
            Text("Оплата: $${"%.2f".format(shift.payRateCents / 100.0)}/ч")
            Text("Статус: ${statusLabel(shift)}")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canPublish(shift)) {
                    Button(onClick = { onPublish(shift.id) }) { Text("Опубликовать") }
                }
                if (canClose(shift)) {
                    Button(onClick = { onClose(shift.id) }) { Text("Закрыть") }
                }
                if (canCancel(shift)) {
                    Button(onClick = { onCancel(shift.id) }) { Text("Отменить") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCreateShiftScreen(
    state: BusinessUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onDismissMessage: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onStartAtChanged: (String) -> Unit,
    onEndAtChanged: (String) -> Unit,
    onPayChanged: (String) -> Unit,
    onWorkTypeChanged: (WorkType) -> Unit,
    onCookCuisineChanged: (CookCuisine) -> Unit,
    onCookStationChanged: (CookStation) -> Unit,
    onBanquetChanged: (Boolean) -> Unit,
    onDishwasherZoneChanged: (DishwasherZone) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it.text); onDismissMessage() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Создать смену") }, actions = { TextButton(onClick = onBack) { Text("Back") } }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BusinessCreateForm(
                    form = state.form,
                    onTitleChanged = onTitleChanged,
                    onLocationChanged = onLocationChanged,
                    onStartAtChanged = onStartAtChanged,
                    onEndAtChanged = onEndAtChanged,
                    onPayChanged = onPayChanged,
                    onWorkTypeChanged = onWorkTypeChanged,
                    onCookCuisineChanged = onCookCuisineChanged,
                    onCookStationChanged = onCookStationChanged,
                    onBanquetChanged = onBanquetChanged,
                    onDishwasherZoneChanged = onDishwasherZoneChanged
                )
            }
            item { Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text("Создать черновик") } }
        }
    }
}

@Composable
private fun BusinessCreateForm(
    form: BusinessShiftFormState,
    onTitleChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onStartAtChanged: (String) -> Unit,
    onEndAtChanged: (String) -> Unit,
    onPayChanged: (String) -> Unit,
    onWorkTypeChanged: (WorkType) -> Unit,
    onCookCuisineChanged: (CookCuisine) -> Unit,
    onCookStationChanged: (CookStation) -> Unit,
    onBanquetChanged: (Boolean) -> Unit,
    onDishwasherZoneChanged: (DishwasherZone) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(form.title, onTitleChanged, label = { Text("title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.locationId, onLocationChanged, label = { Text("locationId") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.startAt, onStartAtChanged, label = { Text("startAt") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.endAt, onEndAtChanged, label = { Text("endAt") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(form.payRatePerHour, onPayChanged, label = { Text("payRate") }, modifier = Modifier.fillMaxWidth())

            Text("workType")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkType.entries.forEach { type ->
                    FilterChip(selected = type == form.workType, onClick = { onWorkTypeChanged(type) }, label = { Text(type.name) })
                }
            }

            when (form.workType) {
                WorkType.COOK -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CookCuisine.entries.forEach { value ->
                            FilterChip(selected = value == form.cookCuisine, onClick = { onCookCuisineChanged(value) }, label = { Text(value.name) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CookStation.entries.forEach { value ->
                            FilterChip(selected = value == form.cookStation, onClick = { onCookStationChanged(value) }, label = { Text(value.name) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = form.isBanquet, onClick = { onBanquetChanged(true) }, label = { Text("banquet") })
                        FilterChip(selected = !form.isBanquet, onClick = { onBanquetChanged(false) }, label = { Text("non-banquet") })
                    }
                }

                WorkType.WAITER, WorkType.BARTENDER -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = form.isBanquet, onClick = { onBanquetChanged(true) }, label = { Text("banquet") })
                        FilterChip(selected = !form.isBanquet, onClick = { onBanquetChanged(false) }, label = { Text("non-banquet") })
                    }
                }

                WorkType.DISHWASHER -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DishwasherZone.entries.forEach { value ->
                            FilterChip(selected = value == form.dishwasherZone, onClick = { onDishwasherZoneChanged(value) }, label = { Text(value.name) })
                        }
                    }
                }
            }
        }
    }
}
