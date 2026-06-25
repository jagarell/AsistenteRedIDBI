package com.upc.asistenteredidbi.presentation.evidence

data class EvidenceItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val captured: Boolean = false
)