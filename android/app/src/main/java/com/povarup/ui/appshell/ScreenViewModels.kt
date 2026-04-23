package com.povarup.ui.appshell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.povarup.data.CreateShiftRequest
import com.povarup.data.repository.MarketplaceDataSource
import com.povarup.domain.Application
import com.povarup.domain.Assignment
import com.povarup.domain.AssignmentStatus
import com.povarup.domain.Payout
import com.povarup.domain.Shift
import com.povarup.ui.shared.components.ScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShiftsViewModel(private val source: MarketplaceDataSource) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<List<Shift>>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<List<Shift>>> = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = source.listShifts().fold({ ScreenState.Success(it) }, { ScreenState.Error(it.message ?: "failed") })
    }

    fun apply(shiftId: String, done: () -> Unit = {}) = viewModelScope.launch {
        source.applyToShift(shiftId)
        refresh()
        done()
    }

    class Factory(private val source: MarketplaceDataSource) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ShiftsViewModel(source) as T
    }
}

class ShiftDetailViewModel(private val source: MarketplaceDataSource, private val shiftId: String) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<Shift>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<Shift>> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = source.getShift(shiftId).fold({ ScreenState.Success(it) }, { ScreenState.Error(it.message ?: "failed") })
    }

    fun applyToShift() = viewModelScope.launch {
        val result = source.applyToShift(shiftId)
        _message.value = result.exceptionOrNull()?.message ?: "Application submitted"
        refresh()
    }

    class Factory(private val source: MarketplaceDataSource, private val shiftId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ShiftDetailViewModel(source, shiftId) as T
    }
}

class ApplicationsViewModel(private val source: MarketplaceDataSource) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<List<Application>>>(ScreenState.Loading)
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = source.listApplications().fold({ ScreenState.Success(it) }, { ScreenState.Error(it.message ?: "failed") })
    }

    class Factory(private val source: MarketplaceDataSource) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ApplicationsViewModel(source) as T
    }
}

class AssignmentsViewModel(private val source: MarketplaceDataSource) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<List<Assignment>>>(ScreenState.Loading)
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = source.listAssignments().fold({ ScreenState.Success(it) }, { ScreenState.Error(it.message ?: "failed") })
    }

    class Factory(private val source: MarketplaceDataSource) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AssignmentsViewModel(source) as T
    }
}

class AssignmentDetailViewModel(private val source: MarketplaceDataSource, private val assignmentId: String) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<Assignment>>(ScreenState.Loading)
    val state = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = source.getAssignment(assignmentId).fold({ ScreenState.Success(it) }, { ScreenState.Error(it.message ?: "failed") })
    }

    fun canCheckIn(assignment: Assignment): Boolean = assignment.status == AssignmentStatus.ASSIGNED
    fun canCheckOut(assignment: Assignment): Boolean = assignment.status == AssignmentStatus.IN_PROGRESS

    fun checkIn() = action { source.checkIn(assignmentId) }
    fun checkOut() = action { source.checkOut(assignmentId) }

    private fun action(block: suspend () -> Result<Unit>) = viewModelScope.launch {
        val result = block()
        _message.value = result.exceptionOrNull()?.message ?: "Action completed"
        refresh()
    }

    class Factory(private val source: MarketplaceDataSource, private val assignmentId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AssignmentDetailViewModel(source, assignmentId) as T
    }
}

class PayoutsViewModel(private val source: MarketplaceDataSource) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<List<Payout>>>(ScreenState.Loading)
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = source.listPayouts().fold({ ScreenState.Success(it) }, { ScreenState.Error(it.message ?: "failed") })
    }

    class Factory(private val source: MarketplaceDataSource) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PayoutsViewModel(source) as T
    }
}

class BusinessShiftCreateViewModel(private val source: MarketplaceDataSource) : ViewModel() {
    data class FormState(val title: String = "", val date: String = "", val pay: String = "", val description: String = "")

    private val _form = MutableStateFlow(FormState())
    val form = _form.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    val publishEnabled: Boolean = false
    val publishDisabledReason: String = "Publish is not wired to backend workflow yet"

    fun update(state: FormState) {
        _form.value = state
    }

    fun createDraft() = viewModelScope.launch {
        val payCents = (_form.value.pay.toIntOrNull() ?: 0) * 100
        val result = source.createShift(
            CreateShiftRequest(
                locationId = "Business HQ",
                title = _form.value.title,
                startAt = _form.value.date,
                endAt = _form.value.date,
                payRateCents = payCents
            )
        )
        _message.value = result.exceptionOrNull()?.message ?: "Draft saved"
    }

    class Factory(private val source: MarketplaceDataSource) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BusinessShiftCreateViewModel(source) as T
    }
}

class ShiftApplicantsViewModel(private val source: MarketplaceDataSource, private val shiftId: String) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<List<Application>>>(ScreenState.Loading)
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = source.listShiftApplications(shiftId).fold({ ScreenState.Success(it) }, { ScreenState.Error(it.message ?: "failed") })
    }

    fun assign(applicationId: String) = viewModelScope.launch {
        source.assignApplicant(applicationId)
        refresh()
    }

    fun reject(applicationId: String) = viewModelScope.launch {
        source.rejectApplicant(applicationId)
        refresh()
    }

    class Factory(private val source: MarketplaceDataSource, private val shiftId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ShiftApplicantsViewModel(source, shiftId) as T
    }
}

class PayoutReleaseViewModel(private val source: MarketplaceDataSource) : ViewModel() {
    private val _state = MutableStateFlow<ScreenState<List<Assignment>>>(ScreenState.Loading)
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = source.listAssignments().fold(
            { list -> ScreenState.Success(list.filter { it.status == AssignmentStatus.COMPLETED || it.status == AssignmentStatus.PAID }) },
            { ScreenState.Error(it.message ?: "failed") }
        )
    }

    fun canRelease(assignment: Assignment): Boolean = assignment.status == AssignmentStatus.COMPLETED

    fun release(assignmentId: String) = viewModelScope.launch {
        source.releasePayout(assignmentId)
        refresh()
    }

    class Factory(private val source: MarketplaceDataSource) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PayoutReleaseViewModel(source) as T
    }
}
