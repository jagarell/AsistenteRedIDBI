package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.ChatAmendRequestDto
import com.upc.asistenteredidbi.data.remote.dto.ChatEditRequestDto
import com.upc.asistenteredidbi.data.remote.dto.TechnicalChatAnswerRequestDto
import com.upc.asistenteredidbi.data.remote.dto.TechnicalChatResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ChatApiService {

    @POST("api/evaluations/{evaluationId}/chat/start")
    suspend fun startTechnicalChat(
        @Path("evaluationId") evaluationId: Long
    ): TechnicalChatResponseDto

    @POST("api/evaluations/{evaluationId}/chat/answer")
    suspend fun answerTechnicalChat(
        @Path("evaluationId") evaluationId: Long,
        @Body request: TechnicalChatAnswerRequestDto
    ): TechnicalChatResponseDto

    @POST("api/evaluations/{evaluationId}/chat/amend")
    suspend fun amendTechnicalChat(
        @Path("evaluationId") evaluationId: Long,
        @Body request: ChatAmendRequestDto
    ): TechnicalChatResponseDto

    /** Editar una respuesta anterior (rewind: chat en curso · reopen: evaluación terminada). */
    @POST("api/evaluations/{evaluationId}/chat/edit")
    suspend fun editTechnicalChat(
        @Path("evaluationId") evaluationId: Long,
        @Body request: ChatEditRequestDto
    ): TechnicalChatResponseDto

    /** Adjunto del cliente (ficha RUC, comprobante, recibo, tarjeta, letrero): devuelve lo leído para revisar. */
    @Multipart
    @POST("api/evaluations/{evaluationId}/chat/adjunto")
    suspend fun leerAdjunto(
        @Path("evaluationId") evaluationId: Long,
        @Part("state") state: RequestBody,
        @Part("tipo") tipo: RequestBody?,
        @Part files: List<MultipartBody.Part>
    ): Map<String, @JvmSuppressWildcards Any?>

    /** Usa los datos revisados del adjunto: responde la pregunta actual y deja las siguientes. */
    @POST("api/evaluations/{evaluationId}/chat/adjunto/usar")
    suspend fun usarAdjunto(
        @Path("evaluationId") evaluationId: Long,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): TechnicalChatResponseDto

    /** Responder un nodo EVIDENCE con 1 a 3 fotos — ver
     * ChatController.answerWithPhotos en el gateway. */
    @Multipart
    @POST("api/evaluations/{evaluationId}/chat/answer-photos")
    suspend fun answerTechnicalChatWithPhotos(
        @Path("evaluationId") evaluationId: Long,
        @Part("state") state: RequestBody,
        @Part("mode") mode: RequestBody,
        @Part files: List<MultipartBody.Part>,
        /** «Guardar como correcta» (mode = "forzar"): nota opcional del técnico. */
        @Part("nota") nota: RequestBody? = null,
        /** «Subir después»: foto pendiente que se sube al final (EV#Calientes). */
        @Part("nodeId") nodeId: RequestBody? = null,
        /** 0.0.15: descripción opcional de la foto («¿Qué muestra esta foto?»). */
        @Part("descripcion") descripcion: RequestBody? = null
    ): TechnicalChatResponseDto

    /** 0.0.15: agregar, editar o borrar (descripcion vacía) la descripción de una evidencia. Body: {state, nodeId, descripcion}. */
    @POST("api/evaluations/{evaluationId}/chat/evidence-note")
    suspend fun evidenceNote(
        @Path("evaluationId") evaluationId: Long,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): TechnicalChatResponseDto
}
