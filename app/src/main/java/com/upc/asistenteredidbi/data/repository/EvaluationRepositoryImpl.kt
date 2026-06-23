package com.upc.asistenteredidbi.data.repository

import com.upc.asistenteredidbi.data.mapper.toDomain
import com.upc.asistenteredidbi.data.remote.EvaluationApiService
import com.upc.asistenteredidbi.data.remote.dto.StartEvaluationRequestDto
import com.upc.asistenteredidbi.domain.model.Evaluation
import com.upc.asistenteredidbi.domain.model.EvaluationFilters
import com.upc.asistenteredidbi.domain.model.EvaluationStatus
import com.upc.asistenteredidbi.domain.model.EvaluationSummaryItem
import com.upc.asistenteredidbi.domain.repository.EvaluationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EvaluationRepositoryImpl @Inject constructor(
    private val api: EvaluationApiService
) : EvaluationRepository {

    override suspend fun startEvaluation(
        establishmentName: String,
        establishmentAddress: String?,
        latitude: Double?,
        longitude: Double?,
        clientName: String?,
        clientPhone: String?
    ): Result<Evaluation> = safeCall {
        api.startEvaluation(
            StartEvaluationRequestDto(
                establishmentName,
                establishmentAddress,
                latitude,
                longitude,
                clientName,
                clientPhone
            )
        ).toDomain()
    }

    override suspend fun listEvaluations(filters: EvaluationFilters): Result<List<EvaluationSummaryItem>> = safeCall {
        api.listEvaluations(
            clientName = filters.clientName,
            status = filters.status?.let { statusToApiValue(it) },
            dateFrom = filters.dateFrom,
            dateTo = filters.dateTo
        ).map { it.toDomain() }
    }

    override suspend fun getEvaluation(evaluationId: String): Result<Evaluation> = safeCall {
        api.getEvaluation(evaluationId).toDomain()
    }

    private fun statusToApiValue(status: EvaluationStatus): String = when (status) {
        EvaluationStatus.BORRADOR -> "borrador"
        EvaluationStatus.EN_PROGRESO -> "en_progreso"
        EvaluationStatus.GENERADA -> "generada"
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
