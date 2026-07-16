package com.upc.asistenteredidbi.domain.repository

import com.upc.asistenteredidbi.domain.model.ProposalPdfData

interface PdfRepository {
    /** Pide al gateway el PDF real de la propuesta y devuelve sus bytes. */
    suspend fun generateProposalPdf(data: ProposalPdfData): Result<ByteArray>

    /** Genera el PDF en el gateway y lo envía por correo con el adjunto. */
    suspend fun sendProposal(
        to: String,
        cc: String?,
        subject: String,
        message: String,
        data: ProposalPdfData
    ): Result<String>
}
