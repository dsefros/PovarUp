package com.povarup

import com.povarup.data.ApiError
import com.povarup.data.ApiItemEnvelope
import com.povarup.data.ApiListEnvelope
import com.povarup.data.ApplicationDto
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.CreateApplicationRequest
import com.povarup.data.CreateSessionRequest
import com.povarup.data.MarketplaceApi
import com.povarup.data.MarketplaceError
import com.povarup.data.SessionDto
import com.povarup.data.SessionStore
import com.povarup.data.SessionToken
import com.povarup.data.ShiftDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ApiMarketplaceRepositoryTest {
    @Test
    fun listShiftsMapsItemsToDomain() {
        val api = object : MarketplaceApi {
            override fun fetchShifts(baseUrl: String, bearerToken: String?): Result<ApiListEnvelope<ShiftDto>> = Result.success(
                ApiListEnvelope(
                    items = listOf(
                        ShiftDto(
                            id = "shift_1",
                            businessId = "biz_1",
                            locationId = "loc_1",
                            title = "Prep Cook",
                            startAt = "2026-04-08T10:00:00Z",
                            endAt = "2026-04-08T14:00:00Z",
                            payRateCents = 2300,
                            status = "open"
                        )
                    )
                )
            )

            override fun createSession(baseUrl: String, request: CreateSessionRequest): Result<ApiItemEnvelope<SessionDto>> {
                error("Not used")
            }

            override fun createApplication(
                baseUrl: String,
                bearerToken: String?,
                request: CreateApplicationRequest
            ): Result<ApiItemEnvelope<ApplicationDto>> {
                error("Not used")
            }
        }

        val repository = ApiMarketplaceRepository(api = api, baseUrlProvider = { "http://localhost:4000/api" })
        val result = repository.listShifts()

        assertTrue(result.isSuccess)
        assertEquals("Prep Cook", result.getOrThrow().first().title)
    }

    @Test
    fun createSessionPersistsTokenAndUsesItForProtectedCalls() {
        val recordingStore = RecordingSessionStore()
        val api = object : MarketplaceApi {
            var lastBearerToken: String? = null

            override fun fetchShifts(baseUrl: String, bearerToken: String?): Result<ApiListEnvelope<ShiftDto>> {
                lastBearerToken = bearerToken
                return Result.success(ApiListEnvelope(items = emptyList()))
            }

            override fun createSession(baseUrl: String, request: CreateSessionRequest): Result<ApiItemEnvelope<SessionDto>> =
                Result.success(ApiItemEnvelope(item = SessionDto(token = "sess_123", userId = request.userId, role = request.role)))

            override fun createApplication(
                baseUrl: String,
                bearerToken: String?,
                request: CreateApplicationRequest
            ): Result<ApiItemEnvelope<ApplicationDto>> {
                error("Not used")
            }
        }

        val repository = ApiMarketplaceRepository(api = api, sessionStore = recordingStore)

        val created = repository.createSession(userId = "u_worker_1", role = "worker")
        assertTrue(created.isSuccess)
        assertEquals("sess_123", recordingStore.load()?.token)

        repository.listShifts()
        assertEquals("sess_123", api.lastBearerToken)
    }

    @Test
    fun createSessionReturnsTypedApiFailureWhenApiReturnsErrorEnvelope() {
        val api = object : MarketplaceApi {
            override fun fetchShifts(baseUrl: String, bearerToken: String?): Result<ApiListEnvelope<ShiftDto>> =
                Result.success(ApiListEnvelope(items = emptyList()))

            override fun createSession(baseUrl: String, request: CreateSessionRequest): Result<ApiItemEnvelope<SessionDto>> =
                Result.success(ApiItemEnvelope(error = ApiError(code = "forbidden", message = "Not authorized")))

            override fun createApplication(
                baseUrl: String,
                bearerToken: String?,
                request: CreateApplicationRequest
            ): Result<ApiItemEnvelope<ApplicationDto>> {
                error("Not used")
            }
        }

        val repository = ApiMarketplaceRepository(api = api)
        val result = repository.createSession(userId = "u_worker_1", role = "worker")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MarketplaceError.Api)
    }

    @Test
    fun listShiftsReturnsTypedApiFailureWhenApiReturnsErrorEnvelope() {
        val api = object : MarketplaceApi {
            override fun fetchShifts(baseUrl: String, bearerToken: String?): Result<ApiListEnvelope<ShiftDto>> = Result.success(
                ApiListEnvelope(error = ApiError(code = "forbidden", message = "Not authorized"))
            )

            override fun createSession(baseUrl: String, request: CreateSessionRequest): Result<ApiItemEnvelope<SessionDto>> {
                error("Not used")
            }

            override fun createApplication(
                baseUrl: String,
                bearerToken: String?,
                request: CreateApplicationRequest
            ): Result<ApiItemEnvelope<ApplicationDto>> {
                error("Not used")
            }
        }

        val repository = ApiMarketplaceRepository(api = api)
        val result = repository.listShifts()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MarketplaceError.Api)
    }

    @Test
    fun listShiftsPassesThroughNetworkFailure() {
        val api = object : MarketplaceApi {
            override fun fetchShifts(baseUrl: String, bearerToken: String?): Result<ApiListEnvelope<ShiftDto>> =
                Result.failure(MarketplaceError.Network(IOException("timeout")))

            override fun createSession(baseUrl: String, request: CreateSessionRequest): Result<ApiItemEnvelope<SessionDto>> {
                error("Not used")
            }

            override fun createApplication(
                baseUrl: String,
                bearerToken: String?,
                request: CreateApplicationRequest
            ): Result<ApiItemEnvelope<ApplicationDto>> {
                error("Not used")
            }
        }

        val repository = ApiMarketplaceRepository(api = api)
        val result = repository.listShifts()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MarketplaceError.Network)
    }

    @Test
    fun clearSessionRemovesPersistedToken() {
        val api = object : MarketplaceApi {
            override fun fetchShifts(baseUrl: String, bearerToken: String?): Result<ApiListEnvelope<ShiftDto>> =
                Result.success(ApiListEnvelope(items = emptyList()))

            override fun createSession(baseUrl: String, request: CreateSessionRequest): Result<ApiItemEnvelope<SessionDto>> =
                Result.success(ApiItemEnvelope(item = SessionDto(token = "sess_123", userId = request.userId, role = request.role)))

            override fun createApplication(
                baseUrl: String,
                bearerToken: String?,
                request: CreateApplicationRequest
            ): Result<ApiItemEnvelope<ApplicationDto>> {
                error("Not used")
            }
        }

        val store = RecordingSessionStore()
        val repository = ApiMarketplaceRepository(api = api, sessionStore = store)
        repository.createSession("u_worker_1", "worker")

        repository.clearSession()

        assertNull(repository.currentSession())
    }

    @Test
    fun applyToShiftUsesSessionTokenAndMapsResponse() {
        val store = RecordingSessionStore().apply {
            save(SessionToken(token = "sess_apply", userId = "u_worker_1", role = "worker"))
        }
        val api = object : MarketplaceApi {
            var lastBearer: String? = null
            var lastRequest: CreateApplicationRequest? = null

            override fun fetchShifts(baseUrl: String, bearerToken: String?): Result<ApiListEnvelope<ShiftDto>> =
                Result.success(ApiListEnvelope(items = emptyList()))

            override fun createSession(baseUrl: String, request: CreateSessionRequest): Result<ApiItemEnvelope<SessionDto>> =
                Result.success(ApiItemEnvelope(item = SessionDto(token = "sess_new", userId = request.userId, role = request.role)))

            override fun createApplication(
                baseUrl: String,
                bearerToken: String?,
                request: CreateApplicationRequest
            ): Result<ApiItemEnvelope<ApplicationDto>> {
                lastBearer = bearerToken
                lastRequest = request
                return Result.success(
                    ApiItemEnvelope(
                        item = ApplicationDto(
                            id = "app_1",
                            shiftId = request.shiftId,
                            workerId = "worker_1",
                            status = "pending"
                        )
                    )
                )
            }
        }

        val repository = ApiMarketplaceRepository(api = api, sessionStore = store)
        val result = repository.applyToShift("shift_77")

        assertTrue(result.isSuccess)
        assertEquals("sess_apply", api.lastBearer)
        assertEquals("shift_77", api.lastRequest?.shiftId)
        assertEquals("pending", result.getOrThrow().status)
    }

    @Test
    fun applyToShiftFailsWithoutSession() {
        val api = object : MarketplaceApi {
            override fun fetchShifts(baseUrl: String, bearerToken: String?): Result<ApiListEnvelope<ShiftDto>> =
                Result.success(ApiListEnvelope(items = emptyList()))

            override fun createSession(baseUrl: String, request: CreateSessionRequest): Result<ApiItemEnvelope<SessionDto>> =
                Result.success(ApiItemEnvelope(item = SessionDto(token = "sess", userId = request.userId, role = request.role)))

            override fun createApplication(
                baseUrl: String,
                bearerToken: String?,
                request: CreateApplicationRequest
            ): Result<ApiItemEnvelope<ApplicationDto>> {
                error("Should not be called without session")
            }
        }

        val repository = ApiMarketplaceRepository(api = api, sessionStore = RecordingSessionStore())
        val result = repository.applyToShift("shift_1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MarketplaceError.Api)
    }

    private class RecordingSessionStore : SessionStore {
        private var session: SessionToken? = null

        override fun load(): SessionToken? = session

        override fun save(session: SessionToken) {
            this.session = session
        }

        override fun clear() {
            session = null
        }
    }
}
