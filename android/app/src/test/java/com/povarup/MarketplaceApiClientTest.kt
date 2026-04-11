package com.povarup

import com.povarup.data.MarketplaceApiClient
import com.povarup.data.MarketplaceError
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress

class MarketplaceApiClientTest {
    @Test
    fun fetchShiftsParsesSuccessfulItemsEnvelope() {
        val server = testServer(
            status = 200,
            body = """{"items":[{"id":"shift_1","businessId":"biz_1","locationId":"loc_1","title":"Cook","startAt":"2026-04-10T10:00:00Z","endAt":"2026-04-10T14:00:00Z","payRateCents":2500,"status":"open"}]}"""
        )
        try {
            val result = MarketplaceApiClient().fetchShifts(server.baseUrl)

            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().items.size)
            assertEquals("shift_1", result.getOrThrow().items.first().id)
        } finally {
            server.stop()
        }
    }

    @Test
    fun fetchShiftsParsesErrorEnvelopeForNon2xxResponse() {
        val server = testServer(
            status = 403,
            body = """{"error":{"code":"forbidden","message":"Not allowed","details":"role mismatch"}}"""
        )
        try {
            val result = MarketplaceApiClient().fetchShifts(server.baseUrl)

            assertTrue(result.isSuccess)
            val envelope = result.getOrThrow()
            assertEquals("forbidden", envelope.error?.code)
            assertEquals("Not allowed", envelope.error?.message)
        } finally {
            server.stop()
        }
    }

    @Test
    fun fetchShiftsReturnsUnexpectedFailureForMalformedJson() {
        val server = testServer(status = 200, body = "{\"items\":[")
        try {
            val result = MarketplaceApiClient().fetchShifts(server.baseUrl)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is MarketplaceError.Unexpected)
        } finally {
            server.stop()
        }
    }

    @Test
    fun fetchShiftsReturnsNetworkFailureOnConnectionError() {
        val result = MarketplaceApiClient().fetchShifts("http://127.0.0.1:1/api")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MarketplaceError.Network)
    }

    private fun testServer(status: Int, body: String): TestServer {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/api/shifts") { exchange ->
            respond(exchange, status, body)
        }
        server.start()

        val baseUrl = "http://127.0.0.1:${server.address.port}/api"
        return TestServer(server, baseUrl)
    }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { output ->
            output.write(bytes)
        }
    }

    private data class TestServer(val server: HttpServer, val baseUrl: String) {
        fun stop() {
            server.stop(0)
        }
    }
}
