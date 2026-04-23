package com.povarup.core

enum class RootEntryMode {
    WELCOME,
    WORKER,
    DEMO_BUSINESS
}

enum class RootContent {
    WELCOME,
    WORKER_SHIFTS,
    DEMO_BUSINESS
}

fun resolveRootContent(
    isWorkerLoggedIn: Boolean,
    isBusinessDemoSessionActive: Boolean,
    mode: RootEntryMode
): RootContent = when {
    isWorkerLoggedIn -> RootContent.WORKER_SHIFTS
    mode == RootEntryMode.DEMO_BUSINESS && isBusinessDemoSessionActive -> RootContent.DEMO_BUSINESS
    else -> RootContent.WELCOME
}
