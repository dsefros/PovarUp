package com.povarup.core

import com.povarup.data.ProblemCasesDto
import com.povarup.domain.Application
import com.povarup.domain.ApplicationStatus
import com.povarup.domain.Assignment
import com.povarup.domain.AssignmentStatus
import com.povarup.domain.Payout
import com.povarup.domain.Shift
import com.povarup.domain.UserRole
import com.povarup.domain.capability
import java.time.Duration
import java.time.Instant

data class DashboardSnapshot(
    val role: UserRole,
    val shifts: List<Shift>,
    val applications: List<Application>,
    val assignments: List<Assignment>,
    val payouts: List<Payout>,
    val adminAssignments: List<Assignment>,
    val adminPayouts: List<Payout>,
    val adminProblemCases: ProblemCasesDto
)

fun buildHomeState(snapshot: DashboardSnapshot): HomeState = when (snapshot.role) {
    UserRole.WORKER -> HomeState.Worker(
        WorkerHomeState(
            shifts = snapshot.shifts,
            applications = snapshot.applications,
            assignments = snapshot.assignments,
            payouts = snapshot.payouts,
            availableShifts = computeAvailableShifts(snapshot.shifts, snapshot.applications, snapshot.assignments),
            activeAssignment = snapshot.assignments.firstOrNull { it.status in setOf(AssignmentStatus.ASSIGNED, AssignmentStatus.IN_PROGRESS) },
            completedAssignments = snapshot.assignments.filter { it.status in setOf(AssignmentStatus.COMPLETED, AssignmentStatus.PAID, AssignmentStatus.CANCELLED) }
        )
    )

    UserRole.BUSINESS -> HomeState.Business(
        BusinessHomeState(
            ownedShifts = snapshot.shifts,
            applicationsForSelectedShift = snapshot.applications,
            assignments = snapshot.assignments,
            payoutReadyAssignments = snapshot.assignments.filter { it.status == AssignmentStatus.COMPLETED }
        )
    )

    UserRole.ADMIN -> HomeState.Admin(AdminHomeState(snapshot.adminAssignments, snapshot.adminPayouts, snapshot.adminProblemCases))
    UserRole.UNKNOWN -> HomeState.None
}

fun deriveImportantEvents(shifts: List<Shift>, assignments: List<Assignment>, payouts: List<Payout>): List<String> {
    val shiftById = shifts.associateBy { it.id }
    val events = mutableListOf<String>()

    assignments.filter { it.status == AssignmentStatus.ASSIGNED }.forEach { events += "Assigned: ${shiftById[it.shiftId]?.title ?: it.shiftId} (${it.id})." }
    assignments.filter { it.status == AssignmentStatus.IN_PROGRESS }.forEach { events += "In progress: ${shiftById[it.shiftId]?.title ?: it.shiftId} (${it.id})." }
    assignments.filter { it.status == AssignmentStatus.COMPLETED || it.status == AssignmentStatus.PAID }
        .forEach { events += "Checked out: ${shiftById[it.shiftId]?.title ?: it.shiftId} (${it.id}) completed." }

    payouts.filter { it.status == "created" }.forEach { events += "Payout created for assignment ${it.assignmentId}." }
    payouts.filter { it.status == "released" }.forEach { events += "Payout released for assignment ${it.assignmentId}." }

    val soon = assignments.firstOrNull { assignment ->
        if (assignment.status !in setOf(AssignmentStatus.ASSIGNED, AssignmentStatus.IN_PROGRESS)) return@firstOrNull false
        val shift = shiftById[assignment.shiftId] ?: return@firstOrNull false
        val start = runCatching { Instant.parse(shift.startAt) }.getOrNull() ?: return@firstOrNull false
        Duration.between(Instant.now(), start).toMinutes() in 0..120
    }
    if (soon != null) events += "Upcoming soon: ${shiftById[soon.shiftId]?.title ?: soon.shiftId} (${soon.id}) starts within 2 hours."

    return events.distinct()
}

fun computeCapabilities(state: MainUiState): MainCapabilities {
    val shifts = state.home.allShifts()
    val applications = state.home.allApplications()
    val assignments = state.home.allAssignments()
    val role = state.role

    val selectedShift = shifts.firstOrNull { it.id == state.selectedShiftId }
    val selectedAssignment = assignments.firstOrNull { it.id == state.selectedAssignmentId }
    val selectedApplication = applications.firstOrNull { it.id == state.selectedApplicationId }
    val relatedShiftIds = (applications.map { it.shiftId } + assignments.map { it.shiftId }).toSet()

    val selectedShiftCapability = selectedShift?.capability(role, relatedShiftIds.contains(selectedShift.id))
    val selectedAssignmentCapability = selectedAssignment?.capability(role)
    val selectedApplicationCapability = selectedApplication?.capability(role)

    return MainCapabilities(
        canRefresh = state.loadState != UiLoadState.LOADING,
        canLogin = !state.inFlightActionKeys.contains("login"),
        canApplySelectedShift = selectedShiftCapability?.canApply == true && !state.inFlightActionKeys.contains("apply:${state.selectedShiftId.orEmpty()}"),
        canCheckInSelectedAssignment = selectedAssignmentCapability?.canCheckIn == true && !state.inFlightActionKeys.contains("checkin:${state.selectedAssignmentId.orEmpty()}"),
        canCheckOutSelectedAssignment = selectedAssignmentCapability?.canCheckOut == true && !state.inFlightActionKeys.contains("checkout:${state.selectedAssignmentId.orEmpty()}"),
        canCreateShift = role == UserRole.BUSINESS && !state.inFlightActionKeys.contains("create_shift"),
        canPublishSelectedShift = selectedShiftCapability?.canPublish == true && !state.inFlightActionKeys.contains("publish_shift:${state.selectedShiftId.orEmpty()}"),
        canOfferSelectedApplication = role == UserRole.BUSINESS && selectedApplication?.status == ApplicationStatus.APPLIED && !state.inFlightActionKeys.contains("offer:${state.selectedApplicationId}"),
        canRejectSelectedApplication = selectedApplicationCapability?.canReject == true && !state.inFlightActionKeys.contains("reject:${state.selectedApplicationId.orEmpty()}"),
        canWithdrawSelectedApplication = selectedApplicationCapability?.canWithdraw == true && !state.inFlightActionKeys.contains("withdraw:${state.selectedApplicationId.orEmpty()}"),
        canCloseSelectedShift = selectedShiftCapability?.canClose == true && !state.inFlightActionKeys.contains("close_shift:${state.selectedShiftId.orEmpty()}"),
        canCancelSelectedShift = selectedShiftCapability?.canCancel == true && !state.inFlightActionKeys.contains("cancel_shift:${state.selectedShiftId.orEmpty()}"),
        canCancelSelectedAssignment = selectedAssignmentCapability?.canCancel == true && !state.inFlightActionKeys.contains("cancel_assignment:${state.selectedAssignmentId.orEmpty()}"),
        canReleaseSelectedAssignmentPayout = selectedAssignmentCapability?.canReleasePayout == true && !state.inFlightActionKeys.contains("release:${state.selectedAssignmentId.orEmpty()}"),
        canProgressAdminPayout = role == UserRole.ADMIN
    )
}

fun HomeState.allShifts(): List<Shift> = when (this) {
    is HomeState.Worker -> content.shifts
    is HomeState.Business -> content.ownedShifts
    else -> emptyList()
}

fun HomeState.allApplications(): List<Application> = when (this) {
    is HomeState.Worker -> content.applications
    is HomeState.Business -> content.applicationsForSelectedShift
    else -> emptyList()
}

fun HomeState.allAssignments(): List<Assignment> = when (this) {
    is HomeState.Worker -> content.assignments
    is HomeState.Business -> content.assignments
    is HomeState.Admin -> content.assignments
    HomeState.None -> emptyList()
}

private fun computeAvailableShifts(shifts: List<Shift>, applications: List<Application>, assignments: List<Assignment>): List<Shift> {
    val appliedShiftIds = applications.map { it.shiftId }.toSet()
    val assignedShiftIds = assignments.map { it.shiftId }.toSet()
    return shifts.filter { it.capability(UserRole.WORKER, appliedShiftIds.contains(it.id) || assignedShiftIds.contains(it.id)).canApply }
}
