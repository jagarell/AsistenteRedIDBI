package com.upc.asistenteredidbi.data.repository

import com.upc.asistenteredidbi.data.mapper.toDomain
import com.upc.asistenteredidbi.data.remote.ChatApiService
import com.upc.asistenteredidbi.data.remote.dto.TechnicalChatAnswerRequestDto
import com.upc.asistenteredidbi.domain.model.TechnicalChatProgress
import com.upc.asistenteredidbi.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApiService
) : ChatRepository {

    override suspend fun startTechnicalChat(
        evaluationId: Long
    ): Result<TechnicalChatProgress> = safeCall {
        api.startTechnicalChat(evaluationId)
            .toDomain()
    }

    override suspend fun answerTechnicalChat(
        evaluationId: Long,
        currentStep: Int,
        answer: String,
        answers: Map<String, String>
    ): Result<TechnicalChatProgress> = safeCall {

        api.answerTechnicalChat(
            evaluationId = evaluationId,
            request = TechnicalChatAnswerRequestDto(
                evaluationId = evaluationId.toString(),
                currentStep = currentStep,
                answer = answer,
                answers = answers
            )
        ).toDomain()
    }

    private suspend fun <T> safeCall(
        block: suspend () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}