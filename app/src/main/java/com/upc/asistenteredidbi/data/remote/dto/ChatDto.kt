package com.upc.asistenteredidbi.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTOs de respuesta individual del chat (usados por el GET de minuta real,
 * `/api/v1/evaluations/{id}/minuta`, ver EvidenceDto.kt). El motor de árbol de
 * decisión que originalmente acompañaba a estos DTOs (nodos/progreso/resumen
 * de cierre) nunca se conectó a ningún backend real y fue retirado.
 */
@JsonClass(generateAdapter = true)
data class ChatResponseExtractedDataDto(
    @Json(name = "extracted_fields") val extractedFields: Map<String, String?>,
    @Json(name = "confidence") val confidence: Float,
    @Json(name = "notes") val notes: String?
)

@JsonClass(generateAdapter = true)
data class ChatResponseDto(
    @Json(name = "node_key") val nodeKey: String,
    @Json(name = "value") val value: String,
    @Json(name = "extracted_data") val extractedData: ChatResponseExtractedDataDto?
)
