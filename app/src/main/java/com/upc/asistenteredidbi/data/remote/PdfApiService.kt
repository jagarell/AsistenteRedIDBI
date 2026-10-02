package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.GenericMessageDto
import com.upc.asistenteredidbi.data.remote.dto.MinutaSendRequestDto
import com.upc.asistenteredidbi.data.remote.dto.ProposalPdfRequestDto
import com.upc.asistenteredidbi.data.remote.dto.ProposalSendRequestDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

/** Servicio Retrofit — refleja `PdfController` del gateway (`/api/proposals`). */
interface PdfApiService {

    @Streaming
    @POST("api/proposals/pdf")
    suspend fun generateProposalPdf(
        @Body request: ProposalPdfRequestDto
    ): ResponseBody

    /** Minuta técnica de la evaluación (la arma el gateway a partir del chat completado). */
    @Streaming
    @GET("api/evaluations/{evaluationId}/minuta/pdf")
    suspend fun generateMinutaPdf(
        @Path("evaluationId") evaluationId: Long
    ): ResponseBody

    @POST("api/evaluations/{evaluationId}/minuta/send")
    suspend fun sendMinuta(
        @Path("evaluationId") evaluationId: Long,
        @Body request: MinutaSendRequestDto
    ): GenericMessageDto

    @POST("api/proposals/send")
    suspend fun sendProposal(
        @Body request: ProposalSendRequestDto
    ): GenericMessageDto
}
