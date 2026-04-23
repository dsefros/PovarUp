package com.povarup.data.repository.demo

import com.povarup.data.CreateShiftRequest
import com.povarup.data.repository.MarketplaceDataSource
import com.povarup.domain.Application
import com.povarup.domain.ApplicationStatus
import com.povarup.domain.Assignment
import com.povarup.domain.AssignmentStatus
import com.povarup.domain.Payout
import com.povarup.domain.PayoutStatus
import com.povarup.domain.Shift
import com.povarup.domain.ShiftStatus
import kotlinx.coroutines.delay

class ScenarioMarketplaceRepository(
    scenarioId: String = SCENARIO_WORKER_BROWSING
) : MarketplaceDataSource {

    private val state = ScenarioState.forScenario(scenarioId)

    fun switchScenario(scenarioId: String) {
        state.reset(ScenarioState.forScenario(scenarioId))
    }

    override suspend fun listShifts(): Result<List<Shift>> = delayed { Result.success(state.shifts.toList()) }
    override suspend fun getShift(shiftId: String): Result<Shift> = delayed {
        state.shifts.firstOrNull { it.id == shiftId }?.let { Result.success(it) }
            ?: Result.failure(IllegalArgumentException("Shift not found"))
    }

    override suspend fun applyToShift(shiftId: String): Result<Application> = delayed {
        val shift = state.shifts.firstOrNull { it.id == shiftId } ?: return@delayed Result.failure(IllegalArgumentException("Shift not found"))
        if (shift.status != ShiftStatus.PUBLISHED) return@delayed Result.failure(IllegalStateException("Shift unavailable"))
        if (state.applications.any { it.shiftId == shiftId && it.workerId == WORKER_ID }) {
            return@delayed Result.failure(IllegalStateException("Already applied"))
        }
        val app = Application(
            id = "app-${System.currentTimeMillis()}",
            shiftId = shiftId,
            workerId = WORKER_ID,
            status = ApplicationStatus.APPLIED,
            rawStatus = "applied"
        )
        state.applications.add(app)
        Result.success(app)
    }

    override suspend fun listApplications(): Result<List<Application>> = delayed {
        Result.success(state.applications.filter { it.workerId == WORKER_ID })
    }

    override suspend fun listAssignments(): Result<List<Assignment>> = delayed {
        Result.success(state.assignments.filter { it.workerId == WORKER_ID || it.businessId == BUSINESS_ID })
    }

    override suspend fun getAssignment(assignmentId: String): Result<Assignment> = delayed {
        state.assignments.firstOrNull { it.id == assignmentId }?.let { Result.success(it) }
            ?: Result.failure(IllegalArgumentException("Assignment not found"))
    }

    override suspend fun checkIn(assignmentId: String): Result<Unit> = delayed {
        transitionAssignment(assignmentId, from = AssignmentStatus.ASSIGNED, to = AssignmentStatus.IN_PROGRESS)
    }

    override suspend fun checkOut(assignmentId: String): Result<Unit> = delayed {
        val result = transitionAssignment(assignmentId, from = AssignmentStatus.IN_PROGRESS, to = AssignmentStatus.COMPLETED)
        if (result.isSuccess) {
            val assignment = state.assignments.first { it.id == assignmentId }
            state.payouts.removeAll { it.assignmentId == assignmentId }
            state.payouts.add(
                Payout(
                    id = "payout-$assignmentId",
                    assignmentId = assignmentId,
                    workerId = WORKER_ID,
                    amountCents = assignment.escrowLockedCents,
                    status = PayoutStatus.PENDING,
                    rawStatus = "pending"
                )
            )
        }
        result
    }

    override suspend fun listPayouts(): Result<List<Payout>> = delayed { Result.success(state.payouts.filter { it.workerId == WORKER_ID }) }

    override suspend fun createShift(input: CreateShiftRequest): Result<Shift> = delayed {
        val shift = Shift(
            id = "shift-${System.currentTimeMillis()}",
            businessId = BUSINESS_ID,
            locationId = input.locationId,
            title = input.title,
            startAt = input.startAt,
            endAt = input.endAt,
            payRateCents = input.payRateCents,
            status = ShiftStatus.DRAFT,
            rawStatus = "draft"
        )
        state.shifts.add(0, shift)
        Result.success(shift)
    }

    override suspend fun listBusinessShifts(): Result<List<Shift>> = delayed { Result.success(state.shifts.filter { it.businessId == BUSINESS_ID }) }

    override suspend fun listShiftApplications(shiftId: String): Result<List<Application>> = delayed {
        Result.success(state.applications.filter { it.shiftId == shiftId })
    }

    override suspend fun assignApplicant(applicationId: String): Result<Assignment> = delayed {
        val app = state.applications.firstOrNull { it.id == applicationId } ?: return@delayed Result.failure(IllegalArgumentException("Application not found"))
        state.applications.replaceAll { if (it.id == applicationId) it.copy(status = ApplicationStatus.ACCEPTED, rawStatus = "accepted") else it }
        val shift = state.shifts.first { it.id == app.shiftId }
        val assignment = Assignment(
            id = "as-${System.currentTimeMillis()}",
            shiftId = app.shiftId,
            workerId = app.workerId,
            businessId = shift.businessId,
            status = AssignmentStatus.ASSIGNED,
            rawStatus = "assigned",
            escrowLockedCents = shift.payRateCents * 8
        )
        state.assignments.add(assignment)
        Result.success(assignment)
    }

    override suspend fun rejectApplicant(applicationId: String): Result<Application> = delayed {
        val existing = state.applications.firstOrNull { it.id == applicationId }
            ?: return@delayed Result.failure(IllegalArgumentException("Application not found"))
        val updated = existing.copy(status = ApplicationStatus.REJECTED, rawStatus = "rejected")
        state.applications.replaceAll { if (it.id == applicationId) updated else it }
        Result.success(updated)
    }

    override suspend fun releasePayout(assignmentId: String): Result<Payout> = delayed {
        val payout = state.payouts.firstOrNull { it.assignmentId == assignmentId }
            ?: return@delayed Result.failure(IllegalArgumentException("Payout not found"))
        val updated = payout.copy(status = PayoutStatus.PAID, rawStatus = "paid")
        state.payouts.replaceAll { if (it.id == payout.id) updated else it }
        state.assignments.replaceAll { if (it.id == assignmentId) it.copy(status = AssignmentStatus.PAID, rawStatus = "paid") else it }
        Result.success(updated)
    }

    private fun transitionAssignment(assignmentId: String, from: AssignmentStatus, to: AssignmentStatus): Result<Unit> {
        val a = state.assignments.firstOrNull { it.id == assignmentId } ?: return Result.failure(IllegalArgumentException("Assignment not found"))
        if (a.status != from) return Result.failure(IllegalStateException("Action blocked for status ${a.rawStatus}"))
        state.assignments.replaceAll { if (it.id == assignmentId) it.copy(status = to, rawStatus = to.name.lowercase()) else it }
        return Result.success(Unit)
    }

    private suspend fun <T> delayed(block: () -> Result<T>): Result<T> {
        delay(250)
        return block()
    }

    private data class ScenarioState(
        val shifts: MutableList<Shift>,
        val applications: MutableList<Application>,
        val assignments: MutableList<Assignment>,
        val payouts: MutableList<Payout>
    ) {
        fun reset(newState: ScenarioState) {
            shifts.clear(); shifts.addAll(newState.shifts)
            applications.clear(); applications.addAll(newState.applications)
            assignments.clear(); assignments.addAll(newState.assignments)
            payouts.clear(); payouts.addAll(newState.payouts)
        }

        companion object {
            fun forScenario(id: String): ScenarioState {
                val baseShifts = mutableListOf(
                    Shift(
                        id = "s1",
                        businessId = BUSINESS_ID,
                        locationId = "Midtown",
                        title = "Line Cook",
                        startAt = "2026-05-01 09:00",
                        endAt = "2026-05-01 17:00",
                        payRateCents = 2500,
                        status = ShiftStatus.PUBLISHED,
                        rawStatus = "published"
                    ),
                    Shift(
                        id = "s2",
                        businessId = BUSINESS_ID,
                        locationId = "Chelsea",
                        title = "Banquet Server",
                        startAt = "2026-05-02 10:00",
                        endAt = "2026-05-02 18:00",
                        payRateCents = 2200,
                        status = ShiftStatus.PUBLISHED,
                        rawStatus = "published"
                    ),
                    Shift(
                        id = "s3",
                        businessId = BUSINESS_ID,
                        locationId = "SoHo",
                        title = "Bartender",
                        startAt = "2026-05-03 16:00",
                        endAt = "2026-05-04 00:00",
                        payRateCents = 2700,
                        status = ShiftStatus.DRAFT,
                        rawStatus = "draft"
                    )
                )
                return when (id) {
                    SCENARIO_WORKER_APPLIED -> ScenarioState(
                        shifts = baseShifts,
                        applications = mutableListOf(
                            Application(
                                id = "a1",
                                shiftId = "s1",
                                workerId = WORKER_ID,
                                status = ApplicationStatus.APPLIED,
                                rawStatus = "applied"
                            )
                        ),
                        assignments = mutableListOf(),
                        payouts = mutableListOf()
                    )
                    SCENARIO_WORKER_ACTIVE_SHIFT -> ScenarioState(
                        shifts = baseShifts,
                        applications = mutableListOf(
                            Application(
                                id = "a2",
                                shiftId = "s1",
                                workerId = WORKER_ID,
                                status = ApplicationStatus.ACCEPTED,
                                rawStatus = "accepted"
                            )
                        ),
                        assignments = mutableListOf(
                            Assignment(
                                id = "as1",
                                shiftId = "s1",
                                workerId = WORKER_ID,
                                businessId = BUSINESS_ID,
                                status = AssignmentStatus.ASSIGNED,
                                rawStatus = "assigned",
                                escrowLockedCents = 20000
                            )
                        ),
                        payouts = mutableListOf()
                    )
                    SCENARIO_WORKER_PAYOUT_PENDING -> ScenarioState(
                        shifts = baseShifts,
                        applications = mutableListOf(),
                        assignments = mutableListOf(
                            Assignment(
                                id = "as2",
                                shiftId = "s2",
                                workerId = WORKER_ID,
                                businessId = BUSINESS_ID,
                                status = AssignmentStatus.COMPLETED,
                                rawStatus = "completed",
                                escrowLockedCents = 17600
                            )
                        ),
                        payouts = mutableListOf(
                            Payout(
                                id = "p1",
                                assignmentId = "as2",
                                workerId = WORKER_ID,
                                amountCents = 17600,
                                status = PayoutStatus.PENDING,
                                rawStatus = "pending"
                            )
                        )
                    )
                    SCENARIO_BUSINESS_WITH_DRAFTS -> ScenarioState(
                        shifts = baseShifts,
                        applications = mutableListOf(),
                        assignments = mutableListOf(),
                        payouts = mutableListOf()
                    )
                    SCENARIO_BUSINESS_WITH_APPLICANTS -> ScenarioState(
                        shifts = baseShifts,
                        applications = mutableListOf(
                            Application(
                                id = "a3",
                                shiftId = "s1",
                                workerId = WORKER_ID,
                                status = ApplicationStatus.APPLIED,
                                rawStatus = "applied"
                            ),
                            Application(
                                id = "a4",
                                shiftId = "s2",
                                workerId = "worker-2",
                                status = ApplicationStatus.APPLIED,
                                rawStatus = "applied"
                            )
                        ),
                        assignments = mutableListOf(),
                        payouts = mutableListOf()
                    )
                    SCENARIO_BUSINESS_ACTIVE_SHIFT -> ScenarioState(
                        shifts = baseShifts,
                        applications = mutableListOf(),
                        assignments = mutableListOf(
                            Assignment(
                                id = "as3",
                                shiftId = "s1",
                                workerId = WORKER_ID,
                                businessId = BUSINESS_ID,
                                status = AssignmentStatus.IN_PROGRESS,
                                rawStatus = "in_progress",
                                escrowLockedCents = 20000
                            )
                        ),
                        payouts = mutableListOf()
                    )
                    else -> ScenarioState(
                        shifts = baseShifts,
                        applications = mutableListOf(),
                        assignments = mutableListOf(),
                        payouts = mutableListOf()
                    )
                }
            }
        }
    }

    companion object {
        const val WORKER_ID = "worker.demo"
        const val BUSINESS_ID = "business.demo"
        const val SCENARIO_WORKER_BROWSING = "worker_browsing"
        const val SCENARIO_WORKER_APPLIED = "worker_applied"
        const val SCENARIO_WORKER_ACTIVE_SHIFT = "worker_active_shift"
        const val SCENARIO_WORKER_PAYOUT_PENDING = "worker_payout_pending"
        const val SCENARIO_BUSINESS_WITH_DRAFTS = "business_with_drafts"
        const val SCENARIO_BUSINESS_WITH_APPLICANTS = "business_with_applicants"
        const val SCENARIO_BUSINESS_ACTIVE_SHIFT = "business_active_shift"

        val scenarioIds = listOf(
            SCENARIO_WORKER_BROWSING,
            SCENARIO_WORKER_APPLIED,
            SCENARIO_WORKER_ACTIVE_SHIFT,
            SCENARIO_WORKER_PAYOUT_PENDING,
            SCENARIO_BUSINESS_WITH_DRAFTS,
            SCENARIO_BUSINESS_WITH_APPLICANTS,
            SCENARIO_BUSINESS_ACTIVE_SHIFT
        )
    }
}
