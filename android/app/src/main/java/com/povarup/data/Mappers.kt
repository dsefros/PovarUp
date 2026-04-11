package com.povarup.data

import com.povarup.domain.Shift

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
