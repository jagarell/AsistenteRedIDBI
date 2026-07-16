package com.upc.asistenteredidbi.data.repository

import com.upc.asistenteredidbi.data.mapper.toDto
import com.upc.asistenteredidbi.data.remote.PdfApiService
import com.upc.asistenteredidbi.domain.model.ProposalPdfData
import com.upc.asistenteredidbi.domain.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PdfRepositoryImpl @Inject constructor(
    private val api: PdfApiService
) : PdfRepository {

    override suspend fun generateProposalPdf(data: ProposalPdfData): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            try {
                val bytes = api.generateProposalPdf(data.toDto()).use { it.bytes() }
                Result.success(bytes)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
