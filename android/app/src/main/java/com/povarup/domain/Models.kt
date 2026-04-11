package com.povarup.domain

data class Shift(
    val id: String,
    val businessId: String,
    val locationId: String,
    val title: String,
    val startAt: String,
    val endAt: String,
    val payRateCents: Int,
    val status: String
)

data class Application(
    val id: String,
    val shiftId: String,
    val workerId: String,
    val status: String
)
