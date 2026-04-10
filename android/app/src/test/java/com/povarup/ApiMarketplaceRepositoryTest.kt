package com.povarup

import com.povarup.data.ApiError
import com.povarup.data.ApiListEnvelope
import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.MarketplaceApi
import com.povarup.data.MarketplaceError
import com.povarup.data.ShiftDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ApiMarketplaceRepositoryTest {
    @Test
    fun listShiftsMapsItemsToDomain() {
        val api = object : MarketplaceApi {
            override fun fetchShifts(baseUrl: String): Result<ApiListEnvelope<ShiftDto>> = Result.success(
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
        }

        val repository = ApiMarketplaceRepository(api = api, baseUrlProvider = { "http://localhost:4000/api" })
        val result = repository.listShifts()

        assertTrue(result.isSuccess)
        assertEquals("Prep Cook", result.getOrThrow().first().title)
    }

    @Test
    fun listShiftsReturnsTypedApiFailureWhenApiReturnsErrorEnvelope() {
        val api = object : MarketplaceApi {
            override fun fetchShifts(baseUrl: String): Result<ApiListEnvelope<ShiftDto>> = Result.success(
                ApiListEnvelope(error = ApiError(code = "forbidden", message = "Not authorized"))
            )
        }

        val repository = ApiMarketplaceRepository(api = api)
        val result = repository.listShifts()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MarketplaceError.Api)
    }

    @Test
    fun listShiftsPassesThroughNetworkFailure() {
        val api = object : MarketplaceApi {
            override fun fetchShifts(baseUrl: String): Result<ApiListEnvelope<ShiftDto>> =
                Result.failure(MarketplaceError.Network(IOException("timeout")))
        }

        val repository = ApiMarketplaceRepository(api = api)
        val result = repository.listShifts()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MarketplaceError.Network)
    }
}
