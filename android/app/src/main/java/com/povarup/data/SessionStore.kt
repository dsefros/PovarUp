package com.povarup.data

import android.content.Context

interface SessionStore {
    fun load(): SessionToken?
    fun save(session: SessionToken)
    fun clear()
}

class InMemorySessionStore : SessionStore {
    private var current: SessionToken? = null

    override fun load(): SessionToken? = current

    override fun save(session: SessionToken) {
        current = session
    }

    override fun clear() {
        current = null
    }
}

class SharedPreferencesSessionStore(context: Context) : SessionStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): SessionToken? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val role = prefs.getString(KEY_ROLE, null) ?: return null
        return SessionToken(token = token, userId = userId, role = role)
    }

    override fun save(session: SessionToken) {
        prefs.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_ROLE, session.role)
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "povarup_session"
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_ROLE = "role"
    }
}
