package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.GenericMessageDto
import com.upc.asistenteredidbi.data.remote.dto.ProposalPdfRequestDto
import com.upc.asistenteredidbi.data.remote.dto.ProposalSendRequestDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

/** Servicio Retrofit — refleja `PdfController` del gateway (`/api/proposals`). */
interface PdfApiService {

    @Streaming
    @POST("api/proposals/pdf")
    suspend fun generateProposalPdf(
        @Body request: ProposalPdfRequestDto
    ): ResponseBody

    @POST("api/proposals/send")
    suspend fun sendProposal(
        @Body request: ProposalSendRequestDto
    ): GenericMessageDto
}
