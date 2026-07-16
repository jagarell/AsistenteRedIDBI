package com.upc.asistenteredidbi.data.repository

import com.upc.asistenteredidbi.data.mapper.toDto
import com.upc.asistenteredidbi.data.remote.PdfApiService
import com.upc.asistenteredidbi.data.remote.dto.ProposalSendRequestDto
import com.upc.asistenteredidbi.domain.model.ProposalPdfData
import com.upc.asistenteredidbi.domain.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import javax.inject.Inject

class PdfRepositoryImpl @Inject constructor(
    private val api: PdfApiService
) : PdfRepository {

    override suspend fun generateProposalPdf(data: ProposalPdfData): Result<ByteArray> = safeCall {
        api.generateProposalPdf(data.toDto()).use { it.bytes() }
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
            } catch (exception: HttpException) {
                Result.failure(IllegalArgumentException(extractErrorMessage(exception)))
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }

    private fun extractErrorMessage(exception: HttpException): String {
        return try {
            val errorBody = exception.response()?.errorBody()?.string()
            if (errorBody.isNullOrBlank()) {
                defaultErrorMessage(exception.code())
            } else {
                JSONObject(errorBody).optString("message", defaultErrorMessage(exception.code()))
            }
        } catch (_: Exception) {
            defaultErrorMessage(exception.code())
        }
    }

    private fun defaultErrorMessage(code: Int): String = when (code) {
        400 -> "Los datos enviados no son válidos"
        401 -> "Tu sesión no está autorizada"
        403 -> "No tienes permisos para realizar esta acción"
        502 -> "No se pudo completar la operación en el servidor"
        else -> "No se pudo completar la solicitud"
    }
}
