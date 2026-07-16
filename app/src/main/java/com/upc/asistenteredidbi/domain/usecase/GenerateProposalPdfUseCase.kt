package com.upc.asistenteredidbi.domain.usecase

import com.upc.asistenteredidbi.domain.model.ProposalPdfData
import com.upc.asistenteredidbi.domain.repository.PdfRepository
import javax.inject.Inject

class GenerateProposalPdfUseCase @Inject constructor(
    private val repository: PdfRepository
) {
    suspend operator fun invoke(data: ProposalPdfData): Result<ByteArray> =
        repository.generateProposalPdf(data)
}
