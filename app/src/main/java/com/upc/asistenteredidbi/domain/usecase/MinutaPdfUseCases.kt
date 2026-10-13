package com.upc.asistenteredidbi.domain.usecase

import com.upc.asistenteredidbi.domain.repository.PdfRepository
import javax.inject.Inject

class GenerateMinutaPdfUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(evaluationId: Long, preview: Boolean = false): Result<ByteArray> =
        repository.generateMinutaPdf(evaluationId, preview)
}

class GetMinutaInfoUseCase @Inject constructor(private val repository: PdfRepository) {
    suspend operator fun invoke(evaluationId: Long) = repository.minutaInfo(evaluationId)
}

class MinutaEditionsUseCase @Inject constructor(private val repository: PdfRepository) {
    suspend fun load(evaluationId: Long) = repository.getMinutaEditions(evaluationId)
    suspend fun save(evaluationId: Long, body: Map<String, Any?>) = repository.saveMinutaEditions(evaluationId, body)
}

class SendMinutaUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(
        evaluationId: Long,
        to: String,
        cc: String?,
        subject: String,
        message: String
    ): Result<String> = repository.sendMinuta(evaluationId, to, cc, subject, message)
}
