package com.upc.asistenteredidbi.domain.repository

import com.upc.asistenteredidbi.domain.model.ProposalPdfData

interface PdfRepository {
    /** Pide al gateway el PDF real de la propuesta y devuelve sus bytes. */
    suspend fun generateProposalPdf(data: ProposalPdfData): Result<ByteArray>

    /** Pide al gateway la minuta técnica (PDF) de la evaluación y devuelve sus bytes. */
    suspend fun generateMinutaPdf(evaluationId: Long, preview: Boolean = false): Result<ByteArray>

    /** Estado de la minuta de la evaluación (null si todavía no existe). */
    suspend fun minutaInfo(evaluationId: Long): Result<com.upc.asistenteredidbi.domain.model.MinutaRecord?>

    suspend fun getMinutaEditions(evaluationId: Long): Result<Map<String, Any?>>

    suspend fun saveMinutaEditions(evaluationId: Long, body: Map<String, Any?>): Result<Map<String, Any?>>

    /** Envía por correo la minuta técnica de la evaluación (PDF adjunto). */
    suspend fun sendMinuta(
        evaluationId: Long,
        to: String,
        cc: String?,
        subject: String,
        message: String
    ): Result<String>

    /** Genera el PDF en el gateway y lo envía por correo con el adjunto. */
    suspend fun sendProposal(
        to: String,
        cc: String?,
        subject: String,
        message: String,
        data: ProposalPdfData
    ): Result<String>
}
