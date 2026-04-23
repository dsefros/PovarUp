package com.povarup.ui.business.shifts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.povarup.domain.Shift
import com.povarup.ui.appshell.BusinessShiftCreateViewModel

@Composable
fun BusinessShiftsScreen(shifts: List<Shift>, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tabs: draft / published / closed")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shifts) { shift ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(shift.id) }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(shift.title)
                        Text(shift.startAt)
                        Text("Applicants: stub")
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessShiftCreateScreen(
    form: BusinessShiftCreateViewModel.FormState,
    message: String?,
    publishEnabled: Boolean,
    publishDisabledReason: String,
    onUpdate: (BusinessShiftCreateViewModel.FormState) -> Unit,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(form.title, { onUpdate(form.copy(title = it)) }, label = { Text("Title") })
        OutlinedTextField(form.date, { onUpdate(form.copy(date = it)) }, label = { Text("Date") })
        OutlinedTextField(form.pay, { onUpdate(form.copy(pay = it)) }, label = { Text("Pay") })
        OutlinedTextField(form.description, { onUpdate(form.copy(description = it)) }, label = { Text("Description") })
        Button(onClick = onSaveDraft) { Text("Save draft") }
        Button(onClick = onPublish, enabled = publishEnabled) { Text("Publish") }
        if (!publishEnabled) Text("Publish disabled: $publishDisabledReason")
        message?.let { Text(it) }
    }
}

@Composable
fun BusinessShiftDetailScreen(shift: Shift, onEdit: () -> Unit, onApplicants: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${shift.title} @ ${shift.locationId}")
        Text("${shift.startAt} → ${shift.endAt}")
        Text("Applicants preview: stub")
        Button(onClick = onEdit) { Text("Edit") }
        Button(onClick = onApplicants) { Text("Review applicants") }
    }
}
