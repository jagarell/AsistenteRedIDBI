package com.upc.asistenteredidbi.domain.repository

import com.upc.asistenteredidbi.data.remote.dto.AnalysisResponseDto
import com.upc.asistenteredidbi.domain.model.Evaluation
import com.upc.asistenteredidbi.domain.model.EvaluationFilters
import com.upc.asistenteredidbi.domain.model.EvaluationSummaryItem

/**
 * Contrato del dominio para HU02 (onboarding técnico) e Historial.
 * El chat (HU03), evidencias (HU04) y minuta (HU05) viven en
 * `ChatRepository` / `EvidenceRepository` — ver esos archivos.
 */
interface EvaluationRepository {

    /** HU02: abre una nueva evaluación en el gateway ("Nueva Evaluación" en el Home).
     *  Ambos campos son opcionales — el gateway aplica un nombre/ubicación por defecto si se omiten. */
    suspend fun startEvaluation(
        establishmentName: String? = null,
        establishmentAddress: String? = null
    ): Result<Evaluation>

    /** Pantalla "Historial", con filtros por cliente/fecha/estado. */
    suspend fun listEvaluations(filters: EvaluationFilters = EvaluationFilters()): Result<List<EvaluationSummaryItem>>

    suspend fun getEvaluation(evaluationId: String): Result<Evaluation>

    suspend fun analyzeEvaluation(evaluationId: Long, answers: Map<String, String> = emptyMap()): Result<AnalysisResponseDto>
}
