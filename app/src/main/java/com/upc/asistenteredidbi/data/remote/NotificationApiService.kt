package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.DeviceTokenRequestDto
import retrofit2.http.Body
import com.upc.asistenteredidbi.data.remote.dto.NotificationsResponseDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

/** Servicio Retrofit — refleja `DeviceTokenController` del gateway (`/api/notifications/device-token`). */
interface NotificationApiService {

    @PUT("api/notifications/device-token")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRequestDto)

    /** Bandeja del usuario (últimas 50) y cuántas faltan por leer. */
    @GET("api/notifications")
    suspend fun listNotifications(): NotificationsResponseDto

    /** Bandeja por rol: minutas con puntaje y estado (filtros, orden y acciones) y «Otros avisos». */
    @GET("api/notifications/bandeja")
    suspend fun bandeja(): Map<String, @JvmSuppressWildcards Any?>

    /** Recordatorio del supervisor o administrador al técnico (terminar, solicitar, corregir) o, el administrador, al supervisor. */
    @POST("api/notifications/recordatorio")
    suspend fun recordatorio(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/notifications/read-all")
    suspend fun markAllRead()
}
