package com.povarup.data

data class ApiListEnvelope<T>(
    val items: List<T> = emptyList(),
    val error: ApiError? = null
)

data class ApiItemEnvelope<T>(
    val item: T? = null,
    val error: ApiError? = null
)

data class ApiError(
    val code: String,
    val message: String,
    val details: String? = null
)

data class ShiftDto(
    val id: String,
    val businessId: String,
    val locationId: String,
    val title: String,
    val startAt: String,
    val endAt: String,
    val payRateCents: Int,
    val status: String
)

data class CreateSessionRequest(
    val userId: String,
    val role: String
)

data class SessionDto(
    val token: String,
    val userId: String,
    val role: String
)

data class SessionToken(
    val token: String,
    val userId: String,
    val role: String
)
