package com.upc.asistenteredidbi.data.repository

import com.upc.asistenteredidbi.data.mapper.toDomain
import com.upc.asistenteredidbi.data.remote.MinutaApiService
import com.upc.asistenteredidbi.data.remote.dto.MinutaRecordRequestDto
import com.upc.asistenteredidbi.domain.model.MinutaRecord
import com.upc.asistenteredidbi.domain.repository.MinutaRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MinutaRecordRepositoryImpl @Inject constructor(
    private val api: MinutaApiService
) : MinutaRecordRepository {

    override suspend fun listMinutas(): Result<List<MinutaRecord>> = safeCall {
        api.listMinutas().map { it.toDomain() }
    }

    override suspend fun getMinuta(id: Long): Result<MinutaRecord> = safeCall {
        api.getMinuta(id).toDomain()
    }

    override suspend fun updateMinuta(
        id: Long,
        clientName: String,
        address: String?,
        contactName: String?,
        contactPhone: String?
    ): Result<MinutaRecord> = safeCall {
        api.updateMinuta(
            id,
            MinutaRecordRequestDto(
                clientName = clientName,
                address = address,
                contactName = contactName,
                contactPhone = contactPhone
            )
        ).toDomain()
    }

    override suspend fun createMinuta(
        evaluationId: Long?,
        clientName: String,
        address: String?,
        summary: String?,
        topologyJson: String?,
        contentJson: String?
    ): Result<MinutaRecord> = safeCall {
        api.createMinuta(
            MinutaRecordRequestDto(
                evaluationId = evaluationId,
                clientName = clientName,
                address = address,
                summary = summary,
                topologyJson = topologyJson,
                contentJson = contentJson
            )
        ).toDomain()
    }

    override suspend fun completeMinuta(id: Long): Result<MinutaRecord> = safeCall {
        api.completeMinuta(id).toDomain()
    }

    override suspend fun validateMinuta(id: Long): Result<MinutaRecord> = safeCall {
        api.validateMinuta(id).toDomain()
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
