package com.upc.asistenteredidbi.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "network_assistant_session")

private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
private val KEY_EXPIRES_IN_MINUTES = intPreferencesKey("expires_in_minutes")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val accessTokenFlow: Flow<String?> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_ACCESS_TOKEN]
        }

    val expiresInMinutesFlow: Flow<Int> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_EXPIRES_IN_MINUTES] ?: 0
        }

    /**
     * Guarda únicamente el JWT.
     */
    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = token
        }
    }

    /**
     * Guarda la sesión completa devuelta por el Login.
     */
    suspend fun saveSession(
        accessToken: String,
        expiresInMinutes: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_EXPIRES_IN_MINUTES] = expiresInMinutes
        }
    }

    suspend fun getAccessToken(): String? =
        accessTokenFlow.first()

    suspend fun getExpiresInMinutes(): Int =
        expiresInMinutesFlow.first()

    suspend fun isLoggedIn(): Boolean =
        !getAccessToken().isNullOrBlank()

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_EXPIRES_IN_MINUTES)
        }
    }

    /**
     * Usado por el Interceptor de OkHttp.
     */
    fun getJwtTokenBlocking(): String? =
        runBlocking {
            getAccessToken()
        }
}