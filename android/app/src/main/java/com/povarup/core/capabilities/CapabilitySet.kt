package com.povarup.core.capabilities

data class CapabilitySet(
    val canApply: Boolean,
    val canCheckIn: Boolean,
    val canCheckOut: Boolean,
    val canAssign: Boolean,
    val canReject: Boolean,
    val canCreateShift: Boolean,
    val canPublishShift: Boolean,
    val canReleasePayout: Boolean
) {
    companion object {
        fun allEnabled() = CapabilitySet(true, true, true, true, true, true, true, true)
        fun workerDefaults() = CapabilitySet(
            canApply = true,
            canCheckIn = true,
            canCheckOut = true,
            canAssign = false,
            canReject = false,
            canCreateShift = false,
            canPublishShift = false,
            canReleasePayout = false
        )

        fun businessDefaults() = CapabilitySet(
            canApply = false,
            canCheckIn = false,
            canCheckOut = false,
            canAssign = true,
            canReject = true,
            canCreateShift = true,
            canPublishShift = true,
            canReleasePayout = true
        )
    }
}
