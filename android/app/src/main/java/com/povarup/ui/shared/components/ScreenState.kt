package com.povarup.ui.shared.components

sealed interface ScreenState<out T> {
    data object Loading : ScreenState<Nothing>
    data class Success<T>(val data: T) : ScreenState<T>
    data class Error(val message: String) : ScreenState<Nothing>
}
