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
            addView(label)
        }
        setContentView(layout)

        workerButton.setOnClickListener {
            userIdInput.setText("u_worker_1")
            viewModel.setRole("worker")
        }
        businessButton.setOnClickListener {
            userIdInput.setText("u_biz_1")
            viewModel.setRole("business")
        }
        adminButton.setOnClickListener {
            userIdInput.setText("u_admin_1")
            viewModel.setRole("admin")
        }
        createSessionButton.setOnClickListener {
            viewModel.createSessionForRole(userIdInput.text?.toString().orEmpty())
        }
        clearSessionButton.setOnClickListener {
            viewModel.clearSession()
        }
        refreshButton.setOnClickListener {
            viewModel.refreshShifts()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    label.text = buildString {
                        append("PovarUp (").append(state.role).append(")\n")
                        append(state.baseUrl).append("\n")
                        append("Session: ")
                        append(if (state.hasSession) "active" else "none")
                        append(" user=").append(state.sessionUserId ?: "-").append("\n")
                        if (state.sessionStatusMessage != null) append(state.sessionStatusMessage).append("\n")
                        when {
                            state.isLoading -> append("Loading shifts...")
                            state.errorMessage != null -> append("Shifts error: ").append(state.errorMessage)
                            else -> append("Shifts: ").append(state.shiftsCount ?: 0)
                        }
                    }
                }
            }
        }
    }
}
