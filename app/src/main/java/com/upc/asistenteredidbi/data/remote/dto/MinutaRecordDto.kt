package com.upc.asistenteredidbi.data.remote.dto

/**
 * DTOs de la minuta técnica — reflejan `MinutaRequest`/`MinutaResponse` del
 * gateway (`com.upc.idbi.gateway.minuta.dto`). Nombrados "Record" para no
 * chocar con [MinutaDto] (vista consolidada de solo lectura de una evaluación).
 */
data class MinutaRecordRequestDto(
    val evaluationId: Long? = null,
    val clientName: String,
    val address: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val summary: String? = null,
    val topologyJson: String? = null,
    val contentJson: String? = null
)

data class MinutaRecordResponseDto(
    val id: Long,
    val evaluationId: Long?,
    val clientName: String,
    val address: String?,
    val contactName: String?,
    val contactPhone: String?,
    val technicianId: Long?,
    val technicianName: String?,
    val status: String,
    val summary: String?,
    val topologyJson: String?,
    val contentJson: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val validatedById: Long?,
    val validatedByName: String?,
    val validatedAt: String?
)
