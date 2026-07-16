package com.upc.asistenteredidbi.data.remote.dto

/** Refleja `ProposalPdfRequest` del gateway (`com.upc.idbi.gateway.pdf`). */
data class ProposalPdfRequestDto(
    val establishmentName: String,
    val address: String?,
    val technicianName: String?,
    val score: Int?,
    val summary: String?,
    val recommendations: List<String>,
    val equipment: List<EquipmentLineDto>,
    val topologyText: String?
)

data class EquipmentLineDto(
    val name: String,
    val description: String,
    val quantity: Int
)

/** Refleja `ProposalSendRequest` del gateway — genera el PDF y lo envía adjunto. */
data class ProposalSendRequestDto(
    val to: String,
    val cc: String?,
    val subject: String,
    val message: String,
    val proposal: ProposalPdfRequestDto
)
