package com.upc.asistenteredidbi.domain.usecase

import com.upc.asistenteredidbi.domain.model.ProposalPdfData
import com.upc.asistenteredidbi.domain.repository.PdfRepository
import javax.inject.Inject

class SendProposalUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(
        to: String,
        cc: String?,
        subject: String,
        message: String,
        data: ProposalPdfData
    ): Result<String> = repository.sendProposal(to, cc, subject, message, data)
}
