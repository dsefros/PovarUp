package com.povarup.data

import com.povarup.domain.CookCuisine
import com.povarup.domain.CookStation
import com.povarup.domain.DishwasherZone
import com.povarup.domain.Shift
import com.povarup.domain.ShiftStatus
import com.povarup.domain.WorkType

class DemoBusinessRepository {
    private var currentSession: DemoBusinessSession? = null
    private val shifts = mutableListOf<Shift>()

    fun currentSession(): DemoBusinessSession? = currentSession

    fun enterDemoBusiness(userId: String = DEFAULT_BUSINESS_USER_ID): DemoBusinessSession {
        return DemoBusinessSession(userId = userId).also { currentSession = it }
    }

    fun createShift(input: CreateDemoBusinessShiftRequest): Result<Shift> {
        val session = currentSession ?: return Result.failure(IllegalStateException("Not in demo business session"))
        if (input.title.isBlank()) return Result.failure(IllegalArgumentException("Title is required"))
        if (input.locationId.isBlank()) return Result.failure(IllegalArgumentException("Location is required"))
        if (input.startAt.isBlank() || input.endAt.isBlank()) return Result.failure(IllegalArgumentException("Start and end are required"))
        if (input.payRateCents <= 0) return Result.failure(IllegalArgumentException("Pay must be positive"))

        val shift = Shift(
            id = "demo-business-shift-${shifts.size + 1}",
            businessId = session.userId,
            locationId = input.locationId,
            title = input.title,
            startAt = input.startAt,
            endAt = input.endAt,
            payRateCents = input.payRateCents,
            status = ShiftStatus.DRAFT,
            rawStatus = "draft",
            workType = input.workType,
            cookCuisine = input.cookCuisine,
            cookStation = input.cookStation,
            isBanquet = input.isBanquet,
            dishwasherZone = input.dishwasherZone
        )
        shifts.add(0, shift)
        return Result.success(shift)
    }

    fun listShifts(): Result<List<Shift>> {
        if (currentSession == null) return Result.failure(IllegalStateException("Not in demo business session"))
        return Result.success(shifts.toList())
    }

    fun getShift(shiftId: String): Result<Shift> {
        if (currentSession == null) return Result.failure(IllegalStateException("Not in demo business session"))
        return shifts.firstOrNull { it.id == shiftId }
            ?.let { Result.success(it) }
            ?: Result.failure(IllegalArgumentException("Shift not found"))
    }

    companion object {
        const val DEFAULT_BUSINESS_USER_ID = "business.demo"
    }
}

data class DemoBusinessSession(
    val userId: String
)

data class CreateDemoBusinessShiftRequest(
    val title: String,
    val locationId: String,
    val startAt: String,
    val endAt: String,
    val payRateCents: Int,
    val workType: WorkType,
    val cookCuisine: CookCuisine? = null,
    val cookStation: CookStation? = null,
    val isBanquet: Boolean? = null,
    val dishwasherZone: DishwasherZone? = null
)
