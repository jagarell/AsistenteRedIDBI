package com.upc.asistenteredidbi.presentation.common

private val MONTHS = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

/** "2026-05-31T10:15:00…" → "31 May 2026". Si no se reconoce el formato, devuelve el texto tal cual. */
fun formatShortDate(iso: String?): String {
    val match = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(iso.orEmpty()) ?: return iso.orEmpty()
    val (year, month, day) = match.destructured
    val name = MONTHS.getOrNull(month.toInt() - 1) ?: return iso.orEmpty()
    return "${day.toInt()} $name $year"
}
