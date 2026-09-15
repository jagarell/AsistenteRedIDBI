package com.upc.asistenteredidbi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.upc.asistenteredidbi.data.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AsistenteRedApp : Application() {

    override fun onCreate() {
        super.onCreate()
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