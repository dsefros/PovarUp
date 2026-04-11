package com.povarup

import com.povarup.core.NetworkConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkConfigTest {
    @Test
    fun trimsTrailingSlash() {
        assertEquals("http://localhost:4000/api", NetworkConfig.baseUrl("http://localhost:4000/api/"))
    }

    @Test
    fun usesFallbackWhenBlank() {
        assertEquals("http://10.0.2.2:4000/api", NetworkConfig.baseUrl("   "))
    }
}
