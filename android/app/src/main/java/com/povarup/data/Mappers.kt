package com.povarup.data

import com.povarup.domain.Shift
import com.povarup.domain.Application

fun ShiftDto.toDomain(): Shift = Shift(
    id = id,
    businessId = businessId,
    locationId = locationId,
    title = title,
    startAt = startAt,
    endAt = endAt,
    payRateCents = payRateCents,
    status = status
)

fun SessionDto.toDomain(): SessionToken = SessionToken(
    token = token,
    userId = userId,
    role = role
)

fun ApplicationDto.toDomain(): Application = Application(
    id = id,
    shiftId = shiftId,
    workerId = workerId,
    status = status
)
