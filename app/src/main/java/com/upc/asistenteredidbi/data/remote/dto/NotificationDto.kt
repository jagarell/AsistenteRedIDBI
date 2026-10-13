package com.upc.asistenteredidbi.data.remote.dto

data class DeviceTokenRequestDto(
    val token: String
)


data class NotificationItemDto(
    val id: Long,
    val title: String,
    val body: String,
    val type: String? = null,
    val minutaId: Long? = null,
    val evaluationId: Long? = null,
    val createdAt: String? = null,
    val read: Boolean = false
)

data class NotificationsResponseDto(
    val items: List<NotificationItemDto> = emptyList(),
    val unread: Int = 0
)
