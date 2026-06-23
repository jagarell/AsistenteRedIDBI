package com.upc.asistenteredidbi.domain.usecase

import com.upc.asistenteredidbi.domain.model.Evaluation
import com.upc.asistenteredidbi.domain.model.EvaluationFilters
import com.upc.asistenteredidbi.domain.model.EvaluationSummaryItem
import com.upc.asistenteredidbi.domain.repository.EvaluationRepository
import javax.inject.Inject

/** HU02: inicia una nueva evaluación ("Nueva Evaluación" en el Home).
 *  La dirección puede venir de GPS (lat/lng) o de texto libre. */
class StartEvaluationUseCase @Inject constructor(private val repository: EvaluationRepository) {
    suspend operator fun invoke(
        establishmentName: String,
        establishmentAddress: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        clientName: String? = null,
        clientPhone: String? = null
    ): Result<Evaluation> {
        if (establishmentName.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre del establecimiento es obligatorio"))
        }
        return repository.startEvaluation(
            establishmentName.trim(), establishmentAddress?.trim(), latitude, longitude,
            clientName?.trim(), clientPhone?.trim()
        )
    }
}

/** Listado para "Continuar Evaluación" y la pantalla Historial (con filtros). */
class ListEvaluationsUseCase @Inject constructor(private val repository: EvaluationRepository) {
    suspend operator fun invoke(filters: EvaluationFilters = EvaluationFilters()): Result<List<EvaluationSummaryItem>> =
        repository.listEvaluations(filters)
}

class GetEvaluationUseCase @Inject constructor(private val repository: EvaluationRepository) {
    suspend operator fun invoke(evaluationId: String): Result<Evaluation> = repository.getEvaluation(evaluationId)
}
