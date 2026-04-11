package com.povarup

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.povarup.core.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val label = TextView(this).apply {
            textSize = 20f
        }
        setContentView(label)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    label.text = when {
                        state.isLoading -> "PovarUp (${state.role})\n${state.baseUrl}\nLoading shifts..."
                        state.errorMessage != null -> "PovarUp (${state.role})\n${state.baseUrl}\nShifts error: ${state.errorMessage}"
                        else -> "PovarUp (${state.role})\n${state.baseUrl}\nShifts: ${state.shiftsCount ?: 0}"
                    }
                }
            }
        }
    }
}
