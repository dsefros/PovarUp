package com.povarup

import com.povarup.data.InMemorySessionStore
import com.povarup.data.SessionToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionStoreTest {
    @Test
    fun inMemoryStorePersistsAndClearsSession() {
        val store = InMemorySessionStore()

        store.save(SessionToken(token = "sess_1", userId = "u_worker_1", role = "worker"))
        assertEquals("sess_1", store.load()?.token)

        store.clear()
        assertNull(store.load())
    }
}
