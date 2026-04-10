package com.povarup.core

import com.povarup.BuildConfig

object NetworkConfig {
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:4000/api"

    fun baseUrl(overrideBaseUrl: String? = null): String {
        val configured = overrideBaseUrl ?: BuildConfig.BACKEND_BASE_URL
        return normalizeBaseUrl(configured.ifBlank { DEFAULT_BASE_URL })
    }

    fun normalizeBaseUrl(url: String): String = url.trim().removeSuffix("/")
}
