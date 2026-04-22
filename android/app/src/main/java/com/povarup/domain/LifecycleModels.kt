package com.povarup.domain

enum class UserRole {
    WORKER,
    BUSINESS,
    ADMIN,
    UNKNOWN;

    companion object {
        fun from(raw: String?): UserRole = when (raw?.lowercase()) {
            "worker" -> WORKER
            "business" -> BUSINESS
            "admin" -> ADMIN
            else -> UNKNOWN
        }
    }

    fun asApiValue(): String = name.lowercase()
}

enum class ShiftStatus {
    DRAFT,
    PUBLISHED,
    FILLED,
    CLOSED,
    CANCELLED,
    UNKNOWN;

    companion object {
        fun from(raw: String?): ShiftStatus = when (raw?.lowercase()) {
            "draft" -> DRAFT
            "published" -> PUBLISHED
            "filled" -> FILLED
            "closed" -> CLOSED
            "cancelled" -> CANCELLED
            else -> UNKNOWN
        }
    }
}

enum class ApplicationStatus {
    APPLIED,
    ACCEPTED,
    REJECTED,
    WITHDRAWN,
    UNKNOWN;

    companion object {
        fun from(raw: String?): ApplicationStatus = when (raw?.lowercase()) {
            "applied" -> APPLIED
            "accepted" -> ACCEPTED
            "rejected" -> REJECTED
            "withdrawn" -> WITHDRAWN
            else -> UNKNOWN
        }
    }
}

enum class AssignmentStatus {
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    PAID,
    CANCELLED,
    UNKNOWN;

    companion object {
        fun from(raw: String?): AssignmentStatus = when (raw?.lowercase()) {
            "assigned" -> ASSIGNED
            "in_progress" -> IN_PROGRESS
            "completed" -> COMPLETED
            "paid" -> PAID
            "cancelled" -> CANCELLED
            else -> UNKNOWN
        }
    }
}

enum class PayoutStatus {
    CREATED,
    PENDING,
    PAID,
    FAILED,
    UNKNOWN;

    companion object {
        fun from(raw: String?): PayoutStatus = when (raw?.lowercase()) {
            "created" -> CREATED
            "pending" -> PENDING
            "paid" -> PAID
            "failed" -> FAILED
            "released" -> PAID
            else -> UNKNOWN
        }
    }
}

data class ShiftCapability(
    val canApply: Boolean,
    val canPublish: Boolean,
    val canClose: Boolean,
    val canCancel: Boolean
)

data class ApplicationCapability(
    val canWithdraw: Boolean,
    val canReject: Boolean
)

data class AssignmentCapability(
    val canCheckIn: Boolean,
    val canCheckOut: Boolean,
    val canCancel: Boolean,
    val canReleasePayout: Boolean
)

fun Shift.capability(role: UserRole, alreadyRelated: Boolean): ShiftCapability = ShiftCapability(
    canApply = role == UserRole.WORKER && status == ShiftStatus.PUBLISHED && !alreadyRelated,
    canPublish = role == UserRole.BUSINESS && status == ShiftStatus.DRAFT,
    canClose = role == UserRole.BUSINESS && status == ShiftStatus.PUBLISHED,
    canCancel = role == UserRole.BUSINESS && status in setOf(ShiftStatus.DRAFT, ShiftStatus.PUBLISHED, ShiftStatus.CLOSED)
)

fun Application.capability(role: UserRole): ApplicationCapability = ApplicationCapability(
    canWithdraw = role == UserRole.WORKER && status in setOf(ApplicationStatus.APPLIED, ApplicationStatus.ACCEPTED),
    canReject = role == UserRole.BUSINESS && status == ApplicationStatus.APPLIED
)

fun Assignment.capability(role: UserRole): AssignmentCapability = AssignmentCapability(
    canCheckIn = role == UserRole.WORKER && status == AssignmentStatus.ASSIGNED,
    canCheckOut = role == UserRole.WORKER && status == AssignmentStatus.IN_PROGRESS,
    canCancel = role == UserRole.BUSINESS && status in setOf(AssignmentStatus.ASSIGNED, AssignmentStatus.IN_PROGRESS),
    canReleasePayout = role == UserRole.BUSINESS && status == AssignmentStatus.COMPLETED
)
