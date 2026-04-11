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
    val status: String,
    val normalizedStatus: String? = null,
    val productStatus: String? = null
)

data class CreateApplicationRequest(val shiftId: String)

data class ApplicationDto(
    val id: String,
    val shiftId: String,
    val workerId: String,
    val status: String,
    val normalizedStatus: String? = null,
    val productStatus: String? = null
)

data class LoginRequest(val userId: String, val password: String)

data class SessionDto(val token: String, val userId: String, val role: String, val displayName: String? = null, val expiresAt: String? = null)

data class SessionToken(val token: String, val userId: String, val role: String)

data class AssignmentDto(
    val id: String,
    val shiftId: String,
    val workerId: String,
    val businessId: String,
    val status: String,
    val normalizedStatus: String? = null,
    val productStatus: String? = null,
    val escrowLockedCents: Int
)

data class CreateShiftRequest(
    val locationId: String,
    val title: String,
    val startAt: String,
    val endAt: String,
    val payRateCents: Int
)

data class OfferAssignmentRequest(val applicationId: String)
data class AttendanceRequest(val assignmentId: String)
data class ReleasePayoutRequest(val force: Boolean = false)

data class PayoutDto(
    val id: String,
    val assignmentId: String,
    val workerId: String,
    val amountCents: Int,
    val status: String,
    val internalStatus: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val note: String? = null
)

data class UpdatePayoutStatusRequest(val status: String, val note: String? = null)

data class ProblemCasesDto(
    val flags: List<Map<String, Any?>> = emptyList(),
    val failedPayouts: List<PayoutDto> = emptyList(),
    val stalledAssignments: List<AssignmentDto> = emptyList()
)
