package com.povarup.domain

enum class WorkType {
    COOK,
    WAITER,
    BARTENDER,
    DISHWASHER
}

enum class CookCuisine {
    RUSSIAN,
    FRENCH,
    ITALIAN
}

enum class CookStation {
    HOT,
    COLD,
    UNIVERSAL
}

enum class DishwasherZone {
    WHITE,
    BLACK
}

data class Shift(
    val id: String,
    val businessId: String,
    val locationId: String,
    val title: String,
    val startAt: String,
    val endAt: String,
    val payRateCents: Int,
    val status: ShiftStatus,
    val rawStatus: String,
    val workType: WorkType? = null,
    val cookCuisine: CookCuisine? = null,
    val cookStation: CookStation? = null,
    val isBanquet: Boolean? = null,
    val dishwasherZone: DishwasherZone? = null
)

data class Application(
    val id: String,
    val shiftId: String,
    val workerId: String,
    val status: ApplicationStatus,
    val rawStatus: String
)

data class Assignment(
    val id: String,
    val shiftId: String,
    val workerId: String,
    val businessId: String,
    val status: AssignmentStatus,
    val rawStatus: String,
    val escrowLockedCents: Int
)

data class Payout(
    val id: String,
    val assignmentId: String,
    val workerId: String,
    val amountCents: Int,
    val status: PayoutStatus,
    val rawStatus: String,
    val note: String? = null
)
