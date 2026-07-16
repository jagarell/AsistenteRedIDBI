package com.upc.asistenteredidbi.domain.repository

import com.upc.asistenteredidbi.domain.model.ProposalPdfData

interface PdfRepository {
    /** Pide al gateway el PDF real de la propuesta y devuelve sus bytes. */
    suspend fun generateProposalPdf(data: ProposalPdfData): Result<ByteArray>
}
