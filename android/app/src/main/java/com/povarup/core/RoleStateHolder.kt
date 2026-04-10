package com.povarup.core

import com.povarup.data.ApiMarketplaceRepository
import com.povarup.data.MarketplaceRepository

data class RoleState(
    val role: String,
    val baseUrl: String
)

class RoleStateHolder(private val repository: MarketplaceRepository = ApiMarketplaceRepository()) {
    fun current(): RoleState = RoleState(role = repository.currentRole(), baseUrl = repository.baseUrl())
    fun switch(role: String) = repository.setRole(role)
}
