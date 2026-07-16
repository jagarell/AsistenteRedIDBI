package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.TechnicalChatAnswerRequestDto
import com.upc.asistenteredidbi.data.remote.dto.TechnicalChatResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
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
}