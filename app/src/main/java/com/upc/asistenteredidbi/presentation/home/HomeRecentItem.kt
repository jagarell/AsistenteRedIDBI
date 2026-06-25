package com.upc.asistenteredidbi.presentation.home

data class HomeRecentItem(
    val title: String,
    val date: String,
    val status: String,
    val statusType: StatusType
)

enum class StatusType {
    COMPLETADO,
    BORRADOR,
    ENVIADO
}