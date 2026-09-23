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

/** Mismo shape real que EvaluationDto (ambos vienen del mismo Evaluation.java
 *  del gateway) — hasta hace poco tenía nombres @Json en snake_case que no
 *  coincidían con nada real (contrato nunca probado, la ruta que la usaba
 *  daba 404 antes de llegar siquiera a deserializar esto). El gateway no
 *  tiene concepto de "cliente" separado del establecimiento. */
data class EvaluationListItemDto(
    val id: Long,
    val restaurantName: String,
    val status: String,
    val score: Int?,
    val createdAt: String
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