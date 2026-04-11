package com.povarup

import com.povarup.core.AppDispatchers
import com.povarup.core.MainViewModel
import com.povarup.data.MarketplaceError
import com.povarup.data.MarketplaceRepository
import com.povarup.data.SessionToken
import com.povarup.domain.Shift
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @Test
    fun refreshShiftsUpdatesUiStateFromRepository() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        try {
            val repository = object : MarketplaceRepository {
                override fun currentRole(): String = "worker"
                override fun setRole(role: String) = Unit
                override fun baseUrl(): String = "http://localhost:4000/api"
                override fun currentSession(): SessionToken? = null
                override fun createSession(userId: String, role: String): Result<SessionToken> =
                    Result.success(SessionToken("sess", userId, role))

                override fun clearSession() = Unit
                override fun listShifts(): Result<List<Shift>> = Result.success(
                    listOf(
                        Shift(
                            id = "shift_1",
                            businessId = "biz_1",
                            locationId = "loc_1",
                            title = "Cook",
                            startAt = "2026-01-01T10:00:00Z",
                            endAt = "2026-01-01T14:00:00Z",
                            payRateCents = 2000,
                            status = "open"
                        )
                    )
                )
            }

            val vm = MainViewModel(repository, AppDispatchers(io = testDispatcher))
            advanceUntilIdle()

            assertEquals(1, vm.uiState.value.shiftsCount)
            assertFalse(vm.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun createSessionForRoleExposesSessionState() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        try {
            val repository = object : MarketplaceRepository {
                override fun currentRole(): String = "worker"
                override fun setRole(role: String) = Unit
                override fun baseUrl(): String = "http://localhost:4000/api"
                override fun currentSession(): SessionToken? = null
                override fun createSession(userId: String, role: String): Result<SessionToken> =
                    Result.success(SessionToken("sess_123", userId, role))

                override fun clearSession() = Unit
                override fun listShifts(): Result<List<Shift>> = Result.success(emptyList())
            }

            val vm = MainViewModel(repository, AppDispatchers(io = testDispatcher))
            vm.createSessionForRole("u_worker_1")
            advanceUntilIdle()

            assertTrue(vm.uiState.value.hasSession)
            assertEquals("u_worker_1", vm.uiState.value.sessionUserId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun refreshShiftsExposesRepositoryError() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        try {
            val repository = object : MarketplaceRepository {
                override fun currentRole(): String = "worker"
                override fun setRole(role: String) = Unit
                override fun baseUrl(): String = "http://localhost:4000/api"
                override fun currentSession(): SessionToken? = null
                override fun createSession(userId: String, role: String): Result<SessionToken> =
                    Result.failure(MarketplaceError.Api("forbidden", "Not authorized"))

                override fun clearSession() = Unit
                override fun listShifts(): Result<List<Shift>> =
                    Result.failure(MarketplaceError.Api("forbidden", "Not authorized"))
            }

            val vm = MainViewModel(repository, AppDispatchers(io = testDispatcher))
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoading)
            assertTrue(vm.uiState.value.errorMessage?.contains("forbidden") == true)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
