package com.upc.asistenteredidbi.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Refleja el body que acepta `POST /api/evaluations` en el gateway (Evaluation.java): todo opcional. */
@JsonClass(generateAdapter = true)
data class StartEvaluationRequestDto(
    val restaurantName: String?,
    val address: String?
)

/** Refleja la entidad `Evaluation` del gateway tal cual la serializa Jackson (camelCase, sin prefijo /v1). */
@JsonClass(generateAdapter = true)
data class EvaluationDto(
    val id: Long,
    val restaurantName: String,
    val address: String?,
    val status: String,
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class EvaluationListItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "establishment_name") val establishmentName: String,
    @Json(name = "client_name") val clientName: String?,
    @Json(name = "status") val status: String,
    @Json(name = "overall_score") val overallScore: Float?,
    @Json(name = "created_at") val createdAt: String
)

data class AnalyzeAnswersRequestDto(
    val answers: Map<String, String>
)

data class AnalysisResponseDto(
    val globalScore: Int,
    val evaluatedAreas: Int,
    val attentionRequired: Int,
    val results: List<AnalysisItemDto>,
    val summary: String,
    val asIsFindings: List<String> = emptyList(),
    val recommendations: List<String>
)

data class AnalysisItemDto(
    val title: String,
    val status: String,
    val score: Int,
    val description: String,
    val color: String
)