package com.povarup.ui.shared.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object RoleSelection : Screen("role_selection")
    data object WorkerHome : Screen("worker_home")
    data object ShiftFeed : Screen("shift_feed")
    data object ShiftDetail : Screen("shift_detail/{shiftId}") { fun withId(id: String) = "shift_detail/$id" }
    data object Applications : Screen("applications")
    data object Assignments : Screen("assignments")
    data object AssignmentDetail : Screen("assignment_detail/{assignmentId}") { fun withId(id: String) = "assignment_detail/$id" }
    data object Payouts : Screen("payouts")
    data object BusinessHome : Screen("business_home")
    data object BusinessShifts : Screen("business_shifts")
    data object BusinessShiftCreate : Screen("business_shift_create")
    data object BusinessShiftDetail : Screen("business_shift_detail/{shiftId}") { fun withId(id: String) = "business_shift_detail/$id" }
    data object Applicants : Screen("applicants/{shiftId}") { fun withId(id: String) = "applicants/$id" }
    data object Operations : Screen("operations")
    data object PayoutRelease : Screen("payout_release")
    data object Chat : Screen("chat/{threadId}") { fun withId(id: String) = "chat/$id" }
    data object Profile : Screen("profile")
}
