package com.upc.asistenteredidbi.domain.usecase

import com.upc.asistenteredidbi.domain.model.TechnicalChatProgress
import com.upc.asistenteredidbi.domain.repository.ChatRepository
import javax.inject.Inject

class StartTechnicalChatUseCase @Inject constructor(
    private val repository: ChatRepository
) {

    suspend operator fun invoke(
        evaluationId: Long
    ): Result<TechnicalChatProgress> {
        return repository.startTechnicalChat(evaluationId)
    }
}

class AnswerTechnicalChatUseCase @Inject constructor(
    private val repository: ChatRepository
) {

    suspend operator fun invoke(
        evaluationId: Long,
        currentStep: Int,
        answer: String,
        answers: Map<String, String>
    ): Result<TechnicalChatProgress> {
        return repository.answerTechnicalChat(
            evaluationId = evaluationId,
            currentStep = currentStep,
            answer = answer,
            answers = answers
        )
    }
}