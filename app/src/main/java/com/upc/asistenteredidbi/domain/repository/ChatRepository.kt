package com.upc.asistenteredidbi.domain.repository

import com.upc.asistenteredidbi.domain.model.TechnicalChatProgress

interface ChatRepository {

    suspend fun startTechnicalChat(
        evaluationId: Long
    ): Result<TechnicalChatProgress>

    suspend fun answerTechnicalChat(
        evaluationId: Long,
        currentStep: Int,
        answer: String,
        answers: Map<String, String>
    ): Result<TechnicalChatProgress>
}