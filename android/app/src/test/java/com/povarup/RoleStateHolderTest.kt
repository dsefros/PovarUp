package com.povarup

import com.povarup.core.RoleStateHolder
import org.junit.Assert.assertEquals
import org.junit.Test

class RoleStateHolderTest {
    @Test
    fun switchRoleUpdatesState() {
        val holder = RoleStateHolder()
        holder.switch("business")
        val state = holder.current()

        assertEquals("business", state.role)
        assertEquals("http://10.0.2.2:4000", state.baseUrl)
    }
}
