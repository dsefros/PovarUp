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
        MainViewModel.Factory(repository = ApiMarketplaceRepository(sessionStore = SharedPreferencesSessionStore(applicationContext)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = EditText(this).apply { hint = "user id"; setText("worker.demo") }
        val password = EditText(this).apply { hint = "password"; setText("workerpass") }
        val login = Button(this).apply { text = "Login" }
        val workerDemo = Button(this).apply { text = "Use Worker Demo" }
        val businessDemo = Button(this).apply { text = "Use Business Demo" }

        val shiftId = EditText(this).apply { hint = "shift id" }
        val assignmentId = EditText(this).apply { hint = "assignment id" }
        val appId = EditText(this).apply { hint = "application id (business offer)" }
        val shiftTitle = EditText(this).apply { hint = "new shift title"; setText("Prep Cook") }
        val locationId = EditText(this).apply { hint = "location id"; setText("loc_1") }
        val payRate = EditText(this).apply { hint = "pay cents/hour"; setText("2200") }

        val refresh = Button(this).apply { text = "Refresh" }
        val apply = Button(this).apply { text = "Apply" }
        val accept = Button(this).apply { text = "Accept Offer" }
        val checkIn = Button(this).apply { text = "Check In" }
        val checkOut = Button(this).apply { text = "Check Out" }
        val createShift = Button(this).apply { text = "Create Shift" }
        val offer = Button(this).apply { text = "Offer Assignment" }
        val release = Button(this).apply { text = "Release Payout" }
        val clear = Button(this).apply { text = "Clear Session" }
        val label = TextView(this).apply { textSize = 14f }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            listOf(userId, password, login, workerDemo, businessDemo, refresh, shiftId, apply, assignmentId, accept, checkIn, checkOut,
                shiftTitle, locationId, payRate, createShift, appId, offer, release, clear, label).forEach { addView(it) }
        })

        workerDemo.setOnClickListener { userId.setText("worker.demo"); password.setText("workerpass") }
        businessDemo.setOnClickListener { userId.setText("business.demo"); password.setText("businesspass") }
        login.setOnClickListener { viewModel.login(userId.text.toString(), password.text.toString()) }
        refresh.setOnClickListener { viewModel.setSelectedShift(shiftId.text.toString()); viewModel.loadDashboard() }
        apply.setOnClickListener { viewModel.applyToShift(shiftId.text.toString()) }
        accept.setOnClickListener { viewModel.acceptAssignment(assignmentId.text.toString()) }
        checkIn.setOnClickListener { viewModel.checkIn(assignmentId.text.toString()) }
        checkOut.setOnClickListener { viewModel.checkOut(assignmentId.text.toString()) }
        createShift.setOnClickListener { viewModel.createShift(shiftTitle.text.toString(), locationId.text.toString(), payRate.text.toString().toIntOrNull() ?: 2200) }
        offer.setOnClickListener { viewModel.offerAssignment(appId.text.toString()) }
        release.setOnClickListener { viewModel.releasePayout(assignmentId.text.toString()) }
        clear.setOnClickListener { viewModel.clearSession() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    label.text = renderStateText(state)
                    if (state.selectedShiftId != null && shiftId.text.toString() != state.selectedShiftId) shiftId.setText(state.selectedShiftId)
                    if (state.selectedAssignmentId != null && assignmentId.text.toString() != state.selectedAssignmentId) assignmentId.setText(state.selectedAssignmentId)
                    val firstAppId = state.applications.firstOrNull()?.id
                    if (firstAppId != null && appId.text.toString() != firstAppId) appId.setText(firstAppId)
                }
            }
        }
    }

    private fun renderStateText(state: MainUiState): String = buildString {
        append("Role: ${state.role} user=${state.sessionUserId ?: "-"}\n")
        state.statusMessage?.let { append("✅ $it\n") }
        state.errorMessage?.let { append("❌ $it\n") }
        append("Shifts (${state.shifts.size}):\n")
        state.shifts.forEach { append("- ${it.id}: ${it.title} (${it.status})\n") }
        append("Applications (${state.applications.size}):\n")
        state.applications.forEach { append("- ${it.id}: shift=${it.shiftId} status=${it.status}\n") }
        append("Assignments (${state.assignments.size}):\n")
        state.assignments.forEach { append("- ${it.id}: status=${it.status} worker=${it.workerId}\n") }
    }
}
