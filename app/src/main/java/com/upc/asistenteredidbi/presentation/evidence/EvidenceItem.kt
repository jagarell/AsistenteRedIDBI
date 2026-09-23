package com.upc.asistenteredidbi.presentation.evidence

data class EvidenceItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val captured: Boolean = false,
    /** URL relativa de la primera foto capturada (ej. "/uploads/evidence/..."),
     *  para mostrar una miniatura real en vez de solo el ícono genérico. */
    val photoUrl: String? = null
)