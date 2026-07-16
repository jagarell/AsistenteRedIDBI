package com.upc.asistenteredidbi.domain.model

/** Datos con los que se genera el PDF real de la propuesta técnica. */
data class ProposalPdfData(
    val establishmentName: String,
    val address: String?,
    val technicianName: String?,
    val score: Int?,
    val summary: String?,
    val recommendations: List<String>,
    val equipment: List<TechnicalEquipmentRecommendation>,
    val topologyText: String?
)
