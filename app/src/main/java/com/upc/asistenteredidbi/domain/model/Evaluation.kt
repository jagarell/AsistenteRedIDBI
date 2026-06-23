package com.upc.asistenteredidbi.domain.model

/** Modelos de dominio para HU02 (onboarding técnico) e Historial. */

enum class EvaluationStatus {
    BORRADOR, EN_PROGRESO, GENERADA;

    companion object {
        fun fromApiValue(value: String): EvaluationStatus = when (value.lowercase()) {
            "en_progreso" -> EN_PROGRESO
            "generada" -> GENERADA
            else -> BORRADOR
        }
    }
}

data class Evaluation(
    val id: String,
    val establishmentId: String,
    val establishmentName: String,
    val establishmentAddress: String?,
    val googleMapsUrl: String?,
    val status: EvaluationStatus,
    val createdAt: String
)

data class EvaluationSummaryItem(
    val id: String,
    val establishmentName: String,
    val clientName: String?,
    val status: EvaluationStatus,
    val overallScore: Float?,
    val createdAt: String
)

data class EvaluationFilters(
    val clientName: String? = null,
    val status: EvaluationStatus? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null
)

/** Vista consolidada de solo lectura (HU03-HU04 §5: tabla de equipos + áreas + respuestas). */
data class Minuta(
    val evaluationId: String,
    val establishmentName: String,
    val establishmentAddress: String?,
    val conversationResponses: List<ChatResponseAnswer>,
    val areas: List<EvidenceAreaItem>,
    val equipment: List<EvidenceEquipmentItem>,
    val equipmentTable: List<EquipmentTableRow>
)

data class EquipmentTableRow(
    val label: String,
    val equipmentType: String,
    val extractedSpecs: Map<String, Any?>?,
    val technicianNotes: String?,
    val photoCount: Int
)
