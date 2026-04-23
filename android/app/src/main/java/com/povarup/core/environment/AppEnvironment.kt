package com.povarup.core.environment

import androidx.compose.runtime.compositionLocalOf
import com.povarup.core.capabilities.CapabilitySet

enum class AppMode { DEMO, PRODUCTION }

sealed class AppRole(val label: String) {
    data object Worker : AppRole("Worker")
    data object Business : AppRole("Business")
}

data class AppEnvironment(
    val mode: AppMode,
    val role: AppRole,
    val capabilities: CapabilitySet,
    val scenarioId: String? = null
)

val LocalAppEnvironment = compositionLocalOf {
    AppEnvironment(
        mode = AppMode.DEMO,
        role = AppRole.Worker,
        capabilities = CapabilitySet.allEnabled(),
        scenarioId = "worker_browsing"
    )
}
