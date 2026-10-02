package com.upc.asistenteredidbi.data.repository

import com.upc.asistenteredidbi.data.mapper.toDto
import com.upc.asistenteredidbi.data.remote.PdfApiService
import com.upc.asistenteredidbi.data.remote.dto.MinutaSendRequestDto
import com.upc.asistenteredidbi.data.remote.dto.ProposalSendRequestDto
import com.upc.asistenteredidbi.data.remote.toFriendlyMessage
import com.upc.asistenteredidbi.domain.model.ProposalPdfData
import com.upc.asistenteredidbi.domain.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PdfRepositoryImpl @Inject constructor(
    private val api: PdfApiService
) : PdfRepository {

    override suspend fun generateProposalPdf(data: ProposalPdfData): Result<ByteArray> = safeCall {
        api.generateProposalPdf(data.toDto()).use { it.bytes() }
    }

    override suspend fun generateMinutaPdf(evaluationId: Long): Result<ByteArray> = safeCall {
        api.generateMinutaPdf(evaluationId).use { it.bytes() }
    }

    override suspend fun sendMinuta(
        evaluationId: Long,
        to: String,
        cc: String?,
        subject: String,
        message: String
    ): Result<String> = safeCall {
        api.sendMinuta(evaluationId, MinutaSendRequestDto(to, cc, subject, message)).message
    }

    override suspend fun sendProposal(
        to: String,
        cc: String?,
        subject: String,
        message: String,
        data: ProposalPdfData
    ): Result<String> = safeCall {
        api.sendProposal(
            ProposalSendRequestDto(
                to = to,
                cc = cc,
                subject = subject,
                message = message,
                proposal = data.toDto()
            )
        ).message
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(block())
            } catch (exception: Exception) {
                Result.failure(
                    IllegalArgumentException(
                        exception.toFriendlyMessage(codeOverrides = PDF_ERROR_OVERRIDES),
                        exception
                    )
                )
            }
        }

    private companion object {
        val PDF_ERROR_OVERRIDES = mapOf(
            400 to "Los datos enviados no son válidos",
            502 to "No se pudo completar la operación en el servidor",
        )
    }
}
