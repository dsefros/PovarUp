package com.povarup

import com.povarup.core.BusinessHomeState
import com.povarup.core.DashboardSnapshot
import com.povarup.core.HomeState
import com.povarup.core.MainUiState
import com.povarup.core.buildHomeState
import com.povarup.core.computeCapabilities
import com.povarup.core.deriveImportantEvents
import com.povarup.domain.Application
import com.povarup.domain.ApplicationStatus
import com.povarup.domain.Assignment
import com.povarup.domain.AssignmentStatus
import com.povarup.domain.Payout
import com.povarup.domain.PayoutStatus
import com.povarup.domain.Shift
import com.povarup.domain.ShiftStatus
import com.povarup.domain.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainStateComputationsTest {
    @Test
    fun workerAvailableShiftsArePrecomputedByBuildHomeState() {
        val home = buildHomeState(
            DashboardSnapshot(
                role = UserRole.WORKER,
                shifts = listOf(
                    Shift("s1", "b", "l", "Cook", "", "", 1000, ShiftStatus.PUBLISHED, "published"),
                    Shift("s2", "b", "l", "Prep", "", "", 1000, ShiftStatus.PUBLISHED, "published")
                ),
                applications = listOf(Application("app1", "s1", "w", ApplicationStatus.APPLIED, "applied")),
                assignments = emptyList(),
                payouts = emptyList(),
                adminAssignments = emptyList(),
                adminPayouts = emptyList(),
                adminProblemCases = com.povarup.data.ProblemCasesDto()
            )
        ) as HomeState.Worker

        assertEquals(listOf("s2"), home.content.availableShifts.map { it.id })
    }

    @Test
    fun capabilityComputationUsesHomeStateSelections() {
        val businessHome = buildHomeState(
            DashboardSnapshot(
                role = UserRole.BUSINESS,
                shifts = listOf(Shift("s1", "b", "l", "Cook", "", "", 1000, ShiftStatus.DRAFT, "draft")),
                applications = listOf(Application("app1", "s1", "w", ApplicationStatus.APPLIED, "applied")),
                assignments = listOf(Assignment("a1", "s1", "w", "b", AssignmentStatus.COMPLETED, "completed", 1000)),
                payouts = emptyList(),
                adminAssignments = emptyList(),
                adminPayouts = emptyList(),
                adminProblemCases = com.povarup.data.ProblemCasesDto()
            )
        ) as HomeState.Business

        val state = MainUiState(
            role = UserRole.BUSINESS,
            home = businessHome,
            selectedShiftId = "s1",
            selectedApplicationId = "app1",
            selectedAssignmentId = "a1"
        )

        val capabilities = computeCapabilities(state)
        assertTrue(capabilities.canPublishSelectedShift)
        assertTrue(capabilities.canRejectSelectedApplication)
        assertTrue(capabilities.canReleaseSelectedAssignmentPayout)
        assertEquals(listOf("a1"), businessHome.content.payoutReadyAssignments.map { it.id })
    }


    @Test
    fun offerCapabilityIsOnlyEnabledForAppliedApplication() {
        fun capabilitiesFor(status: ApplicationStatus): Boolean {
            val state = MainUiState(
                role = UserRole.BUSINESS,
                home = HomeState.Business(
                    BusinessHomeState(
                        ownedShifts = listOf(Shift("s1", "b", "l", "Cook", "", "", 1000, ShiftStatus.DRAFT, "draft")),
                        applicationsForSelectedShift = listOf(Application("app1", "s1", "w", status, status.name.lowercase())),
                        assignments = emptyList()
                    )
                ),
                selectedApplicationId = "app1"
            )
            return computeCapabilities(state).canOfferSelectedApplication
        }

        assertTrue(capabilitiesFor(ApplicationStatus.APPLIED))
        assertTrue(!capabilitiesFor(ApplicationStatus.REJECTED))
        assertTrue(!capabilitiesFor(ApplicationStatus.WITHDRAWN))
        assertTrue(!capabilitiesFor(ApplicationStatus.ACCEPTED))
    }

    @Test
    fun buildHomeStateUsesRoleSpecificSlices() {
        val snapshot = DashboardSnapshot(
            role = UserRole.ADMIN,
            shifts = emptyList(),
            applications = emptyList(),
            assignments = emptyList(),
            payouts = emptyList(),
            adminAssignments = listOf(Assignment("a1", "s", "w", "b", AssignmentStatus.ASSIGNED, "assigned", 1000)),
            adminPayouts = listOf(Payout("p1", "a1", "w", 1000, PayoutStatus.CREATED, "created")),
            adminProblemCases = com.povarup.data.ProblemCasesDto()
        )

        val home = buildHomeState(snapshot)
        assertTrue(home is HomeState.Admin)
    }

    @Test
    fun importantEventsUseTypedPayoutStatusLifecycle() {
        val events = deriveImportantEvents(
            shifts = emptyList(),
            assignments = emptyList(),
            payouts = listOf(
                Payout("p1", "a1", "w", 1000, PayoutStatus.CREATED, "created"),
                Payout("p2", "a2", "w", 1000, PayoutStatus.PENDING, "pending"),
                Payout("p3", "a3", "w", 1000, PayoutStatus.PAID, "paid"),
                Payout("p4", "a4", "w", 1000, PayoutStatus.FAILED, "failed")
            )
        )

        assertTrue(events.contains("Payout created for assignment a1."))
        assertTrue(events.contains("Payout pending for assignment a2."))
        assertTrue(events.contains("Payout paid for assignment a3."))
        assertTrue(events.contains("Payout failed for assignment a4."))
    }
}
