package com.povarup

import com.povarup.data.ApplicationDto
import com.povarup.data.PayoutDto
import com.povarup.data.SessionDto
import com.povarup.data.ShiftDto
import com.povarup.data.toDomain
import com.povarup.domain.ApplicationStatus
import com.povarup.domain.PayoutStatus
import com.povarup.domain.ShiftStatus
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
            status = "published"
        )

        val model = dto.toDomain()
        assertEquals("biz_1", model.businessId)
        assertEquals(2500, model.payRateCents)
        assertEquals(ShiftStatus.PUBLISHED, model.status)
        assertEquals("published", model.rawStatus)
    }

    @Test
    fun mapSessionDtoToDomain() {
        val dto = SessionDto(token = "sess_1", userId = "worker.demo", role = "worker")
        val model = dto.toDomain()
        assertEquals("sess_1", model.token)
        assertEquals("worker.demo", model.userId)
        assertEquals("worker", model.role)
    }

    @Test
    fun mapApplicationDtoToDomain() {
        val dto = ApplicationDto(id = "app_1", shiftId = "shift_1", workerId = "worker_1", status = "applied")
        val model = dto.toDomain()
        assertEquals("app_1", model.id)
        assertEquals("shift_1", model.shiftId)
        assertEquals(ApplicationStatus.APPLIED, model.status)
    }

    @Test
    fun mapPayoutDtoToDomainWithCurrentLifecycleStatus() {
        val dto = PayoutDto(
            id = "pay_1",
            assignmentId = "asn_1",
            workerId = "worker_1",
            amountCents = 1200,
            status = "pending",
            createdAt = "2026-04-08T10:00:00Z"
        )

        val model = dto.toDomain()
        assertEquals(PayoutStatus.PENDING, model.status)
        assertEquals("pending", model.rawStatus)
    }

    @Test
    fun mapPayoutDtoToDomainMapsLegacyReleasedToPaid() {
        val dto = PayoutDto(
            id = "pay_legacy",
            assignmentId = "asn_legacy",
            workerId = "worker_1",
            amountCents = 1200,
            status = "released",
            createdAt = "2026-04-08T10:00:00Z"
        )

        val model = dto.toDomain()
        assertEquals(PayoutStatus.PAID, model.status)
        assertEquals("released", model.rawStatus)
    }
}
