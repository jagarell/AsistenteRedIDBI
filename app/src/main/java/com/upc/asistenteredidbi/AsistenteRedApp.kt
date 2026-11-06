package com.upc.asistenteredidbi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.upc.asistenteredidbi.data.notification.NotificationChannels
import androidx.appcompat.app.AppCompatDelegate
import com.upc.asistenteredidbi.data.session.AppPreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class AsistenteRedApp : Application() {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        // Lectura única y corta antes de mostrar nada, para no parpadear en claro.
        val dark = runCatching { runBlocking { appPreferences.darkMode() } }.getOrNull()
        if (dark != null) {
            AppCompatDelegate.setDefaultNightMode(
                if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        createPushNotificationChannel()
    }

    private fun createPushNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            NotificationChannels.PUSH_CHANNEL_ID,
            getString(R.string.notification_channel_push_name),
            NotificationManager.IMPORTANCE_HIGH
        )

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}