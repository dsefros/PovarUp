package com.povarup.core

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class RootViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val mode: StateFlow<RootEntryMode> = savedStateHandle.getStateFlow(KEY_MODE, RootEntryMode.WELCOME)

    fun setMode(value: RootEntryMode) {
        savedStateHandle[KEY_MODE] = value
    }

    companion object {
        private const val KEY_MODE = "root_entry_mode"
    }
}
