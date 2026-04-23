package com.povarup

import com.povarup.domain.CookCuisine
import com.povarup.domain.CookStation
import com.povarup.domain.DishwasherZone
import com.povarup.domain.Shift
import com.povarup.domain.ShiftStatus
import com.povarup.domain.WorkType
import com.povarup.domain.workTypeDescription
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkTypeFormattingTest {
    @Test
    fun cookFormattingIncludesCuisineStationAndBanquet() {
        val shift = Shift(
            id = "s1",
            businessId = "b1",
            locationId = "l1",
            title = "Cook",
            startAt = "",
            endAt = "",
            payRateCents = 1000,
            status = ShiftStatus.PUBLISHED,
            rawStatus = "published",
            workType = WorkType.COOK,
            cookCuisine = CookCuisine.RUSSIAN,
            cookStation = CookStation.HOT,
            isBanquet = true
        )

        assertEquals("Cook · Russian cuisine · Hot station · Banquet", shift.workTypeDescription())
    }

    @Test
    fun dishwasherFormattingIncludesZone() {
        val shift = Shift(
            id = "s2",
            businessId = "b1",
            locationId = "l1",
            title = "Dishwasher",
            startAt = "",
            endAt = "",
            payRateCents = 1000,
            status = ShiftStatus.PUBLISHED,
            rawStatus = "published",
            workType = WorkType.DISHWASHER,
            dishwasherZone = DishwasherZone.BLACK
        )

        assertEquals("Dishwasher · Black zone", shift.workTypeDescription())
    }
}
