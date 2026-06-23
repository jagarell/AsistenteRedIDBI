package com.upc.asistenteredidbi.domain.repository

import com.upc.asistenteredidbi.domain.model.Evaluation
import com.upc.asistenteredidbi.domain.model.EvaluationFilters
import com.upc.asistenteredidbi.domain.model.EvaluationSummaryItem

/**
 * Contrato del dominio para HU02 (onboarding técnico) e Historial.
 * El chat (HU03), evidencias (HU04) y minuta (HU05) viven en
 * `ChatRepository` / `EvidenceRepository` — ver esos archivos.
 */
interface EvaluationRepository {

    /** HU02: crea el establecimiento (si no existe) y abre una nueva evaluación.
     *  La dirección puede venir de GPS (lat/lng) o de texto libre. */
    suspend fun startEvaluation(
        establishmentName: String,
        establishmentAddress: String?,
        latitude: Double?,
        longitude: Double?,
        clientName: String?,
        clientPhone: String?
    ): Result<Evaluation>

    /** Pantalla "Historial", con filtros por cliente/fecha/estado. */
    suspend fun listEvaluations(filters: EvaluationFilters = EvaluationFilters()): Result<List<EvaluationSummaryItem>>

    suspend fun getEvaluation(evaluationId: String): Result<Evaluation>
}
