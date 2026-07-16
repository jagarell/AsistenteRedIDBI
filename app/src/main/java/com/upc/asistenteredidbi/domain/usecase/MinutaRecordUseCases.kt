package com.upc.asistenteredidbi.domain.usecase

import com.upc.asistenteredidbi.domain.model.MinutaRecord
import com.upc.asistenteredidbi.domain.repository.MinutaRecordRepository
import javax.inject.Inject

class ListMinutasUseCase @Inject constructor(
    private val repository: MinutaRecordRepository
) {
    suspend operator fun invoke(): Result<List<MinutaRecord>> = repository.listMinutas()
}

class CreateMinutaUseCase @Inject constructor(
    private val repository: MinutaRecordRepository
) {
    suspend operator fun invoke(
        evaluationId: Long?,
        clientName: String,
        address: String?,
        summary: String?,
        topologyJson: String?,
        contentJson: String?
    ): Result<MinutaRecord> = repository.createMinuta(
        evaluationId = evaluationId,
        clientName = clientName,
        address = address,
        summary = summary,
        topologyJson = topologyJson,
        contentJson = contentJson
    )
}

class CompleteMinutaUseCase @Inject constructor(
    private val repository: MinutaRecordRepository
) {
    suspend operator fun invoke(id: Long): Result<MinutaRecord> = repository.completeMinuta(id)
}

class ValidateMinutaUseCase @Inject constructor(
    private val repository: MinutaRecordRepository
) {
    suspend operator fun invoke(id: Long): Result<MinutaRecord> = repository.validateMinuta(id)
}
