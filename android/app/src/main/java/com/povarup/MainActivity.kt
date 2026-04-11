package com.povarup

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.povarup.core.MainViewModel
import com.povarup.core.MainUiState
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.SharedPreferencesSessionStore
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            repository = ApiMarketplaceRepository(
                sessionStore = SharedPreferencesSessionStore(applicationContext)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val workerButton = Button(this).apply { text = "Worker" }
        val businessButton = Button(this).apply { text = "Business" }
        val adminButton = Button(this).apply { text = "Admin" }
        val userIdInput = EditText(this).apply {
            hint = "user id"
            setText("u_worker_1")
        }
        val createSessionButton = Button(this).apply { text = "Create Session" }
        val clearSessionButton = Button(this).apply { text = "Clear Session" }
        val refreshButton = Button(this).apply { text = "Refresh Shifts" }
        val shiftIdInput = EditText(this).apply { hint = "shift id" }
        val applyButton = Button(this).apply { text = "Apply to Shift" }
        val label = TextView(this).apply { textSize = 16f }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            addView(workerButton)
            addView(businessButton)
            addView(adminButton)
            addView(userIdInput)
            addView(createSessionButton)
            addView(clearSessionButton)
            addView(refreshButton)
            addView(shiftIdInput)
            addView(applyButton)
            addView(label)
        }
        setContentView(layout)

        bindRoleButton(workerButton, userIdInput, role = "worker", defaultUserId = "u_worker_1")
        bindRoleButton(businessButton, userIdInput, role = "business", defaultUserId = "u_biz_1")
        bindRoleButton(adminButton, userIdInput, role = "admin", defaultUserId = "u_admin_1")
        createSessionButton.setOnClickListener {
            viewModel.createSessionForRole(userIdInput.text?.toString().orEmpty())
        }
        clearSessionButton.setOnClickListener {
            viewModel.clearSession()
        }
        refreshButton.setOnClickListener {
            viewModel.refreshShifts()
        }
        applyButton.setOnClickListener {
            viewModel.applyToShift(shiftIdInput.text?.toString().orEmpty())
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    label.text = renderStateText(state)
                    if (state.selectedShiftId != null && shiftIdInput.text?.toString() != state.selectedShiftId) {
                        shiftIdInput.setText(state.selectedShiftId)
                    }
                }
            }
        }
    }

    private fun bindRoleButton(button: Button, userIdInput: EditText, role: String, defaultUserId: String) {
        button.setOnClickListener {
            userIdInput.setText(defaultUserId)
            viewModel.setRole(role)
        }
    }

    private fun renderStateText(state: MainUiState): String = buildString {
        append("PovarUp (").append(state.role).append(")\n")
        append(state.baseUrl).append("\n")
        append("Session: ")
        append(if (state.hasSession) "active" else "none")
        append(" user=").append(state.sessionUserId ?: "-").append("\n")
        state.sessionStatusMessage?.let { append(it).append("\n") }
        state.applyStatusMessage?.let { append(it).append("\n") }

        when {
            state.isLoading -> append("Loading shifts...")
            state.errorMessage != null -> append("Shifts error: ").append(state.errorMessage)
            else -> {
                append("Shifts: ").append(state.shifts.size).append("\n")
                state.shifts.forEach { shift ->
                    append("- ").append(shift.id).append(": ").append(shift.title).append("\n")
                }
            }
        }
    }
}
