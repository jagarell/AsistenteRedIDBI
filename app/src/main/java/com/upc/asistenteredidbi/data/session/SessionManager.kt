package com.upc.asistenteredidbi.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
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

/**
 * Maneja la persistencia del JWT (HU01). Usado por:
 *  - `presentation/auth` (guarda el token al hacer login/registro exitoso).
 *  - `presentation/di/NetworkModule` del Sprint 2 (interceptor que agrega
 *    el header `Authorization: Bearer <token>` a cada request).
 *
 * Se usa DataStore (no SharedPreferences) por ser la recomendación
 * actual de Android Jetpack; para mayor seguridad en producción se
 * recomienda envolverlo con `androidx.security.crypto` (EncryptedFile)
 * o Tink, sin cambiar la interfaz pública de esta clase.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { prefs -> prefs[KEY_ACCESS_TOKEN] }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { prefs -> prefs[KEY_ACCESS_TOKEN] = token }
    }

    suspend fun getAccessToken(): String? = accessTokenFlow.first()

    suspend fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    suspend fun clearSession() {
        context.dataStore.edit { prefs -> prefs.remove(KEY_ACCESS_TOKEN) }
    }

    /**
     * Variante bloqueante, requerida por el `Interceptor` de OkHttp
     * (que es síncrono). Se evita con `runBlocking` solo aquí, en el
     * borde de la capa de red, nunca en ViewModels/UseCases.
     */
    fun getJwtTokenBlocking(): String? = runBlocking { getAccessToken() }
}
