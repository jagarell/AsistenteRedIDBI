package com.upc.asistenteredidbi.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.upc.asistenteredidbi.domain.model.Role
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
private val KEY_ROLE = stringPreferencesKey("role")
private val KEY_USER_ID = longPreferencesKey("user_id")
private val KEY_FULL_NAME = stringPreferencesKey("full_name")

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

    val roleFlow: Flow<Role> =
        context.dataStore.data.map { prefs ->
            Role.fromString(prefs[KEY_ROLE])
        }

    val isSupervisorFlow: Flow<Boolean> =
        roleFlow.map { it == Role.SUPERVISOR }

    val userIdFlow: Flow<Long?> =
        context.dataStore.data.map { prefs -> prefs[KEY_USER_ID] }

    val fullNameFlow: Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[KEY_FULL_NAME] }

    /**
     * Guarda únicamente el JWT.
     */
    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = token
        }
    }

    /**
     * Guarda la sesión completa devuelta por el Login, incluido el rol: es la
     * base del gating de UI (por ejemplo, la acción "Validar" de una minuta
     * sólo debe mostrarse si el rol persistido es SUPERVISOR).
     */
    suspend fun saveSession(
        accessToken: String,
        expiresInMinutes: Int,
        userId: Long,
        fullName: String,
        role: Role
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_EXPIRES_IN_MINUTES] = expiresInMinutes
            prefs[KEY_USER_ID] = userId
            prefs[KEY_FULL_NAME] = fullName
            prefs[KEY_ROLE] = role.name
        }
    }

    suspend fun getAccessToken(): String? =
        accessTokenFlow.first()

    suspend fun getExpiresInMinutes(): Int =
        expiresInMinutesFlow.first()

    suspend fun getRole(): Role =
        roleFlow.first()

    suspend fun isSupervisor(): Boolean =
        getRole() == Role.SUPERVISOR

    suspend fun getUserId(): Long? =
        userIdFlow.first()

    suspend fun isLoggedIn(): Boolean =
        !getAccessToken().isNullOrBlank()

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_EXPIRES_IN_MINUTES)
            prefs.remove(KEY_ROLE)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_FULL_NAME)
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