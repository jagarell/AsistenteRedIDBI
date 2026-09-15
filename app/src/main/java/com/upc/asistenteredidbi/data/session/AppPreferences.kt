package com.upc.asistenteredidbi.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "network_assistant_settings")

private val KEY_PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications")
private val KEY_EMAIL_NOTIFICATIONS = booleanPreferencesKey("email_notifications")
private val KEY_SYSTEM_ALERTS = booleanPreferencesKey("system_alerts")
private val KEY_TWO_FACTOR_AUTH = booleanPreferencesKey("two_factor_auth")

/**
 * Preferencias de UI locales de Configuración (no hay endpoint de backend para
 * esto todavía). Solo recuerda lo que el usuario tocó en este dispositivo,
 * nunca simula una confirmación de servidor.
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val pushNotificationsFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_PUSH_NOTIFICATIONS] ?: true }

    val emailNotificationsFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_EMAIL_NOTIFICATIONS] ?: true }

    val systemAlertsFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_SYSTEM_ALERTS] ?: false }

    val twoFactorAuthFlow: Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_TWO_FACTOR_AUTH] ?: false }

    suspend fun setPushNotifications(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_PUSH_NOTIFICATIONS] = enabled }
    }

    suspend fun setEmailNotifications(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_EMAIL_NOTIFICATIONS] = enabled }
    }

    suspend fun setSystemAlerts(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_SYSTEM_ALERTS] = enabled }
    }

    suspend fun setTwoFactorAuth(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_TWO_FACTOR_AUTH] = enabled }
    }
}
