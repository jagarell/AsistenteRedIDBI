package com.upc.asistenteredidbi.presentation.historial

data class HistoryItem(
    val id: String,
    val restaurantName: String,
    val location: String,
    val date: String,
    val status: HistoryStatus,
    val progress: Int
)

enum class HistoryStatus(val label: String) {
    COMPLETADO("Completado"),
    BORRADOR("Borrador"),
    ENVIADO("Enviado"),
    EN_ANALISIS("En Análisis")
}