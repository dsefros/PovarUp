package com.povarup

import com.povarup.core.BusinessDemoViewModel
import com.povarup.domain.DishwasherZone
import com.povarup.domain.WorkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessDemoViewModelTest {
    @Test
    fun createsCookShiftWithStructuredFields() {
        val vm = BusinessDemoViewModel()
        vm.enterDemoBusiness()
        vm.onTitleChanged("Cook Shift")
        vm.onLocationChanged("Center")
        vm.onStartAtChanged("2026-05-10 10:00")
        vm.onEndAtChanged("2026-05-10 18:00")
        vm.onPayChanged("30")
        vm.onWorkTypeChanged(WorkType.COOK)
        vm.onBanquetChanged(true)
        vm.createShift()

        val shift = vm.uiState.value.shifts.first()
        vm.openShift(shift.id)
        val selected = vm.uiState.value.selectedShift!!
        assertEquals(WorkType.COOK, selected.workType)
        assertTrue(selected.isBanquet == true)
        assertEquals("Cook Shift", selected.title)
    }

    @Test
    fun createsDishwasherShiftWithoutBanquetField() {
        val vm = BusinessDemoViewModel()
        vm.enterDemoBusiness()
        vm.onTitleChanged("Dish Shift")
        vm.onLocationChanged("Center")
        vm.onStartAtChanged("2026-05-10 10:00")
        vm.onEndAtChanged("2026-05-10 18:00")
        vm.onPayChanged("19")
        vm.onWorkTypeChanged(WorkType.DISHWASHER)
        vm.onDishwasherZoneChanged(DishwasherZone.BLACK)
        vm.createShift()

        val shiftId = vm.uiState.value.shifts.first().id
        vm.openShift(shiftId)
        val selected = vm.uiState.value.selectedShift!!
        assertEquals(WorkType.DISHWASHER, selected.workType)
        assertEquals(DishwasherZone.BLACK, selected.dishwasherZone)
        assertNull(selected.isBanquet)
    }
}
