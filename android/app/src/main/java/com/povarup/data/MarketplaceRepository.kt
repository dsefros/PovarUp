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
    fun currentSession(): SessionToken?
    fun createSession(userId: String, role: String): Result<SessionToken>
    fun clearSession()
    fun listShifts(): Result<List<Shift>>
}

class InMemoryMarketplaceRepository(
    private val sessionStore: SessionStore = InMemorySessionStore()
) : MarketplaceRepository {
    private var role: String = "worker"

    override fun currentRole(): String = role

    override fun setRole(role: String) {
        this.role = role
    }

    override fun baseUrl(): String = NetworkConfig.baseUrl()

    override fun currentSession(): SessionToken? = sessionStore.load()

    override fun createSession(userId: String, role: String): Result<SessionToken> {
        val session = SessionToken(token = "in_memory_session", userId = userId, role = role)
        sessionStore.save(session)
        setRole(role)
        return Result.success(session)
    }

    override fun clearSession() {
        sessionStore.clear()
    }

    override fun listShifts(): Result<List<Shift>> = Result.success(emptyList())
}

interface MarketplaceApi {
    fun fetchShifts(baseUrl: String, bearerToken: String?): Result<ApiListEnvelope<ShiftDto>>
    fun createSession(baseUrl: String, request: CreateSessionRequest): Result<ApiItemEnvelope<SessionDto>>
}

class MarketplaceApiClient(private val gson: Gson = Gson()) : MarketplaceApi {
    override fun fetchShifts(baseUrl: String, bearerToken: String?): Result<ApiListEnvelope<ShiftDto>> = runCatching {
        val endpoint = "${baseUrl}/shifts"
        val connection = buildConnection(endpoint = endpoint, method = "GET", bearerToken = bearerToken)

        try {
            val code = connection.responseCode
            val body = readResponseBody(connection, code)
            if (code in 200..299) {
                gson.fromJson(body.orEmpty(), ShiftListResponse::class.java).toEnvelope()
            } else {
                ApiListEnvelope(error = parseApiError(body, code))
            }
        } catch (ioe: IOException) {
            throw MarketplaceError.Network(ioe)
        } catch (jsonError: JsonParseException) {
            throw MarketplaceError.Unexpected(jsonError)
        } finally {
            connection.disconnect()
        }
    }

    override fun createSession(baseUrl: String, request: CreateSessionRequest): Result<ApiItemEnvelope<SessionDto>> = runCatching {
        val endpoint = "${baseUrl}/auth/session"
        val connection = buildConnection(endpoint = endpoint, method = "POST", requestBody = gson.toJson(request))

        try {
            writeRequestBody(connection, gson.toJson(request))

            val code = connection.responseCode
            val body = readResponseBody(connection, code)
            if (code in 200..299) {
                ApiItemEnvelope(item = gson.fromJson(body.orEmpty(), SessionDto::class.java))
            } else {
                ApiItemEnvelope(error = parseApiError(body, code))
            }
        } catch (ioe: IOException) {
            throw MarketplaceError.Network(ioe)
        } catch (jsonError: JsonParseException) {
            throw MarketplaceError.Unexpected(jsonError)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildConnection(
        endpoint: String,
        method: String,
        bearerToken: String? = null,
        requestBody: String? = null
    ): HttpURLConnection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 5_000
        readTimeout = 5_000
        setRequestProperty("Accept", "application/json")
        if (!bearerToken.isNullOrBlank()) {
            setRequestProperty("Authorization", "Bearer $bearerToken")
        }
        if (requestBody != null) {
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }
    }

    private fun writeRequestBody(connection: HttpURLConnection, body: String) {
        connection.outputStream.use { output ->
            output.write(body.toByteArray())
        }
    }

    private fun readResponseBody(connection: HttpURLConnection, code: Int): String? {
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        return stream?.use { input ->
            BufferedReader(InputStreamReader(input)).readText()
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
    private val baseUrlProvider: () -> String = { NetworkConfig.baseUrl() },
    private val sessionStore: SessionStore = InMemorySessionStore()
) : MarketplaceRepository {
    private var role: String = sessionStore.load()?.role ?: "worker"

    override fun currentRole(): String = role

    override fun setRole(role: String) {
        this.role = role
    }

    override fun baseUrl(): String = baseUrlProvider()

    override fun currentSession(): SessionToken? = sessionStore.load()

    override fun createSession(userId: String, role: String): Result<SessionToken> {
        val apiResult = api.createSession(baseUrl(), CreateSessionRequest(userId = userId, role = role))
        if (apiResult.isFailure) {
            val throwable = apiResult.exceptionOrNull() ?: MarketplaceError.Unexpected(Exception("Unknown API error"))
            return Result.failure(throwable)
        }

        val response = apiResult.getOrThrow()
        response.error?.let {
            return Result.failure(MarketplaceError.Api(code = it.code, apiMessage = it.message, details = it.details))
        }

        val session = response.item?.toDomain()
            ?: return Result.failure(MarketplaceError.Unexpected(IllegalStateException("Session payload missing")))

        sessionStore.save(session)
        setRole(session.role)
        return Result.success(session)
    }

    override fun clearSession() {
        sessionStore.clear()
    }

    override fun listShifts(): Result<List<Shift>> {
        val apiResult = api.fetchShifts(baseUrl(), currentSession()?.token)
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
