package com.povarup

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.povarup.core.DashboardLoadState
import com.povarup.core.MainUiState
import com.povarup.core.MainViewModel
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

        val refresh = Button(this).apply { text = "Retry / Refresh" }
        val apply = Button(this).apply { text = "Apply" }
        val accept = Button(this).apply { text = "Accept Offer" }
        val checkIn = Button(this).apply { text = "Check In" }
        val checkOut = Button(this).apply { text = "Check Out" }
        val createShift = Button(this).apply { text = "Create Shift" }
        val offer = Button(this).apply { text = "Offer Assignment" }
        val release = Button(this).apply { text = "Release Payout" }
        val clear = Button(this).apply { text = "Clear Session" }

        val header = sectionTitle("PovarUp MVP Dashboard")
        val accountState = bodyText()
        val errorBanner = bodyText()
        val statusBanner = bodyText()
        val loadingBanner = bodyText()
        val eventsBlock = bodyText()
        val availableShiftsBlock = bodyText()
        val applicationsBlock = bodyText()
        val assignmentsBlock = bodyText()
        val activeShiftBlock = bodyText()
        val completedBlock = bodyText()
        val payoutsBlock = bodyText()
        val businessShiftsBlock = bodyText()
        val businessAppsBlock = bodyText()
        val businessAssignmentsBlock = bodyText()
        val businessPayoutBlock = bodyText()

        setContentView(ScrollView(this).apply {
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)
                addView(header)
                addView(accountState)
                addView(loadingBanner)
                addView(errorBanner)
                addView(statusBanner)
                addView(refresh)
                addView(workerDemo)
                addView(businessDemo)
                addView(userId)
                addView(password)
                addView(login)
                addView(clear)

                addView(sectionTitle("Worker actions"))
                addView(shiftId)
                addView(apply)
                addView(assignmentId)
                addView(accept)
                addView(checkIn)
                addView(checkOut)

                addView(sectionTitle("Business actions"))
                addView(shiftTitle)
                addView(locationId)
                addView(payRate)
                addView(createShift)
                addView(appId)
                addView(offer)
                addView(release)

                addView(sectionTitle("Important events"))
                addView(eventsBlock)
                addView(sectionTitle("Worker: available shifts"))
                addView(availableShiftsBlock)
                addView(sectionTitle("Worker: my applications"))
                addView(applicationsBlock)
                addView(sectionTitle("Worker: my assignments"))
                addView(assignmentsBlock)
                addView(sectionTitle("Worker: current active shift"))
                addView(activeShiftBlock)
                addView(sectionTitle("Worker: completed shifts"))
                addView(completedBlock)
                addView(sectionTitle("Worker: payout status"))
                addView(payoutsBlock)

                addView(sectionTitle("Business: owned shifts"))
                addView(businessShiftsBlock)
                addView(sectionTitle("Business: applications for selected shift"))
                addView(businessAppsBlock)
                addView(sectionTitle("Business: assignments status"))
                addView(businessAssignmentsBlock)
                addView(sectionTitle("Business: payout/release status"))
                addView(businessPayoutBlock)
            })
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
                    accountState.text = "Role: ${state.role} user=${state.sessionUserId ?: "-"}"
                    loadingBanner.text = if (state.dashboardState == DashboardLoadState.LOADING) "Loading dashboard..." else ""
                    errorBanner.text = state.errorMessage?.let { "Error: $it" } ?: ""
                    statusBanner.text = state.statusMessage?.let { "Last action: $it" } ?: ""

                    val disableDashboardActions = state.dashboardState == DashboardLoadState.LOADING
                    val isWorker = state.role == "worker"
                    val isBusiness = state.role == "business"
                    val selectedAssignment = state.assignments.firstOrNull { it.id == assignmentId.text.toString() }
                    val selectedShift = state.shifts.firstOrNull { it.id == shiftId.text.toString() }
                    val relatedShiftIds = (state.applications.map { it.shiftId } + state.assignments.map { it.shiftId }).toSet()

                    refresh.isEnabled = !disableDashboardActions
                    login.isEnabled = !viewModel.isActionInFlight("login")
                    apply.isEnabled = isWorker && !viewModel.isActionInFlight("apply:${shiftId.text.toString()}") && selectedShift?.status == "open" && !relatedShiftIds.contains(shiftId.text.toString())
                    accept.isEnabled = isWorker && selectedAssignment?.status == "offered" && !viewModel.isActionInFlight("accept:${assignmentId.text.toString()}")
                    checkIn.isEnabled = isWorker && selectedAssignment?.status == "active" && !viewModel.isActionInFlight("checkin:${assignmentId.text.toString()}")
                    checkOut.isEnabled = isWorker && selectedAssignment?.status == "in_progress" && !viewModel.isActionInFlight("checkout:${assignmentId.text.toString()}")
                    createShift.isEnabled = isBusiness && !viewModel.isActionInFlight("create_shift")
                    offer.isEnabled = isBusiness && appId.text.isNotBlank() && !viewModel.isActionInFlight("offer:${appId.text.toString()}")
                    release.isEnabled = isBusiness && selectedAssignment?.status in setOf("completed_pending_rating", "completed_rated") && !viewModel.isActionInFlight("release:${assignmentId.text.toString()}")

                    eventsBlock.text = if (state.importantEvents.isEmpty()) "No important events yet." else state.importantEvents.joinToString("\n") { "• $it" }
                    renderWorkerSections(state, availableShiftsBlock, applicationsBlock, assignmentsBlock, activeShiftBlock, completedBlock, payoutsBlock)
                    renderBusinessSections(state, businessShiftsBlock, businessAppsBlock, businessAssignmentsBlock, businessPayoutBlock)

                    if (shiftId.text.isBlank() && !state.selectedShiftId.isNullOrBlank()) shiftId.setText(state.selectedShiftId)
                    if (assignmentId.text.isBlank() && !state.selectedAssignmentId.isNullOrBlank()) assignmentId.setText(state.selectedAssignmentId)
                    if (appId.text.isBlank() && state.applications.firstOrNull()?.id != null) appId.setText(state.applications.first().id)
                }
            }
        }
    }

    private fun renderWorkerSections(
        state: MainUiState,
        availableShiftsBlock: TextView,
        applicationsBlock: TextView,
        assignmentsBlock: TextView,
        activeShiftBlock: TextView,
        completedBlock: TextView,
        payoutsBlock: TextView
    ) {
        val appliedShiftIds = state.applications.map { it.shiftId }.toSet()
        val assignedShiftIds = state.assignments.map { it.shiftId }.toSet()
        val availableShifts = state.shifts.filter { it.status == "open" && !appliedShiftIds.contains(it.id) && !assignedShiftIds.contains(it.id) }
        availableShiftsBlock.text = if (state.role != "worker") "Sign in as a worker to view this section."
        else if (availableShifts.isEmpty()) "No available shifts right now."
        else availableShifts.joinToString("\n") { "• ${it.title} (${it.id}) @ ${it.startAt}" }

        applicationsBlock.text = if (state.role != "worker") "Sign in as a worker to view this section."
        else if (state.applications.isEmpty()) "No applications yet."
        else state.applications.joinToString("\n") { "• ${it.id}: shift=${it.shiftId} status=${it.status}" }

        assignmentsBlock.text = if (state.role != "worker") "Sign in as a worker to view this section."
        else if (state.assignments.isEmpty()) "No assignments yet."
        else state.assignments.joinToString("\n") { "• ${it.id}: shift=${it.shiftId} status=${it.status}" }

        val shiftById = state.shifts.associateBy { it.id }
        val activeAssignment = state.assignments.firstOrNull { it.status == "active" || it.status == "in_progress" }
        activeShiftBlock.text = when {
            state.role != "worker" -> "Sign in as a worker to view this section."
            activeAssignment == null -> "No active shift."
            else -> {
                val shift = shiftById[activeAssignment.shiftId]
                "${shift?.title ?: activeAssignment.shiftId} (${activeAssignment.status})"
            }
        }

        val completedAssignments = state.assignments.filter { it.status == "completed_pending_rating" || it.status == "completed_rated" }
        completedBlock.text = if (state.role != "worker") "Sign in as a worker to view this section."
        else if (completedAssignments.isEmpty()) "No completed shifts yet."
        else completedAssignments.joinToString("\n") { assignment ->
            val shift = shiftById[assignment.shiftId]
            "• ${shift?.title ?: assignment.shiftId}: ${assignment.status}"
        }

        payoutsBlock.text = if (state.role != "worker") "Sign in as a worker to view this section."
        else if (state.payouts.isEmpty()) "No payouts yet."
        else state.payouts.joinToString("\n") { "• ${it.id}: assignment=${it.assignmentId} amount=${it.amountCents} status=${it.status}" }
    }

    private fun renderBusinessSections(
        state: MainUiState,
        businessShiftsBlock: TextView,
        businessAppsBlock: TextView,
        businessAssignmentsBlock: TextView,
        businessPayoutBlock: TextView
    ) {
        businessShiftsBlock.text = if (state.role != "business") "Sign in as a business user to view this section."
        else if (state.shifts.isEmpty()) "No owned shifts yet."
        else state.shifts.joinToString("\n") { "• ${it.title} (${it.id}) status=${it.status}" }

        businessAppsBlock.text = if (state.role != "business") "Sign in as a business user to view this section."
        else if (state.selectedShiftId == null) "Select or enter a shift ID to view applications."
        else if (state.applications.isEmpty()) "No applications yet for selected shift."
        else state.applications.joinToString("\n") { "• ${it.id}: worker=${it.workerId} status=${it.status}" }

        businessAssignmentsBlock.text = if (state.role != "business") "Sign in as a business user to view this section."
        else if (state.assignments.isEmpty()) "No assignments yet."
        else state.assignments.joinToString("\n") { "• ${it.id}: worker=${it.workerId} status=${it.status}" }

        val releaseReady = state.assignments.filter { it.status == "completed_pending_rating" || it.status == "completed_rated" }
        businessPayoutBlock.text = when {
            state.role != "business" -> "Sign in as a business user to view this section."
            releaseReady.isEmpty() -> "No completed assignments ready for payout release."
            else -> releaseReady.joinToString("\n") {
                "• ${it.id}: ready to release payout (business payout status is not exposed by API)"
            }
        }
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 16f
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.START
    }

    private fun bodyText() = TextView(this).apply { textSize = 14f }
}
