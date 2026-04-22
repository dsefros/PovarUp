package com.povarup.data

import com.povarup.domain.Application
import com.povarup.domain.ApplicationStatus
import com.povarup.domain.Assignment
import com.povarup.domain.AssignmentStatus
import com.povarup.domain.Payout
import com.povarup.domain.PayoutStatus
import com.povarup.domain.Shift
import com.povarup.domain.ShiftStatus

fun ShiftDto.toDomain(): Shift {
    val lifecycleStatus = productStatus ?: normalizedStatus ?: status
    return Shift(
        id = id,
        businessId = businessId,
        locationId = locationId,
        title = title,
        startAt = startAt,
        endAt = endAt,
        payRateCents = payRateCents,
        status = ShiftStatus.from(lifecycleStatus),
        rawStatus = lifecycleStatus
    )
}

fun ApplicationDto.toDomain(): Application {
    val lifecycleStatus = productStatus ?: normalizedStatus ?: status
    return Application(
        id = id,
        shiftId = shiftId,
        workerId = workerId,
        status = ApplicationStatus.from(lifecycleStatus),
        rawStatus = lifecycleStatus
    )
}

fun SessionDto.toDomain(): SessionToken = SessionToken(token = token, userId = userId, role = role)

fun AssignmentDto.toDomain(): Assignment {
    val lifecycleStatus = productStatus ?: normalizedStatus ?: status
    return Assignment(
        id = id,
        shiftId = shiftId,
        workerId = workerId,
        businessId = businessId,
        status = AssignmentStatus.from(lifecycleStatus),
        rawStatus = lifecycleStatus,
        escrowLockedCents = escrowLockedCents
    )
}

fun PayoutDto.toDomain(): Payout {
    val lifecycleStatus = internalStatus ?: status
    return Payout(
        id = id,
        assignmentId = assignmentId,
        workerId = workerId,
        amountCents = amountCents,
        status = PayoutStatus.from(lifecycleStatus),
        rawStatus = lifecycleStatus,
        note = note
    )
}
