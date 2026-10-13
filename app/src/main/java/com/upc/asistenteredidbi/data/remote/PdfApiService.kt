package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.GenericMessageDto
import com.upc.asistenteredidbi.data.remote.dto.MinutaSendRequestDto
import com.upc.asistenteredidbi.data.remote.dto.ProposalPdfRequestDto
import com.upc.asistenteredidbi.data.remote.dto.ProposalSendRequestDto
import com.upc.asistenteredidbi.data.remote.dto.MinutaRecordResponseDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.PUT
import retrofit2.http.Query
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
        @Path("evaluationId") evaluationId: Long,
        /** true = vista previa (marca VISTA PREVIA): no cambia el estado. Sin preview, al generarse la minuta pasa a COMPLETA. */
        @Query("preview") preview: Boolean = false
    ): ResponseBody

    /** La minuta de esa evaluación (estado, quién la validó); 204 si todavía no existe. */
    @GET("api/evaluations/{evaluationId}/minuta/info")
    suspend fun minutaInfo(@Path("evaluationId") evaluationId: Long): Response<MinutaRecordResponseDto>

    /** Ediciones de la minuta: datos principales, observaciones, propuesta editada. */
    @GET("api/evaluations/{evaluationId}/minuta/ediciones")
    suspend fun getMinutaEditions(@Path("evaluationId") evaluationId: Long): Map<String, Any?>

    @PUT("api/evaluations/{evaluationId}/minuta/ediciones")
    suspend fun saveMinutaEditions(
        @Path("evaluationId") evaluationId: Long,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any?>

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
