package com.povarup

import com.povarup.data.MarketplaceApiClient
import com.povarup.data.MarketplaceError
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceApiClientTest {
    @Test
    fun fetchShiftsParsesSuccessfulItemsEnvelope() {
        testServer(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"items":[{"id":"shift_1","businessId":"biz_1","locationId":"loc_1","title":"Cook","startAt":"2026-04-10T10:00:00Z","endAt":"2026-04-10T14:00:00Z","payRateCents":2500,"status":"open"}]}""")
        ) { baseUrl ->
            val result = MarketplaceApiClient().fetchShifts(baseUrl)

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().items.size)
            assertEquals("shift_1", result.getOrThrow().items.first().id)
        }
    }

    @Test
    fun fetchShiftsParsesErrorEnvelopeForNon2xxResponse() {
        testServer(
            MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":{"code":"forbidden","message":"Not allowed","details":"role mismatch"}}""")
        ) { baseUrl ->
            val result = MarketplaceApiClient().fetchShifts(baseUrl)

            assertTrue(result.isSuccess)
            val envelope = result.getOrThrow()
            assertEquals("forbidden", envelope.error?.code)
            assertEquals("Not allowed", envelope.error?.message)
        }
    }

    @Test
    fun fetchShiftsReturnsUnexpectedFailureForMalformedJson() {
        testServer(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"items\":[")
        ) { baseUrl ->
            val result = MarketplaceApiClient().fetchShifts(baseUrl)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is MarketplaceError.Unexpected)
        }
    }

    @Test
    fun fetchShiftsReturnsNetworkFailureOnConnectionError() {
        val result = MarketplaceApiClient().fetchShifts("http://127.0.0.1:1/api")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MarketplaceError.Network)
    }

    private fun testServer(response: MockResponse, testBlock: (baseUrl: String) -> Unit) {
        MockWebServer().use { server ->
            server.enqueue(response)
            val baseUrl = server.url("/api/").toString().removeSuffix("/")
            testBlock(baseUrl)
        }
    }
}
