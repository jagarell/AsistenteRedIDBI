package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.ChatClosingSummaryDto
import com.upc.asistenteredidbi.data.remote.dto.ChatProgressDto
import com.upc.asistenteredidbi.data.remote.dto.ChatResponseDto
import com.upc.asistenteredidbi.data.remote.dto.ConfirmClosingSummaryRequestDto
import com.upc.asistenteredidbi.data.remote.dto.SaveChatResponseRequestDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** Servicio Retrofit — refleja `chat_router.py` (HU03, árbol de decisión). */
interface ChatApiService {

    @GET("api/v1/evaluations/{evaluationId}/chat/progress")
    suspend fun getChatProgress(@Path("evaluationId") evaluationId: String): ChatProgressDto

    @POST("api/v1/evaluations/{evaluationId}/chat/responses")
    suspend fun saveChatResponse(
        @Path("evaluationId") evaluationId: String,
        @Body request: SaveChatResponseRequestDto
    ): ChatProgressDto

    @Multipart
    @POST("api/v1/evaluations/{evaluationId}/chat/responses/photo")
    suspend fun saveChatPhotoResponse(
        @Path("evaluationId") evaluationId: String,
        @Part("node_key") nodeKey: RequestBody,
        @Part file: MultipartBody.Part
    ): ChatProgressDto

    @GET("api/v1/evaluations/{evaluationId}/chat/responses")
    suspend fun listChatResponses(@Path("evaluationId") evaluationId: String): List<ChatResponseDto>

    @GET("api/v1/evaluations/{evaluationId}/chat/closing-summary")
    suspend fun getClosingSummary(@Path("evaluationId") evaluationId: String): ChatClosingSummaryDto

    @POST("api/v1/evaluations/{evaluationId}/chat/closing-summary/confirm")
    suspend fun confirmClosingSummary(
        @Path("evaluationId") evaluationId: String,
        @Body request: ConfirmClosingSummaryRequestDto
    ): ChatProgressDto
}
