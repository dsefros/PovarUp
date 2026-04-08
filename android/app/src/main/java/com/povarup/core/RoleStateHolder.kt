package com.povarup.core

import com.povarup.data.InMemoryMarketplaceRepository
import com.povarup.data.MarketplaceRepository

data class RoleState(
    val role: String,
    val baseUrl: String
)

class RoleStateHolder(private val repository: MarketplaceRepository = InMemoryMarketplaceRepository()) {
    fun current(): RoleState = RoleState(role = repository.currentRole(), baseUrl = repository.baseUrl())
    fun switch(role: String) = repository.setRole(role)
}
