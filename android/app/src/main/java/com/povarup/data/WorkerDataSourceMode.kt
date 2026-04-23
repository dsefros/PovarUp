package com.povarup.data

enum class WorkerDataSourceMode {
    REAL,
    DEMO
}

interface WorkerModeSelectable {
    fun selectMode(mode: WorkerDataSourceMode)
}
