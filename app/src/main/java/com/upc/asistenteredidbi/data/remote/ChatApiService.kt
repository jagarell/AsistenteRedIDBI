package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.ChatAmendRequestDto
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

    /** Responder un nodo EVIDENCE con 1 a 3 fotos — ver
     * ChatController.answerWithPhotos en el gateway. */
    @Multipart
    @POST("api/evaluations/{evaluationId}/chat/answer-photos")
    suspend fun answerTechnicalChatWithPhotos(
        @Path("evaluationId") evaluationId: Long,
        @Part("state") state: RequestBody,
        @Part files: List<MultipartBody.Part>
    ): TechnicalChatResponseDto
}
