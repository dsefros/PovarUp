package com.povarup

import com.povarup.core.RoleStateHolder
import com.povarup.data.InMemoryMarketplaceRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class RoleStateHolderTest {
    @Test
    fun switchRoleUpdatesState() {
        val holder = RoleStateHolder(InMemoryMarketplaceRepository())
        holder.switch("business")
        val state = holder.current()

        assertEquals("business", state.role)
        assertEquals("http://10.0.2.2:4000/api", state.baseUrl)
    }
}
