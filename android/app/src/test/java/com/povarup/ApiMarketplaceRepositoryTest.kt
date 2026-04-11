package com.povarup

import com.povarup.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiMarketplaceRepositoryTest {
    @Test
    fun loginStoresSessionAndProtectedGetUsesBearerToken() {
        val store = RecordingStore()
        val api = RecordingApi(
            onPost = { path, _, _, _ ->
                if (path == "/auth/login") Result.success(SessionDto("sess_1", "worker.demo", "worker"))
                else Result.failure(IllegalStateException("Unexpected path"))
            },
            onGet = { _, _, _, _ -> Result.success(ApiListEnvelope(items = emptyList<ShiftDto>())) }
        )

        val repo = ApiMarketplaceRepository(api = api, sessionStore = store)
        val login = repo.login("worker.demo", "workerpass")

        assertTrue(login.isSuccess)
        repo.listShifts()
        assertEquals("sess_1", api.lastBearer)
    }

    @Test
    fun listShiftsMapsListEnvelope() {
        val store = RecordingStore().apply { save(SessionToken("sess_a", "worker.demo", "worker")) }
        val api = RecordingApi(
            onGet = { path, _, _, _ ->
                if (path == "/shifts") {
                    Result.success(
                        ApiListEnvelope(
                            items = listOf(
                                ShiftDto(
                                    id = "shift_1",
                                    businessId = "biz_1",
                                    locationId = "loc_1",
                                    title = "Prep",
                                    startAt = "2026-01-01T00:00:00Z",
                                    endAt = "2026-01-01T01:00:00Z",
                                    payRateCents = 2000,
                                    status = "open"
                                )
                            )
                        )
                    )
                } else Result.failure(IllegalStateException("Unexpected path"))
            }
        )

        val repo = ApiMarketplaceRepository(api = api, sessionStore = store)
        val result = repo.listShifts()

        assertTrue(result.isSuccess)
        assertEquals("Prep", result.getOrThrow().first().title)
    }

    @Test
    fun applyToShiftUsesBearerAndMapsItemEnvelope() {
        val store = RecordingStore().apply { save(SessionToken("sess_apply", "worker.demo", "worker")) }
        val api = RecordingApi(
            onPost = { path, _, bearer, request ->
                if (path == "/applications") {
                    assertEquals("sess_apply", bearer)
                    val req = request as CreateApplicationRequest
                    Result.success(ApiItemEnvelope(item = ApplicationDto("app_1", req.shiftId, "worker_1", "applied")))
                } else Result.failure(IllegalStateException("Unexpected path"))
            }
        )

        val repo = ApiMarketplaceRepository(api = api, sessionStore = store)
        val result = repo.applyToShift("shift_9")

        assertTrue(result.isSuccess)
        assertEquals("applied", result.getOrThrow().status)
    }

    @Test
    fun non2xxFailurePropagatesForProtectedCall() {
        val store = RecordingStore().apply { save(SessionToken("sess_fail", "worker.demo", "worker")) }
        val api = RecordingApi(
            onGet = { _, _, _, _ -> Result.failure(MarketplaceError.Api("http_error", "403 forbidden")) }
        )

        val repo = ApiMarketplaceRepository(api = api, sessionStore = store)
        val result = repo.listAssignments()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MarketplaceError.Api)
    }

    private class RecordingStore : SessionStore {
        private var session: SessionToken? = null
        override fun load(): SessionToken? = session
        override fun save(session: SessionToken) { this.session = session }
        override fun clear() { session = null }
    }

    private class RecordingApi(
        private val onGet: (String, String, String?, java.lang.reflect.Type) -> Result<Any> = { _, _, _, _ -> Result.failure(IllegalStateException("GET not configured")) },
        private val onPost: (String, String, String?, Any?, java.lang.reflect.Type) -> Result<Any> = { _, _, _, _, _ -> Result.failure(IllegalStateException("POST not configured")) }
    ) : MarketplaceApi {
        var lastBearer: String? = null

        override fun <T> get(baseUrl: String, path: String, bearerToken: String?, type: java.lang.reflect.Type): Result<T> {
            lastBearer = bearerToken
            @Suppress("UNCHECKED_CAST")
            return onGet(path, baseUrl, bearerToken, type) as Result<T>
        }

        override fun <T> post(baseUrl: String, path: String, bearerToken: String?, request: Any?, type: java.lang.reflect.Type): Result<T> {
            lastBearer = bearerToken
            @Suppress("UNCHECKED_CAST")
            return onPost(path, baseUrl, bearerToken, request, type) as Result<T>
        }
    }
}
