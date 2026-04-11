package com.povarup.data

import com.povarup.domain.Application
import com.povarup.domain.Assignment
import com.povarup.domain.Payout
import com.povarup.domain.Shift

fun ShiftDto.toDomain(): Shift = Shift(
    id = id,
    businessId = businessId,
    locationId = locationId,
    title = title,
    startAt = startAt,
    endAt = endAt,
    payRateCents = payRateCents,
    status = status,
    productStatus = productStatus ?: status
)

fun ApplicationDto.toDomain(): Application = Application(
    id = id,
    shiftId = shiftId,
    workerId = workerId,
    status = status,
    productStatus = productStatus ?: status
)

fun SessionDto.toDomain(): SessionToken = SessionToken(token = token, userId = userId, role = role)

fun AssignmentDto.toDomain(): Assignment = Assignment(
    id = id,
    shiftId = shiftId,
    workerId = workerId,
    businessId = businessId,
    status = status,
    productStatus = productStatus ?: status,
    escrowLockedCents = escrowLockedCents
)

fun PayoutDto.toDomain(): Payout = Payout(
    id = id,
    assignmentId = assignmentId,
    workerId = workerId,
    amountCents = amountCents,
    status = status,
    note = note
)
