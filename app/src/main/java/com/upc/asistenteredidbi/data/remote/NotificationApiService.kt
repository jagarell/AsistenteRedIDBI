package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.DeviceTokenRequestDto
import retrofit2.http.Body
import retrofit2.http.PUT

/** Servicio Retrofit — refleja `DeviceTokenController` del gateway (`/api/notifications/device-token`). */
interface NotificationApiService {

    @PUT("api/notifications/device-token")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRequestDto)
}
