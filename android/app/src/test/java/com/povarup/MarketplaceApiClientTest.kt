package com.povarup

import com.google.gson.reflect.TypeToken
import com.povarup.data.ApiItemEnvelope
import com.povarup.data.ApiListEnvelope
import com.povarup.data.CreateApplicationRequest
import com.povarup.data.MarketplaceApiClient
import com.povarup.data.MarketplaceError
import com.povarup.data.SessionDto
import com.povarup.data.ShiftDto
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceApiClientTest {
    @Test
    fun getParsesSuccessfulItemsEnvelope() {
        testServer(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"items":[{"id":"shift_1","businessId":"biz_1","locationId":"loc_1","title":"Cook","startAt":"2026-04-10T10:00:00Z","endAt":"2026-04-10T14:00:00Z","payRateCents":2500,"status":"open"}]}""")
        ) { baseUrl, _ ->
            val result = MarketplaceApiClient().get<ApiListEnvelope<ShiftDto>>(
                baseUrl = baseUrl,
                path = "/shifts",
                bearerToken = null,
                type = object : TypeToken<ApiListEnvelope<ShiftDto>>() {}.type
            )

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().items.size)
            assertEquals("shift_1", result.getOrThrow().items.first().id)
        }
    }

    @Test
    fun postMapsSuccessfulSessionResponse() {
        testServer(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"token":"sess_123","userId":"u_worker_1","role":"worker"}""")
        ) { baseUrl, _ ->
            val result = MarketplaceApiClient().post<SessionDto>(
                baseUrl = baseUrl,
                path = "/auth/login",
                bearerToken = null,
                request = mapOf("userId" to "u_worker_1", "password" to "workerpass"),
                type = object : TypeToken<SessionDto>() {}.type
            )

            assertTrue(result.isSuccess)
            assertEquals("sess_123", result.getOrThrow().token)
            assertEquals("worker", result.getOrThrow().role)
        }
    }

    @Test
    fun getAttachesBearerHeaderWhenTokenProvided() {
        testServer(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"items":[]}""")
        ) { baseUrl, server ->
            val result = MarketplaceApiClient().get<ApiListEnvelope<ShiftDto>>(
                baseUrl = baseUrl,
                path = "/shifts",
                bearerToken = "sess_abc",
                type = object : TypeToken<ApiListEnvelope<ShiftDto>>() {}.type
            )

            assertTrue(result.isSuccess)
            val request = server.takeRequest()
            assertEquals("Bearer sess_abc", request.getHeader("Authorization"))
        }
    }

    @Test
    fun getReturnsApiFailureForNon2xxResponse() {
        testServer(
            MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":{"code":"forbidden","message":"Not allowed","details":"role mismatch"}}""")
        ) { baseUrl, _ ->
            val result = MarketplaceApiClient().get<ApiListEnvelope<ShiftDto>>(
                baseUrl = baseUrl,
                path = "/shifts",
                bearerToken = null,
                type = object : TypeToken<ApiListEnvelope<ShiftDto>>() {}.type
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is MarketplaceError.Api)
            assertTrue(result.exceptionOrNull()?.message?.contains("Not allowed") == true)
        }
    }

    @Test
    fun getReturnsUnexpectedFailureForMalformedJson() {
        testServer(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"items\":[")
        ) { baseUrl, _ ->
            val result = MarketplaceApiClient().get<ApiListEnvelope<ShiftDto>>(
                baseUrl = baseUrl,
                path = "/shifts",
                bearerToken = null,
                type = object : TypeToken<ApiListEnvelope<ShiftDto>>() {}.type
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is MarketplaceError.Unexpected)
        }
    }

    @Test
    fun getReturnsNetworkFailureOnConnectionError() {
        val result = MarketplaceApiClient().get<ApiListEnvelope<ShiftDto>>(
            baseUrl = "http://127.0.0.1:1/api",
            path = "/shifts",
            bearerToken = null,
            type = object : TypeToken<ApiListEnvelope<ShiftDto>>() {}.type
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MarketplaceError.Network)
    }

    @Test
    fun postSendsRequestAndMapsItemEnvelope() {
        testServer(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"item":{"id":"app_1","shiftId":"shift_1","workerId":"worker_1","status":"pending"}}""")
        ) { baseUrl, server ->
            val result = MarketplaceApiClient().post<ApiItemEnvelope<Any>>(
                baseUrl = baseUrl,
                path = "/applications",
                bearerToken = "sess_1",
                request = CreateApplicationRequest(shiftId = "shift_1"),
                type = object : TypeToken<ApiItemEnvelope<Any>>() {}.type
            )

            assertTrue(result.isSuccess)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/applications", request.path)
            assertEquals("Bearer sess_1", request.getHeader("Authorization"))
            assertEquals("""{"shiftId":"shift_1"}""", request.body.readUtf8())
        }
    }

    private fun testServer(response: MockResponse, testBlock: (baseUrl: String, server: MockWebServer) -> Unit) {
        MockWebServer().use { server ->
            server.enqueue(response)
            val baseUrl = server.url("/").toString().removeSuffix("/")
            testBlock(baseUrl, server)
        }
    }
}
