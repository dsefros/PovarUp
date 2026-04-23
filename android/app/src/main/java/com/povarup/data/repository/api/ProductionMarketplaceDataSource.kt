package com.povarup.data.repository.api

import com.povarup.data.CreateShiftRequest
import com.povarup.data.MarketplaceRepository
import com.povarup.data.repository.MarketplaceDataSource
import com.povarup.domain.Application
import com.povarup.domain.Assignment
import com.povarup.domain.Payout
import com.povarup.domain.Shift
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductionMarketplaceDataSource(
    private val repository: MarketplaceRepository
) : MarketplaceDataSource {
    override suspend fun listShifts(): Result<List<Shift>> = io { repository.listShifts() }
    override suspend fun getShift(shiftId: String): Result<Shift> = io { repository.getShift(shiftId) }
    override suspend fun applyToShift(shiftId: String): Result<Application> = io { repository.applyToShift(shiftId) }
    override suspend fun listApplications(): Result<List<Application>> = io { repository.listApplications() }
    override suspend fun listAssignments(): Result<List<Assignment>> = io { repository.listAssignments() }
    override suspend fun getAssignment(assignmentId: String): Result<Assignment> = io { repository.getAssignment(assignmentId) }
    override suspend fun checkIn(assignmentId: String): Result<Unit> = io { repository.checkIn(assignmentId) }
    override suspend fun checkOut(assignmentId: String): Result<Unit> = io { repository.checkOut(assignmentId) }
    override suspend fun listPayouts(): Result<List<Payout>> = io { repository.listMyPayouts() }
    override suspend fun createShift(input: CreateShiftRequest): Result<Shift> = io { repository.createShift(input) }
    override suspend fun listBusinessShifts(): Result<List<Shift>> = io { repository.listBusinessShifts() }
    override suspend fun listShiftApplications(shiftId: String): Result<List<Application>> = io { repository.listShiftApplications(shiftId) }
    override suspend fun assignApplicant(applicationId: String): Result<Assignment> = io { repository.offerAssignment(applicationId) }
    override suspend fun rejectApplicant(applicationId: String): Result<Application> = io { repository.rejectApplication(applicationId) }
    override suspend fun releasePayout(assignmentId: String): Result<Payout> = io { repository.releasePayout(assignmentId) }

    private suspend fun <T> io(block: () -> Result<T>): Result<T> = withContext(Dispatchers.IO) { block() }
}
