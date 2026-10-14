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
    val createdAt: String,
    val address: String? = null,
    /** EN_CURSO, BORRADOR, COMPLETA o VALIDADA: lo deriva el gateway (chat terminado + estado de la minuta). */
    val minutaStatus: String? = null,
    val minutaId: Long? = null,
    val validatedByName: String? = null,
    val technicianName: String? = null,
    val rejectionReason: String? = null
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
    val recommendations: List<String>,
    /** APTO | OBSERVADO | NO APTO */
    val estado: String? = null,
    val recommendationItems: List<AnalysisRecommendationDto> = emptyList(),
    val steps: List<AnalysisStepDto> = emptyList(),
    val references: AnalysisReferencesDto? = null
)

data class AnalysisItemDto(
    val title: String,
    val status: String,
    /** null = no aplica (por ejemplo WiFi sin mozos con tablet). */
    val score: Int? = null,
    val description: String,
    val color: String,
    val criteria: List<AnalysisCriterionDto> = emptyList()
)

/** Un criterio con peso: cumplimiento medido de 0 a 1 (null = no aplica). */
data class AnalysisCriterionDto(
    val name: String,
    val weight: Int,
    val compliance: Double? = null,
    val detail: String = ""
)

data class AnalysisRecommendationDto(
    val id: String,
    val type: String,
    val title: String,
    val detail: String,
    val equipment: String? = null
)

data class AnalysisStepDto(val p: String, val t: String)

data class AnalysisReferencesDto(
    val similar: List<ReferenceCaseDto> = emptyList(),
    val recommendations: List<ReferenceRecommendationDto> = emptyList(),
    val suggested: List<ReferenceSuggestionDto> = emptyList()
)

data class ReferenceCaseDto(val name: String, val rubro: String, val pct: Int, val why: String = "")
data class ReferenceRecommendationDto(val t: String, val n: Int = 0)
data class ReferenceSuggestionDto(val r: String, val n: Int, val t: String, val d: String)