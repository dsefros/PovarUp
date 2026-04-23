package com.povarup.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.povarup.data.CreateDemoBusinessShiftRequest
import com.povarup.data.DemoBusinessRepository
import com.povarup.domain.CookCuisine
import com.povarup.domain.CookStation
import com.povarup.domain.DishwasherZone
import com.povarup.domain.Shift
import com.povarup.domain.WorkType
import com.povarup.domain.workTypeDescription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BusinessShiftFormState(
    val title: String = "",
    val location: String = "",
    val startAt: String = "",
    val endAt: String = "",
    val payRatePerHour: String = "",
    val workType: WorkType = WorkType.COOK,
    val cookCuisine: CookCuisine = CookCuisine.RUSSIAN,
    val cookStation: CookStation = CookStation.HOT,
    val isBanquet: Boolean = false,
    val dishwasherZone: DishwasherZone = DishwasherZone.WHITE
)

data class BusinessShiftCardUiModel(
    val id: String,
    val title: String,
    val workTypeDetails: String,
    val locationLabel: String,
    val timeLabel: String,
    val payLabel: String,
    val statusLabel: String
)

data class BusinessDemoUiState(
    val isInDemoSession: Boolean = false,
    val form: BusinessShiftFormState = BusinessShiftFormState(),
    val shifts: List<BusinessShiftCardUiModel> = emptyList(),
    val selectedShift: Shift? = null,
    val message: UiMessage? = null
)

class BusinessDemoViewModel(
    private val repository: DemoBusinessRepository = DemoBusinessRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessDemoUiState())
    val uiState: StateFlow<BusinessDemoUiState> = _uiState.asStateFlow()

    fun enterDemoBusiness() {
        repository.enterDemoBusiness()
        refreshShifts()
        _uiState.update {
            it.copy(
                isInDemoSession = true,
                message = UiMessage("Demo business mode enabled", UiMessageKind.INFO)
            )
        }
    }

    fun onTitleChanged(value: String) = updateForm { copy(title = value) }
    fun onLocationChanged(value: String) = updateForm { copy(location = value) }
    fun onStartAtChanged(value: String) = updateForm { copy(startAt = value) }
    fun onEndAtChanged(value: String) = updateForm { copy(endAt = value) }
    fun onPayChanged(value: String) = updateForm { copy(payRatePerHour = value) }
    fun onWorkTypeChanged(value: WorkType) = updateForm { copy(workType = value) }
    fun onCookCuisineChanged(value: CookCuisine) = updateForm { copy(cookCuisine = value) }
    fun onCookStationChanged(value: CookStation) = updateForm { copy(cookStation = value) }
    fun onBanquetChanged(value: Boolean) = updateForm { copy(isBanquet = value) }
    fun onDishwasherZoneChanged(value: DishwasherZone) = updateForm { copy(dishwasherZone = value) }

    fun createShift() {
        val form = _uiState.value.form
        val payCents = (form.payRatePerHour.toDoubleOrNull()?.times(100))?.toInt()
            ?: run {
                _uiState.update { it.copy(message = UiMessage("Pay must be a valid number", UiMessageKind.ERROR)) }
                return
            }

        val request = CreateDemoBusinessShiftRequest(
            title = form.title.trim(),
            locationId = form.location.trim(),
            startAt = form.startAt.trim(),
            endAt = form.endAt.trim(),
            payRateCents = payCents,
            workType = form.workType,
            cookCuisine = form.cookCuisine.takeIf { form.workType == WorkType.COOK },
            cookStation = form.cookStation.takeIf { form.workType == WorkType.COOK },
            isBanquet = form.isBanquet.takeIf { form.workType in setOf(WorkType.COOK, WorkType.WAITER, WorkType.BARTENDER) },
            dishwasherZone = form.dishwasherZone.takeIf { form.workType == WorkType.DISHWASHER }
        )

        val result = repository.createShift(request)
        if (result.isFailure) {
            _uiState.update {
                it.copy(
                    message = UiMessage(result.exceptionOrNull()?.message ?: "Failed to create shift", UiMessageKind.ERROR)
                )
            }
            return
        }

        refreshShifts()
        _uiState.update {
            it.copy(
                form = it.form.copy(title = "", payRatePerHour = ""),
                message = UiMessage("Shift created", UiMessageKind.INFO)
            )
        }
    }

    fun openShift(shiftId: String) {
        val result = repository.getShift(shiftId)
        _uiState.update {
            it.copy(
                selectedShift = result.getOrNull(),
                message = result.exceptionOrNull()?.message?.let { text -> UiMessage(text, UiMessageKind.ERROR) }
            )
        }
    }

    fun closeShiftDetails() {
        _uiState.update { it.copy(selectedShift = null) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun refreshShifts() {
        val result = repository.listShifts()
        _uiState.update {
            it.copy(
                shifts = result.getOrNull().orEmpty().map { shift ->
                    BusinessShiftCardUiModel(
                        id = shift.id,
                        title = shift.title,
                        workTypeDetails = shift.workTypeDescription() ?: "General",
                        locationLabel = shift.locationId,
                        timeLabel = "${shift.startAt} → ${shift.endAt}",
                        payLabel = "$${"%.2f".format(shift.payRateCents / 100.0)}/hr",
                        statusLabel = shift.rawStatus.replaceFirstChar { c -> c.uppercase() }
                    )
                },
                message = result.exceptionOrNull()?.message?.let { text -> UiMessage(text, UiMessageKind.ERROR) }
            )
        }
    }

    private fun updateForm(update: BusinessShiftFormState.() -> BusinessShiftFormState) {
        _uiState.update { it.copy(form = it.form.update(), message = null) }
    }

    class Factory(
        private val repository: DemoBusinessRepository = DemoBusinessRepository()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BusinessDemoViewModel::class.java)) {
                return BusinessDemoViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
