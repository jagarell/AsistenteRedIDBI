package com.upc.asistenteredidbi.data.remote

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

    /** Responder un nodo PHOTO (ej. captura de speedtest) — ver
     * ChatController.answerWithPhoto en el gateway. */
    @Multipart
    @POST("api/evaluations/{evaluationId}/chat/answer-photo")
    suspend fun answerTechnicalChatWithPhoto(
        @Path("evaluationId") evaluationId: Long,
        @Part("currentStep") currentStep: RequestBody,
        @Part("answersJson") answersJson: RequestBody,
        @Part file: MultipartBody.Part
    ): TechnicalChatResponseDto
}