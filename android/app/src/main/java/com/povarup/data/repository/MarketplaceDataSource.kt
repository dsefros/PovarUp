package com.povarup.data.repository

import com.povarup.data.CreateShiftRequest
import com.povarup.domain.Application
import com.povarup.domain.Assignment
import com.povarup.domain.Payout
import com.povarup.domain.Shift

interface MarketplaceDataSource {
    suspend fun listShifts(): Result<List<Shift>>
    suspend fun getShift(shiftId: String): Result<Shift>
    suspend fun applyToShift(shiftId: String): Result<Application>
    suspend fun listApplications(): Result<List<Application>>
    suspend fun listAssignments(): Result<List<Assignment>>
    suspend fun getAssignment(assignmentId: String): Result<Assignment>
    suspend fun checkIn(assignmentId: String): Result<Unit>
    suspend fun checkOut(assignmentId: String): Result<Unit>
    suspend fun listPayouts(): Result<List<Payout>>

    suspend fun createShift(input: CreateShiftRequest): Result<Shift>
    suspend fun listBusinessShifts(): Result<List<Shift>>
    suspend fun listShiftApplications(shiftId: String): Result<List<Application>>
    suspend fun assignApplicant(applicationId: String): Result<Assignment>
    suspend fun rejectApplicant(applicationId: String): Result<Application>
    suspend fun releasePayout(assignmentId: String): Result<Payout>
}
