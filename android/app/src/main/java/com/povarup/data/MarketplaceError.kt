package com.povarup.data

sealed class MarketplaceError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data class Api(
        val code: String,
        val apiMessage: String,
        val details: String? = null
    ) : MarketplaceError("$code: $apiMessage")

    data class Network(val throwable: Throwable) : MarketplaceError(
        message = throwable.message ?: "Network request failed",
        cause = throwable
    )

    data class Unexpected(val throwable: Throwable) : MarketplaceError(
        message = throwable.message ?: "Unexpected repository failure",
        cause = throwable
    )
}
