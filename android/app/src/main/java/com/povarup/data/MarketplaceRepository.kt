package com.povarup.data

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.povarup.core.NetworkConfig
import com.povarup.domain.Shift
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

interface MarketplaceRepository {
    fun currentRole(): String
    fun setRole(role: String)
    fun baseUrl(): String
    fun listShifts(): Result<List<Shift>>
}

class InMemoryMarketplaceRepository : MarketplaceRepository {
    private var role: String = "worker"

    override fun currentRole(): String = role

    override fun setRole(role: String) {
        this.role = role
    }

    override fun baseUrl(): String = NetworkConfig.baseUrl()

    override fun listShifts(): Result<List<Shift>> = Result.success(emptyList())
}

interface MarketplaceApi {
    fun fetchShifts(baseUrl: String): Result<ApiListEnvelope<ShiftDto>>
}

class MarketplaceApiClient(private val gson: Gson = Gson()) : MarketplaceApi {
    override fun fetchShifts(baseUrl: String): Result<ApiListEnvelope<ShiftDto>> = runCatching {
        val endpoint = "${baseUrl}/shifts"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }

            if (code in 200..299) {
                gson.fromJson(body.orEmpty(), ShiftListResponse::class.java).toEnvelope()
            } else {
                val error = parseApiError(body, code)
                ApiListEnvelope(error = error)
            }
        } catch (ioe: IOException) {
            throw MarketplaceError.Network(ioe)
        } catch (jsonError: JsonParseException) {
            throw MarketplaceError.Unexpected(jsonError)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseApiError(body: String?, code: Int): ApiError {
        if (body.isNullOrBlank()) {
            return ApiError(code = "http_error", message = "Request failed with HTTP $code")
        }
        return try {
            gson.fromJson(body, ErrorResponse::class.java).error
                ?: ApiError(code = "http_error", message = "Request failed with HTTP $code")
        } catch (_: JsonParseException) {
            ApiError(code = "http_error", message = "Request failed with HTTP $code")
        }
    }

    private data class ShiftListResponse(
        val items: List<ShiftDto> = emptyList(),
        val error: ApiError? = null
    ) {
        fun toEnvelope(): ApiListEnvelope<ShiftDto> = ApiListEnvelope(items = items, error = error)
    }

    private data class ErrorResponse(val error: ApiError? = null)
}

class ApiMarketplaceRepository(
    private val api: MarketplaceApi = MarketplaceApiClient(),
    private val baseUrlProvider: () -> String = { NetworkConfig.baseUrl() }
) : MarketplaceRepository {
    private var role: String = "worker"

    override fun currentRole(): String = role

    override fun setRole(role: String) {
        this.role = role
    }

    override fun baseUrl(): String = baseUrlProvider()

    override fun listShifts(): Result<List<Shift>> {
        val apiResult = api.fetchShifts(baseUrl())
        if (apiResult.isFailure) {
            val throwable = apiResult.exceptionOrNull() ?: MarketplaceError.Unexpected(Exception("Unknown API error"))
            return Result.failure(throwable)
        }

        val response = apiResult.getOrThrow()
        response.error?.let {
            return Result.failure(MarketplaceError.Api(code = it.code, apiMessage = it.message, details = it.details))
        }

        return Result.success(response.items.map { it.toDomain() })
    }
}
