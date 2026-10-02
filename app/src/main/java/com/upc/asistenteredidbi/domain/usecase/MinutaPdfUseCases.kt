package com.upc.asistenteredidbi.domain.usecase

import com.upc.asistenteredidbi.domain.repository.PdfRepository
import javax.inject.Inject

class GenerateMinutaPdfUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(evaluationId: Long): Result<ByteArray> =
        repository.generateMinutaPdf(evaluationId)
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
