package com.upc.asistenteredidbi.domain.usecase

import com.upc.asistenteredidbi.domain.model.Evaluation
import com.upc.asistenteredidbi.domain.model.EvaluationFilters
import com.upc.asistenteredidbi.domain.model.EvaluationSummaryItem
import com.upc.asistenteredidbi.domain.repository.EvaluationRepository
import javax.inject.Inject

/** HU02: inicia una nueva evaluación ("Nueva Evaluación" en el Home). */
class StartEvaluationUseCase @Inject constructor(private val repository: EvaluationRepository) {
    suspend operator fun invoke(
        establishmentName: String? = null,
        establishmentAddress: String? = null
    ): Result<Evaluation> = repository.startEvaluation(
        establishmentName?.trim()?.takeIf { it.isNotBlank() },
        establishmentAddress?.trim()?.takeIf { it.isNotBlank() }
    )
}

/** Listado para "Continuar Evaluación" y la pantalla Historial (con filtros). */
class ListEvaluationsUseCase @Inject constructor(private val repository: EvaluationRepository) {
    suspend operator fun invoke(filters: EvaluationFilters = EvaluationFilters()): Result<List<EvaluationSummaryItem>> =
        repository.listEvaluations(filters)
}

class GetEvaluationUseCase @Inject constructor(private val repository: EvaluationRepository) {
    suspend operator fun invoke(evaluationId: String): Result<Evaluation> = repository.getEvaluation(evaluationId)
}
