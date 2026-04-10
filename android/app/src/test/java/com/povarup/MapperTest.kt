package com.povarup

import com.povarup.data.ShiftDto
import com.povarup.data.toDomain
import org.junit.Assert.assertEquals
import org.junit.Test

class MapperTest {
    @Test
    fun mapShiftDtoToDomain() {
        val dto = ShiftDto(
            id = "shift_1",
            businessId = "biz_1",
            locationId = "loc_1",
            title = "Line Cook",
            startAt = "2026-04-08T10:00:00Z",
            endAt = "2026-04-08T14:00:00Z",
            payRateCents = 2500,
            status = "open"
        )

        val model = dto.toDomain()
        assertEquals("biz_1", model.businessId)
        assertEquals(2500, model.payRateCents)
        assertEquals("open", model.status)
    }
}
