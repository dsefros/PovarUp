package com.povarup.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.CreateShiftRequest
import com.povarup.data.DemoMarketplaceRepository
import com.povarup.data.MarketplaceRepository
import com.povarup.data.WorkerDataSourceMode
import com.povarup.data.WorkerModeSelectable
import com.povarup.domain.CookCuisine
import com.povarup.domain.CookStation
import com.povarup.domain.DishwasherZone
import com.povarup.domain.Shift
import com.povarup.domain.ShiftStatus
import com.povarup.domain.UserRole
import com.povarup.domain.WorkType
import com.povarup.domain.capability
import com.povarup.domain.workTypeDescription
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BusinessDispatchers(val io: CoroutineDispatcher = Dispatchers.IO)

data class BusinessShiftFormState(
    val title: String = "",
    val locationId: String = "",
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
    val dateTimeLabel: String,
    val locationLabel: String,
    val payLabel: String,
    val statusLabel: String
)

data class BusinessSummaryUiModel(
    val total: Int = 0,
    val draft: Int = 0,
    val published: Int = 0,
    val closed: Int = 0,
    val cancelled: Int = 0
)

data class BusinessUiState(
    val isInDemoSession: Boolean = false,
    val isLoading: Boolean = false,
    val shifts: List<BusinessShiftCardUiModel> = emptyList(),
    val rawShifts: List<Shift> = emptyList(),
    val form: BusinessShiftFormState = BusinessShiftFormState(),
    val message: UiMessage? = null,
    val summary: BusinessSummaryUiModel = BusinessSummaryUiModel()
)

class BusinessViewModel(
    private val repository: MarketplaceRepository = ApiMarketplaceRepository(),
    private val dispatchers: BusinessDispatchers = BusinessDispatchers()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessUiState())
    val uiState: StateFlow<BusinessUiState> = _uiState.asStateFlow()

    fun enterBusiness() {
        (repository as? WorkerModeSelectable)?.selectMode(WorkerDataSourceMode.DEMO)
        viewModelScope.launch(dispatchers.io) {
            val loginResult = repository.login(
                DemoMarketplaceRepository.DEMO_BUSINESS_USER_ID,
                DemoMarketplaceRepository.DEMO_BUSINESS_PASSWORD
            )
            if (loginResult.isFailure) {
                postError(loginResult.exceptionOrNull()?.message ?: "Не удалось открыть бизнес-демо")
                return@launch
            }
            refreshShiftsInternal(showMessage = false, isDemoSession = true)
        }
    }

    fun refreshShifts() {
        viewModelScope.launch(dispatchers.io) {
            refreshShiftsInternal(showMessage = true, isDemoSession = _uiState.value.isInDemoSession)
        }
    }

    fun onTitleChanged(value: String) = updateForm { copy(title = value) }
    fun onLocationIdChanged(value: String) = updateForm { copy(locationId = value) }
    fun onStartAtChanged(value: String) = updateForm { copy(startAt = value) }
    fun onEndAtChanged(value: String) = updateForm { copy(endAt = value) }
    fun onPayChanged(value: String) = updateForm { copy(payRatePerHour = value) }
    fun onWorkTypeChanged(value: WorkType) = updateForm { copy(workType = value) }
    fun onCookCuisineChanged(value: CookCuisine) = updateForm { copy(cookCuisine = value) }
    fun onCookStationChanged(value: CookStation) = updateForm { copy(cookStation = value) }
    fun onBanquetChanged(value: Boolean) = updateForm { copy(isBanquet = value) }
    fun onDishwasherZoneChanged(value: DishwasherZone) = updateForm { copy(dishwasherZone = value) }

    fun createDraftShift(onCreated: (String) -> Unit = {}) {
        val form = _uiState.value.form
        val title = form.title.trim()
        val location = form.locationId.trim()
        val startAt = form.startAt.trim()
        val endAt = form.endAt.trim()
        val payCents = (form.payRatePerHour.toDoubleOrNull()?.times(100))?.toInt() ?: 0

        when {
            title.isBlank() -> return postError("Введите название смены")
            location.isBlank() -> return postError("Введите locationId")
            startAt.isBlank() || endAt.isBlank() -> return postError("Введите startAt и endAt")
            payCents <= 0 -> return postError("Ставка должна быть больше нуля")
        }

        viewModelScope.launch(dispatchers.io) {
            val result = repository.createShift(
                CreateShiftRequest(
                    locationId = location,
                    title = title,
                    startAt = startAt,
                    endAt = endAt,
                    payRateCents = payCents
                )
            )
            result.onSuccess { shift ->
                _uiState.update { it.copy(form = it.form.copy(title = "", payRatePerHour = "")) }
                refreshShiftsInternal(showMessage = false, isDemoSession = _uiState.value.isInDemoSession)
                _uiState.update { it.copy(message = UiMessage("Черновик создан", UiMessageKind.INFO)) }
                onCreated(shift.id)
            }.onFailure { error ->
                postError(error.message ?: "Не удалось создать смену")
            }
        }
    }

    fun publishShift(shiftId: String) = transition(shiftId, "Смена опубликована") { repository.publishShift(shiftId) }
    fun closeShift(shiftId: String) = transition(shiftId, "Смена закрыта") { repository.closeShift(shiftId) }
    fun cancelShift(shiftId: String) = transition(shiftId, "Смена отменена") { repository.cancelShift(shiftId) }

    fun statusLabel(shift: Shift): String = when (shift.status) {
        ShiftStatus.DRAFT -> "Черновик"
        ShiftStatus.PUBLISHED -> "Опубликована"
        ShiftStatus.CLOSED -> "Закрыта"
        ShiftStatus.CANCELLED -> "Отменена"
        else -> "Статус уточняется"
    }

    fun canPublish(shift: Shift): Boolean = shift.capability(UserRole.BUSINESS, alreadyRelated = false).canPublish
    fun canClose(shift: Shift): Boolean = shift.capability(UserRole.BUSINESS, alreadyRelated = false).canClose
    fun canCancel(shift: Shift): Boolean = shift.capability(UserRole.BUSINESS, alreadyRelated = false).canCancel
    fun findShift(shiftId: String): Shift? = _uiState.value.rawShifts.firstOrNull { it.id == shiftId }
    fun workTypeDetails(shift: Shift): String = shift.workTypeDescription() ?: "Общий тип работ"

    fun logout() {
        repository.clearSession()
        _uiState.value = BusinessUiState()
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun transition(shiftId: String, successText: String, action: () -> Result<Shift>) {
        viewModelScope.launch(dispatchers.io) {
            action().onSuccess {
                refreshShiftsInternal(showMessage = false, isDemoSession = _uiState.value.isInDemoSession)
                _uiState.update { state -> state.copy(message = UiMessage(successText, UiMessageKind.INFO)) }
            }.onFailure { error ->
                postError(error.message ?: "Не удалось выполнить действие")
            }
        }
    }

    private fun refreshShiftsInternal(showMessage: Boolean, isDemoSession: Boolean) {
        _uiState.update { it.copy(isLoading = true) }
        val result = repository.listBusinessShifts()
        result.onSuccess { shifts ->
            val summary = BusinessSummaryUiModel(
                total = shifts.size,
                draft = shifts.count { it.status == ShiftStatus.DRAFT },
                published = shifts.count { it.status == ShiftStatus.PUBLISHED },
                closed = shifts.count { it.status == ShiftStatus.CLOSED },
                cancelled = shifts.count { it.status == ShiftStatus.CANCELLED }
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isInDemoSession = isDemoSession,
                    rawShifts = shifts,
                    shifts = shifts.map(::toCard),
                    summary = summary,
                    message = if (showMessage) UiMessage("Список смен обновлён", UiMessageKind.INFO) else it.message
                )
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(isLoading = false, isInDemoSession = isDemoSession, message = UiMessage(error.message ?: "Не удалось загрузить смены", UiMessageKind.ERROR))
            }
        }
    }

    private fun postError(text: String) {
        _uiState.update { it.copy(message = UiMessage(text, UiMessageKind.ERROR)) }
    }

    private fun updateForm(update: BusinessShiftFormState.() -> BusinessShiftFormState) {
        _uiState.update { it.copy(form = it.form.update(), message = null) }
    }

    private fun toCard(shift: Shift): BusinessShiftCardUiModel = BusinessShiftCardUiModel(
        id = shift.id,
        title = shift.title,
        dateTimeLabel = "${shift.startAt} → ${shift.endAt}",
        locationLabel = shift.locationId,
        payLabel = "$${"%.2f".format(shift.payRateCents / 100.0)}/ч",
        statusLabel = statusLabel(shift)
    )

    class Factory(
        private val repository: MarketplaceRepository = ApiMarketplaceRepository(),
        private val dispatchers: BusinessDispatchers = BusinessDispatchers()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BusinessViewModel::class.java)) {
                return BusinessViewModel(repository, dispatchers) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
