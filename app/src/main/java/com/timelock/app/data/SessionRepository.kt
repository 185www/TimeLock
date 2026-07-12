package com.timelock.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "timelock")

/**
 * Stores the set of monitored apps and the currently active usage session.
 *
 * A session is intentionally kept simple: which app is being timed and until
 * when (epoch millis). Everything is local — nothing leaves the device.
 */
class SessionRepository(private val context: Context) {

    private val monitoredKey = stringSetPreferencesKey("monitored_apps")
    private val sessionPackageKey = stringSetPreferencesKey("session_package")
    private val sessionEndKey = longPreferencesKey("session_end_time")

    val monitoredApps: Flow<Set<String>> =
        context.dataStore.data.map { it[monitoredKey] ?: emptySet() }

    val session: Flow<Session?> = context.dataStore.data.map { prefs ->
        val pkg = prefs[sessionPackageKey]?.firstOrNull()
        val end = prefs[sessionEndKey] ?: 0L
        if (pkg != null && end > 0L) Session(pkg, end) else null
    }

    suspend fun setMonitoredApps(packages: Set<String>) {
        context.dataStore.edit { it[monitoredKey] = packages }
    }

    suspend fun startSession(packageName: String, endTime: Long) {
        context.dataStore.edit {
            it[sessionPackageKey] = setOf(packageName)
            it[sessionEndKey] = endTime
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(sessionPackageKey)
            it.remove(sessionEndKey)
        }
    }

    suspend fun getMonitoredApps(): Set<String> =
        context.dataStore.data.map { it[monitoredKey] ?: emptySet() }.firstOrNull()
            ?: emptySet()

    suspend fun getSession(): Session? {
        val prefs = context.dataStore.data.map { it }.firstOrNull() ?: return null
        val pkg = prefs[sessionPackageKey]?.firstOrNull() ?: return null
        val end = prefs[sessionEndKey] ?: 0L
        return if (end > 0L) Session(pkg, end) else null
    }

    data class Session(val packageName: String, val endTime: Long)
}
