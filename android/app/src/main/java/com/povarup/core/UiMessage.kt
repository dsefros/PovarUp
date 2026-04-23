package com.povarup.core

enum class UiMessageKind {
    INFO,
    ERROR
}

data class UiMessage(
    val text: String,
    val kind: UiMessageKind
)
