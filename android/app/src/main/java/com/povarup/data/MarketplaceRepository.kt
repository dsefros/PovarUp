package com.povarup.data

interface MarketplaceRepository {
    fun currentRole(): String
    fun setRole(role: String)
    fun baseUrl(): String
}

class InMemoryMarketplaceRepository : MarketplaceRepository {
    private var role: String = "worker"

    override fun currentRole(): String = role
    override fun setRole(role: String) {
        this.role = role
    }

    override fun baseUrl(): String = "http://10.0.2.2:4000"
}
