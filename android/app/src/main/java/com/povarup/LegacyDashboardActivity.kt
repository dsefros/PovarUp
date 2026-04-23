package com.povarup

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.povarup.core.HomeState
import com.povarup.core.MainAction
import com.povarup.core.MainUiState
import com.povarup.core.MainViewModel
import com.povarup.core.UiLoadState
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.SharedPreferencesSessionStore
import kotlinx.coroutines.launch

class LegacyDashboardActivity : AppCompatActivity() {
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
        val adminDemo = Button(this).apply { text = "Use Admin Demo" }
        val shiftId = EditText(this).apply { hint = "shift id" }
        val assignmentId = EditText(this).apply { hint = "assignment id" }
        val appId = EditText(this).apply { hint = "application id (business offer)" }
        val shiftTitle = EditText(this).apply { hint = "new shift title"; setText("Prep Cook") }
        val locationId = EditText(this).apply { hint = "location id"; setText("loc_1") }
        val payRate = EditText(this).apply { hint = "pay cents/hour"; setText("2200") }
        val refresh = Button(this).apply { text = "Retry / Refresh" }
        val apply = Button(this).apply { text = "Apply" }
        val checkIn = Button(this).apply { text = "Check In" }
        val checkOut = Button(this).apply { text = "Check Out" }
        val createShift = Button(this).apply { text = "Create Shift" }
        val publishShift = Button(this).apply { text = "Publish Shift" }
        val offer = Button(this).apply { text = "Offer Assignment" }
        val reject = Button(this).apply { text = "Reject Application" }
        val withdraw = Button(this).apply { text = "Withdraw Application" }
        val closeShift = Button(this).apply { text = "Close Shift" }
        val cancelShift = Button(this).apply { text = "Cancel Shift" }
        val cancelAssignment = Button(this).apply { text = "Cancel Assignment" }
        val release = Button(this).apply { text = "Release Payout" }
        val clear = Button(this).apply { text = "Clear Session" }
        val logout = Button(this).apply { text = "Logout" }
        val adminPayoutId = EditText(this).apply { hint = "admin payout id" }
        val adminToPending = Button(this).apply { text = "Mark Payout Pending" }
        val adminToPaid = Button(this).apply { text = "Mark Payout Paid" }
        val adminToFailed = Button(this).apply { text = "Mark Payout Failed" }
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
        val adminAssignmentsBlock = bodyText()
        val adminPayoutsBlock = bodyText()
        val adminProblemsBlock = bodyText()

        setContentView(
            ScrollView(this).apply {
                addView(
                    LinearLayout(this@LegacyDashboardActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(24, 24, 24, 24)
                        addView(header); addView(accountState); addView(loadingBanner); addView(errorBanner); addView(statusBanner)
                        addView(refresh); addView(workerDemo); addView(businessDemo); addView(adminDemo); addView(userId); addView(password)
                        addView(login); addView(logout); addView(clear)
                        addView(sectionTitle("Worker actions")); addView(shiftId); addView(apply); addView(assignmentId); addView(checkIn); addView(checkOut)
                        addView(sectionTitle("Business actions")); addView(shiftTitle); addView(locationId); addView(payRate); addView(createShift)
                        addView(publishShift); addView(appId); addView(offer); addView(reject); addView(withdraw); addView(closeShift); addView(cancelShift)
                        addView(cancelAssignment); addView(release)
                        addView(sectionTitle("Important events")); addView(eventsBlock)
                        addView(sectionTitle("Worker: available shifts")); addView(availableShiftsBlock)
                        addView(sectionTitle("Worker: my applications")); addView(applicationsBlock)
                        addView(sectionTitle("Worker: my assignments")); addView(assignmentsBlock)
                        addView(sectionTitle("Worker: current active shift")); addView(activeShiftBlock)
                        addView(sectionTitle("Worker: completed shifts")); addView(completedBlock)
                        addView(sectionTitle("Worker: payout status")); addView(payoutsBlock)
                        addView(sectionTitle("Business: owned shifts")); addView(businessShiftsBlock)
                        addView(sectionTitle("Business: applications for selected shift")); addView(businessAppsBlock)
                        addView(sectionTitle("Business: assignments status")); addView(businessAssignmentsBlock)
                        addView(sectionTitle("Business: payout/release status")); addView(businessPayoutBlock)
                        addView(sectionTitle("Admin/operator payout ops")); addView(adminPayoutId); addView(adminToPending); addView(adminToPaid); addView(adminToFailed)
                        addView(sectionTitle("Admin: assignments")); addView(adminAssignmentsBlock)
                        addView(sectionTitle("Admin: payouts")); addView(adminPayoutsBlock)
                        addView(sectionTitle("Admin: problem cases")); addView(adminProblemsBlock)
                    }
                )
            }
        )

        workerDemo.setOnClickListener { userId.setText("worker.demo"); password.setText("workerpass") }
        businessDemo.setOnClickListener { userId.setText("business.demo"); password.setText("businesspass") }
        adminDemo.setOnClickListener { userId.setText("admin.demo"); password.setText("adminpass") }
        login.setOnClickListener { viewModel.onAction(MainAction.Login(userId.text.toString(), password.text.toString())) }
        logout.setOnClickListener { viewModel.onAction(MainAction.Logout) }
        refresh.setOnClickListener {
            viewModel.onAction(MainAction.Refresh)
        }
        apply.setOnClickListener { viewModel.onAction(MainAction.ApplyToShift(shiftId.text.toString())) }
        checkIn.setOnClickListener { viewModel.onAction(MainAction.CheckIn(assignmentId.text.toString())) }
        checkOut.setOnClickListener { viewModel.onAction(MainAction.CheckOut(assignmentId.text.toString())) }
        createShift.setOnClickListener { viewModel.onAction(MainAction.CreateShift(shiftTitle.text.toString(), locationId.text.toString(), payRate.text.toString().toIntOrNull() ?: 2200)) }
        publishShift.setOnClickListener { viewModel.onAction(MainAction.PublishShift(shiftId.text.toString())) }
        offer.setOnClickListener { viewModel.onAction(MainAction.OfferAssignment(appId.text.toString())) }
        reject.setOnClickListener { viewModel.onAction(MainAction.RejectApplication(appId.text.toString())) }
        withdraw.setOnClickListener { viewModel.onAction(MainAction.WithdrawApplication(appId.text.toString())) }
        closeShift.setOnClickListener { viewModel.onAction(MainAction.CloseShift(shiftId.text.toString())) }
        cancelShift.setOnClickListener { viewModel.onAction(MainAction.CancelShift(shiftId.text.toString())) }
        cancelAssignment.setOnClickListener { viewModel.onAction(MainAction.CancelAssignment(assignmentId.text.toString())) }
        release.setOnClickListener { viewModel.onAction(MainAction.ReleasePayout(assignmentId.text.toString())) }
        adminToPending.setOnClickListener { viewModel.onAction(MainAction.ProgressAdminPayout(adminPayoutId.text.toString(), "pending")) }
        adminToPaid.setOnClickListener { viewModel.onAction(MainAction.ProgressAdminPayout(adminPayoutId.text.toString(), "paid")) }
        adminToFailed.setOnClickListener { viewModel.onAction(MainAction.ProgressAdminPayout(adminPayoutId.text.toString(), "failed")) }
        clear.setOnClickListener { viewModel.onAction(MainAction.ClearSession) }

        val selectionWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                viewModel.onAction(
                    MainAction.UpdateSelections(
                        shiftId = shiftId.text?.toString(),
                        assignmentId = assignmentId.text?.toString(),
                        applicationId = appId.text?.toString()
                    )
                )
            }
        }
        shiftId.addTextChangedListener(selectionWatcher)
        assignmentId.addTextChangedListener(selectionWatcher)
        appId.addTextChangedListener(selectionWatcher)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    accountState.text = "Role: ${state.role.asApiValue()} user=${state.sessionUserId ?: "-"}"
                    loadingBanner.text = if (state.loadState == UiLoadState.LOADING) "Loading dashboard..." else ""
                    errorBanner.text = state.errorMessage?.let { "Error: $it" } ?: ""
                    statusBanner.text = state.statusMessage?.let { "Last action: $it" } ?: ""

                    refresh.isEnabled = state.capabilities.canRefresh
                    login.isEnabled = state.capabilities.canLogin
                    apply.isEnabled = state.capabilities.canApplySelectedShift
                    checkIn.isEnabled = state.capabilities.canCheckInSelectedAssignment
                    checkOut.isEnabled = state.capabilities.canCheckOutSelectedAssignment
                    createShift.isEnabled = state.capabilities.canCreateShift
                    publishShift.isEnabled = state.capabilities.canPublishSelectedShift
                    offer.isEnabled = state.capabilities.canOfferSelectedApplication
                    reject.isEnabled = state.capabilities.canRejectSelectedApplication
                    withdraw.isEnabled = state.capabilities.canWithdrawSelectedApplication
                    closeShift.isEnabled = state.capabilities.canCloseSelectedShift
                    cancelShift.isEnabled = state.capabilities.canCancelSelectedShift
                    cancelAssignment.isEnabled = state.capabilities.canCancelSelectedAssignment
                    release.isEnabled = state.capabilities.canReleaseSelectedAssignmentPayout
                    adminToPending.isEnabled = state.capabilities.canProgressAdminPayout && adminPayoutId.text.isNotBlank()
                    adminToPaid.isEnabled = state.capabilities.canProgressAdminPayout && adminPayoutId.text.isNotBlank()
                    adminToFailed.isEnabled = state.capabilities.canProgressAdminPayout && adminPayoutId.text.isNotBlank()

                    eventsBlock.text = if (state.importantEvents.isEmpty()) "No important events yet." else state.importantEvents.joinToString("\n") { "• $it" }
                    renderSections(state, availableShiftsBlock, applicationsBlock, assignmentsBlock, activeShiftBlock, completedBlock, payoutsBlock, businessShiftsBlock, businessAppsBlock, businessAssignmentsBlock, businessPayoutBlock, adminAssignmentsBlock, adminPayoutsBlock, adminProblemsBlock)

                    if (shiftId.text.isBlank() && !state.selectedShiftId.isNullOrBlank()) shiftId.setText(state.selectedShiftId)
                    if (assignmentId.text.isBlank() && !state.selectedAssignmentId.isNullOrBlank()) assignmentId.setText(state.selectedAssignmentId)
                    if (appId.text.isBlank() && !state.selectedApplicationId.isNullOrBlank()) appId.setText(state.selectedApplicationId)
                }
            }
        }
    }

    private fun renderSections(state: MainUiState, availableShiftsBlock: TextView, applicationsBlock: TextView, assignmentsBlock: TextView, activeShiftBlock: TextView, completedBlock: TextView, payoutsBlock: TextView, businessShiftsBlock: TextView, businessAppsBlock: TextView, businessAssignmentsBlock: TextView, businessPayoutBlock: TextView, adminAssignmentsBlock: TextView, adminPayoutsBlock: TextView, adminProblemsBlock: TextView) {
        when (val home = state.home) {
            is HomeState.Worker -> {
                availableShiftsBlock.text = home.content.availableShifts.joinToString("\n") { "• ${it.title} (${it.id}) @ ${it.startAt}" }.ifBlank { "No available shifts right now." }
                applicationsBlock.text = home.content.applications.joinToString("\n") { "• ${it.id}: shift=${it.shiftId} status=${it.rawStatus}" }.ifBlank { "No applications yet." }
                assignmentsBlock.text = home.content.assignments.joinToString("\n") { "• ${it.id}: shift=${it.shiftId} status=${it.rawStatus}" }.ifBlank { "No assignments yet." }
                activeShiftBlock.text = home.content.activeAssignment?.let { "${it.shiftId} (${it.rawStatus})" } ?: "No active shift."
                completedBlock.text = home.content.completedAssignments.joinToString("\n") { "• ${it.shiftId}: ${it.rawStatus}" }.ifBlank { "No completed shifts yet." }
                payoutsBlock.text = home.content.payouts.joinToString("\n") { "• ${it.id}: assignment=${it.assignmentId} amount=${it.amountCents} status=${it.rawStatus} (${it.status})" }.ifBlank { "No payouts yet." }
                businessShiftsBlock.text = "Sign in as a business user to view this section."
                businessAppsBlock.text = businessShiftsBlock.text
                businessAssignmentsBlock.text = businessShiftsBlock.text
                businessPayoutBlock.text = businessShiftsBlock.text
                adminAssignmentsBlock.text = "Sign in as admin to view this section."
                adminPayoutsBlock.text = adminAssignmentsBlock.text
                adminProblemsBlock.text = adminAssignmentsBlock.text
            }
            is HomeState.Business -> {
                val prompt = "Sign in as a worker to view this section."
                availableShiftsBlock.text = prompt
                applicationsBlock.text = prompt
                assignmentsBlock.text = prompt
                activeShiftBlock.text = prompt
                completedBlock.text = prompt
                payoutsBlock.text = prompt
                businessShiftsBlock.text = home.content.ownedShifts.joinToString("\n") { "• ${it.title} (${it.id}) status=${it.rawStatus}" }.ifBlank { "No owned shifts yet." }
                businessAppsBlock.text = home.content.applicationsForSelectedShift.joinToString("\n") { "• ${it.id}: worker=${it.workerId} status=${it.rawStatus}" }.ifBlank { "No applications yet for selected shift." }
                businessAssignmentsBlock.text = home.content.assignments.joinToString("\n") { "• ${it.id}: worker=${it.workerId} status=${it.rawStatus}" }.ifBlank { "No assignments yet." }
                businessPayoutBlock.text = home.content.payoutReadyAssignments.joinToString("\n") { "• ${it.id}: ready to release payout" }.ifBlank { "No completed assignments ready for payout release." }
                adminAssignmentsBlock.text = "Sign in as admin to view this section."
                adminPayoutsBlock.text = adminAssignmentsBlock.text
                adminProblemsBlock.text = adminAssignmentsBlock.text
            }
            is HomeState.Admin -> {
                val prompt = "Sign in as a worker to view this section."
                availableShiftsBlock.text = prompt
                applicationsBlock.text = prompt
                assignmentsBlock.text = prompt
                activeShiftBlock.text = prompt
                completedBlock.text = prompt
                payoutsBlock.text = prompt
                val bizPrompt = "Sign in as a business user to view this section."
                businessShiftsBlock.text = bizPrompt
                businessAppsBlock.text = bizPrompt
                businessAssignmentsBlock.text = bizPrompt
                businessPayoutBlock.text = bizPrompt
                adminAssignmentsBlock.text = home.content.assignments.joinToString("\n") { "• ${it.id}: worker=${it.workerId} status=${it.rawStatus}" }.ifBlank { "No assignments." }
                adminPayoutsBlock.text = home.content.payouts.joinToString("\n") { "• ${it.id}: assignment=${it.assignmentId} status=${it.rawStatus} (${it.status})" }.ifBlank { "No payouts." }
                adminProblemsBlock.text = "Flags=${home.content.problemCases.flags.size}, FailedPayouts=${home.content.problemCases.failedPayouts.size}, StalledAssignments=${home.content.problemCases.stalledAssignments.size}"
            }
            HomeState.None -> {
                val msg = "Load dashboard to view data."
                availableShiftsBlock.text = msg
                applicationsBlock.text = msg
                assignmentsBlock.text = msg
                activeShiftBlock.text = msg
                completedBlock.text = msg
                payoutsBlock.text = msg
                businessShiftsBlock.text = msg
                businessAppsBlock.text = msg
                businessAssignmentsBlock.text = msg
                businessPayoutBlock.text = msg
                adminAssignmentsBlock.text = msg
                adminPayoutsBlock.text = msg
                adminProblemsBlock.text = msg
            }
        }
    }

    private fun sectionTitle(text: String) = TextView(this).apply { this.text = text; textSize = 16f; setTypeface(typeface, Typeface.BOLD); gravity = Gravity.START }
    private fun bodyText() = TextView(this).apply { textSize = 14f }
}
