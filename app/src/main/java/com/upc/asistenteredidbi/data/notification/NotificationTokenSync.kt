package com.upc.asistenteredidbi.data.notification

import com.google.firebase.messaging.FirebaseMessaging
import com.upc.asistenteredidbi.data.remote.NotificationApiService
import com.upc.asistenteredidbi.data.remote.dto.DeviceTokenRequestDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registra el token FCM del dispositivo en el gateway. Se invoca desde
 * MainActivity cuando hay sesión activa y desde PushMessagingService cuando
 * Firebase rota el token. Si el usuario no está autenticado, el gateway
 * responde 401 y el intento se descarta en silencio (se reintenta en el
 * próximo punto de sincronización).
 */
@Singleton
class NotificationTokenSync @Inject constructor(
    private val api: NotificationApiService
) {

    suspend fun syncCurrentToken() {
        // Sin google-services.json (pendiente del proyecto Firebase real),
        // FirebaseApp nunca se inicializa y FirebaseMessaging.getInstance()
        // lanza IllegalStateException de forma síncrona. Se omite en vez de
        // crashear la app.
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }
            .getOrNull() ?: return
        registerToken(token)
    }

    suspend fun registerToken(token: String) {
        runCatching {
            api.registerDeviceToken(DeviceTokenRequestDto(token))
        }
    }
}
