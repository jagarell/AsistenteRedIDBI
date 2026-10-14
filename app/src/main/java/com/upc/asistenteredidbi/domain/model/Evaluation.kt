package com.upc.asistenteredidbi.domain.model

/** Modelos de dominio para HU02 (onboarding técnico) e Historial. */

/** Traducción real: gatewayStatusToDomain en EvaluationMappers.kt (el
 *  gateway manda COMPLETADO/BORRADOR/ENVIADO/EN_ANALISIS). */
enum class EvaluationStatus {
    BORRADOR, EN_PROGRESO, GENERADA
}

data class Evaluation(
    val id: String,
    val establishmentName: String,
    val establishmentAddress: String?,
    val status: EvaluationStatus,
    val createdAt: String
)

data class EvaluationSummaryItem(
    val id: String,
    val establishmentName: String,
    val clientName: String?,
    val status: EvaluationStatus,
    val overallScore: Float?,
    val createdAt: String,
    val address: String? = null,
    /** Estado que ve el técnico en Historial e Inicio. */
    val stage: EvaluationStage = EvaluationStage.EN_CURSO,
    val minutaId: Long? = null,
    val validatedByName: String? = null,
    val technicianName: String? = null,
    /** Motivo del rechazo mientras el supervisor la tiene devuelta al técnico. */
    val rejectionReason: String? = null
) {
    val isRejected: Boolean get() = stage == EvaluationStage.BORRADOR && !rejectionReason.isNullOrBlank()
}

/** Mismos estados que el prototipo: En análisis (chat sin terminar) → Borrador → Completa → Validada. */
enum class EvaluationStage(val label: String) {
    EN_CURSO("En análisis"), BORRADOR("Borrador"), COMPLETA("Completa"), VALIDADA("Validada");

    /** La evaluación ya tiene propuesta (el chat terminó). */
    val isFinished: Boolean get() = this != EN_CURSO

    companion object {
        fun from(value: String?): EvaluationStage = entries.firstOrNull { it.name == value?.uppercase() } ?: EN_CURSO
    }
}

data class EvaluationFilters(
    val clientName: String? = null,
    val status: EvaluationStatus? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null
)

/** Vista consolidada de solo lectura (HU03-HU04 §5: tabla de equipos + áreas + respuestas). */
data class Minuta(
    val evaluationId: Long,
    val establishmentName: String,
    val establishmentAddress: String?,
    val establishmentType: String?,
    val conversationResponses: List<ChatAnswerItem>,
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
