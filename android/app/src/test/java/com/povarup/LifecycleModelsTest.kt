package com.povarup

import com.povarup.domain.Application
import com.povarup.domain.ApplicationStatus
import com.povarup.domain.Assignment
import com.povarup.domain.AssignmentStatus
import com.povarup.domain.Shift
import com.povarup.domain.ShiftStatus
import com.povarup.domain.UserRole
import com.povarup.domain.capability
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LifecycleModelsTest {
    @Test
    fun applyCapabilityDependsOnShiftStatusAndRelationship() {
        val shift = Shift("s1", "b1", "l1", "Cook", "", "", 1000, ShiftStatus.PUBLISHED, "published")
        assertTrue(shift.capability(UserRole.WORKER, alreadyRelated = false).canApply)
        assertFalse(shift.capability(UserRole.WORKER, alreadyRelated = true).canApply)
        assertFalse(shift.copy(status = ShiftStatus.CLOSED).capability(UserRole.WORKER, alreadyRelated = false).canApply)
    }

    @Test
    fun checkoutCapabilityRequiresInProgress() {
        val inProgress = Assignment("a1", "s1", "w1", "b1", AssignmentStatus.IN_PROGRESS, "in_progress", 1000)
        val completed = inProgress.copy(status = AssignmentStatus.COMPLETED)
        assertTrue(inProgress.capability(UserRole.WORKER).canCheckOut)
        assertFalse(completed.capability(UserRole.WORKER).canCheckOut)
    }

    @Test
    fun withdrawCapabilityFollowsApplicationLifecycle() {
        val applied = Application("app", "s1", "w1", ApplicationStatus.APPLIED, "applied")
        val rejected = applied.copy(status = ApplicationStatus.REJECTED)
        assertTrue(applied.capability(UserRole.WORKER).canWithdraw)
        assertFalse(rejected.capability(UserRole.WORKER).canWithdraw)
    }
}
