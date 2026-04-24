package com.povarup.data

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.povarup.core.NetworkConfig
import com.povarup.domain.Application
import com.povarup.domain.Assignment
import com.povarup.domain.Payout
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
    fun login(userId: String, password: String): Result<SessionToken>
    fun logout(): Result<Unit>
    fun clearSession()
    fun listShifts(): Result<List<Shift>>
    fun getShift(shiftId: String): Result<Shift>
    fun applyToShift(shiftId: String): Result<Application>
    fun listApplications(): Result<List<Application>>
    fun withdrawApplication(applicationId: String): Result<Application>
    fun rejectApplication(applicationId: String): Result<Application>
    fun listAssignments(): Result<List<Assignment>>
    fun getAssignment(assignmentId: String): Result<Assignment>
    fun acceptAssignment(assignmentId: String): Result<Assignment>
    fun checkIn(assignmentId: String): Result<Unit>
    fun checkOut(assignmentId: String): Result<Unit>
    fun createShift(input: CreateShiftRequest): Result<Shift>
    fun listBusinessShifts(): Result<List<Shift>>
    fun listShiftApplications(shiftId: String): Result<List<Application>>
    fun offerAssignment(applicationId: String): Result<Assignment>
    fun publishShift(shiftId: String): Result<Shift>
    fun closeShift(shiftId: String): Result<Shift>
    fun cancelShift(shiftId: String): Result<Shift>
    fun cancelAssignment(assignmentId: String): Result<Assignment>
    fun releasePayout(assignmentId: String): Result<Payout>
    fun listMyPayouts(): Result<List<Payout>>
    fun listAdminAssignments(): Result<List<Assignment>>
    fun listAdminPayouts(): Result<List<Payout>>
    fun updateAdminPayoutStatus(payoutId: String, status: String, note: String? = null): Result<Payout>
    fun getAdminProblemCases(): Result<ProblemCasesDto>
}

interface MarketplaceApi {
    fun <T> get(baseUrl: String, path: String, bearerToken: String?, type: java.lang.reflect.Type): Result<T>
    fun <T> post(baseUrl: String, path: String, bearerToken: String?, request: Any?, type: java.lang.reflect.Type): Result<T>
}

class MarketplaceApiClient(private val gson: Gson = Gson()) : MarketplaceApi {
    override fun <T> get(baseUrl: String, path: String, bearerToken: String?, type: java.lang.reflect.Type): Result<T> =
        request(baseUrl, path, "GET", bearerToken, null, type)

    override fun <T> post(baseUrl: String, path: String, bearerToken: String?, request: Any?, type: java.lang.reflect.Type): Result<T> =
        request(baseUrl, path, "POST", bearerToken, request?.let { gson.toJson(it) }, type)

    private fun <T> request(
        baseUrl: String,
        path: String,
        method: String,
        bearerToken: String?,
        payload: String?,
        type: java.lang.reflect.Type
    ): Result<T> = runCatching {
        val endpoint = "$baseUrl$path"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "application/json")
            if (!bearerToken.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $bearerToken")
            if (payload != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (payload != null) connection.outputStream.use { it.write(payload.toByteArray()) }
            val code = connection.responseCode
            val body = readResponseBody(connection, code)
            if (code !in 200..299) {
                val parsedMessage = runCatching {
                    gson.fromJson(body.orEmpty(), ApiItemEnvelope::class.java)?.error?.message
                }.getOrNull()
                throw MarketplaceError.Api(code = "http_error", apiMessage = parsedMessage ?: body ?: "HTTP $code")
            }
            @Suppress("UNCHECKED_CAST")
            gson.fromJson<Any>(body.orEmpty(), type) as T
        } catch (ioe: IOException) {
            throw MarketplaceError.Network(ioe)
        } catch (jsonError: JsonParseException) {
            throw MarketplaceError.Unexpected(jsonError)
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponseBody(connection: HttpURLConnection, code: Int): String? {
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        return stream?.use { BufferedReader(InputStreamReader(it)).readText() }
    }
}

class ApiMarketplaceRepository(
    private val api: MarketplaceApi = MarketplaceApiClient(),
    private val baseUrlProvider: () -> String = { NetworkConfig.baseUrl() },
    private val sessionStore: SessionStore = InMemorySessionStore()
) : MarketplaceRepository {
    private var role: String = sessionStore.load()?.role ?: "worker"
    private fun token(): String? = currentSession()?.token
    private fun <T> withRetry(block: () -> Result<T>): Result<T> {
        val first = block()
        if (first.isSuccess) return first
        val err = first.exceptionOrNull()
        return if (err is MarketplaceError.Network) block() else first
    }

    override fun currentRole(): String = role
    override fun setRole(role: String) { this.role = role }
    override fun baseUrl(): String = baseUrlProvider()
    override fun currentSession(): SessionToken? = sessionStore.load()

    override fun login(userId: String, password: String): Result<SessionToken> {
        val t = object : TypeToken<SessionDto>() {}.type
        val response = api.post<SessionDto>(baseUrl(), "/auth/login", null, LoginRequest(userId, password), t)
        return response.map { it.toDomain().also { session -> sessionStore.save(session); setRole(session.role) } }
    }

    override fun logout(): Result<Unit> =
        api.post<Map<String, Any>>(baseUrl(), "/auth/logout", token(), null, object : TypeToken<Map<String, Any>>() {}.type)
            .map { sessionStore.clear() }

    override fun clearSession() { sessionStore.clear() }

    override fun listShifts(): Result<List<Shift>> =
        api.get<ApiListEnvelope<ShiftDto>>(baseUrl(), "/shifts", token(), object : TypeToken<ApiListEnvelope<ShiftDto>>() {}.type)
            .map { it.items.map { dto -> dto.toDomain() } }

    override fun getShift(shiftId: String): Result<Shift> =
        api.get<ApiItemEnvelope<ShiftDto>>(baseUrl(), "/shifts/$shiftId", token(), object : TypeToken<ApiItemEnvelope<ShiftDto>>() {}.type)
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing shift") }

    override fun applyToShift(shiftId: String): Result<Application> =
        withRetry { api.post<ApiItemEnvelope<ApplicationDto>>(baseUrl(), "/applications", token(), CreateApplicationRequest(shiftId), object : TypeToken<ApiItemEnvelope<ApplicationDto>>() {}.type) }
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing application") }

    override fun listApplications(): Result<List<Application>> =
        api.get<ApiListEnvelope<ApplicationDto>>(baseUrl(), "/applications", token(), object : TypeToken<ApiListEnvelope<ApplicationDto>>() {}.type)
            .map { it.items.map { dto -> dto.toDomain() } }

    override fun withdrawApplication(applicationId: String): Result<Application> =
        withRetry { api.post<ApiItemEnvelope<ApplicationDto>>(baseUrl(), "/applications/$applicationId/withdraw", token(), null, object : TypeToken<ApiItemEnvelope<ApplicationDto>>() {}.type) }
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing application") }

    override fun rejectApplication(applicationId: String): Result<Application> =
        withRetry { api.post<ApiItemEnvelope<ApplicationDto>>(baseUrl(), "/applications/$applicationId/reject", token(), null, object : TypeToken<ApiItemEnvelope<ApplicationDto>>() {}.type) }
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing application") }

    override fun listAssignments(): Result<List<Assignment>> =
        api.get<ApiListEnvelope<AssignmentDto>>(baseUrl(), "/assignments", token(), object : TypeToken<ApiListEnvelope<AssignmentDto>>() {}.type)
            .map { it.items.map { dto -> dto.toDomain() } }

    override fun getAssignment(assignmentId: String): Result<Assignment> =
        api.get<ApiItemEnvelope<AssignmentDto>>(baseUrl(), "/assignments/$assignmentId", token(), object : TypeToken<ApiItemEnvelope<AssignmentDto>>() {}.type)
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing assignment") }

    override fun acceptAssignment(assignmentId: String): Result<Assignment> =
        api.post<ApiItemEnvelope<AssignmentDto>>(baseUrl(), "/assignments/$assignmentId/accept", token(), null, object : TypeToken<ApiItemEnvelope<AssignmentDto>>() {}.type)
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing assignment") }

    override fun checkIn(assignmentId: String): Result<Unit> =
        api.post<ApiItemEnvelope<Any>>(baseUrl(), "/attendance/check-in", token(), AttendanceRequest(assignmentId), object : TypeToken<ApiItemEnvelope<Any>>() {}.type).map { }

    override fun checkOut(assignmentId: String): Result<Unit> =
        api.post<ApiItemEnvelope<Any>>(baseUrl(), "/attendance/check-out", token(), AttendanceRequest(assignmentId), object : TypeToken<ApiItemEnvelope<Any>>() {}.type).map { }

    override fun createShift(input: CreateShiftRequest): Result<Shift> =
        api.post<ApiItemEnvelope<ShiftDto>>(baseUrl(), "/shifts", token(), input, object : TypeToken<ApiItemEnvelope<ShiftDto>>() {}.type)
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing shift") }

    override fun listBusinessShifts(): Result<List<Shift>> =
        api.get<ApiListEnvelope<ShiftDto>>(baseUrl(), "/business/shifts", token(), object : TypeToken<ApiListEnvelope<ShiftDto>>() {}.type)
            .map { it.items.map { dto -> dto.toDomain() } }

    override fun listShiftApplications(shiftId: String): Result<List<Application>> =
        api.get<ApiListEnvelope<ApplicationDto>>(baseUrl(), "/business/shifts/$shiftId/applications", token(), object : TypeToken<ApiListEnvelope<ApplicationDto>>() {}.type)
            .map { it.items.map { dto -> dto.toDomain() } }

    override fun offerAssignment(applicationId: String): Result<Assignment> =
        withRetry { api.post<ApiItemEnvelope<AssignmentDto>>(baseUrl(), "/assignments/offer", token(), OfferAssignmentRequest(applicationId), object : TypeToken<ApiItemEnvelope<AssignmentDto>>() {}.type) }
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing assignment") }

    override fun publishShift(shiftId: String): Result<Shift> =
        withRetry { api.post<ApiItemEnvelope<ShiftDto>>(baseUrl(), "/shifts/$shiftId/publish", token(), null, object : TypeToken<ApiItemEnvelope<ShiftDto>>() {}.type) }
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing shift") }

    override fun closeShift(shiftId: String): Result<Shift> =
        withRetry { api.post<ApiItemEnvelope<ShiftDto>>(baseUrl(), "/shifts/$shiftId/close", token(), null, object : TypeToken<ApiItemEnvelope<ShiftDto>>() {}.type) }
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing shift") }

    override fun cancelShift(shiftId: String): Result<Shift> =
        withRetry { api.post<ApiItemEnvelope<ShiftDto>>(baseUrl(), "/shifts/$shiftId/cancel", token(), null, object : TypeToken<ApiItemEnvelope<ShiftDto>>() {}.type) }
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing shift") }

    override fun cancelAssignment(assignmentId: String): Result<Assignment> =
        withRetry { api.post<ApiItemEnvelope<AssignmentDto>>(baseUrl(), "/assignments/$assignmentId/cancel", token(), null, object : TypeToken<ApiItemEnvelope<AssignmentDto>>() {}.type) }
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing assignment") }

    override fun releasePayout(assignmentId: String): Result<Payout> =
        api.post<ApiItemEnvelope<PayoutDto>>(baseUrl(), "/escrow/release/$assignmentId", token(), ReleasePayoutRequest(false), object : TypeToken<ApiItemEnvelope<PayoutDto>>() {}.type)
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing payout") }

    override fun listMyPayouts(): Result<List<Payout>> =
        api.get<ApiListEnvelope<PayoutDto>>(baseUrl(), "/me/payouts", token(), object : TypeToken<ApiListEnvelope<PayoutDto>>() {}.type)
            .map { it.items.map { dto -> dto.toDomain() } }

    override fun listAdminAssignments(): Result<List<Assignment>> =
        api.get<ApiListEnvelope<AssignmentDto>>(baseUrl(), "/admin/assignments", token(), object : TypeToken<ApiListEnvelope<AssignmentDto>>() {}.type)
            .map { it.items.map { dto -> dto.toDomain() } }

    override fun listAdminPayouts(): Result<List<Payout>> =
        api.get<ApiListEnvelope<PayoutDto>>(baseUrl(), "/admin/payouts", token(), object : TypeToken<ApiListEnvelope<PayoutDto>>() {}.type)
            .map { it.items.map { dto -> dto.toDomain() } }

    override fun updateAdminPayoutStatus(payoutId: String, status: String, note: String?): Result<Payout> =
        api.post<ApiItemEnvelope<PayoutDto>>(baseUrl(), "/admin/payouts/$payoutId/status", token(), UpdatePayoutStatusRequest(status, note), object : TypeToken<ApiItemEnvelope<PayoutDto>>() {}.type)
            .map { it.item?.toDomain() ?: throw IllegalStateException("Missing payout") }

    override fun getAdminProblemCases(): Result<ProblemCasesDto> =
        api.get<ApiItemEnvelope<ProblemCasesDto>>(baseUrl(), "/admin/problem-cases", token(), object : TypeToken<ApiItemEnvelope<ProblemCasesDto>>() {}.type)
            .map { it.item ?: ProblemCasesDto() }
}
