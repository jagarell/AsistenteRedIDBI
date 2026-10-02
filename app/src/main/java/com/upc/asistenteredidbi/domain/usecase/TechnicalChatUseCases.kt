package com.upc.asistenteredidbi.domain.usecase

import com.upc.asistenteredidbi.domain.model.TechnicalChatProgress
import com.upc.asistenteredidbi.domain.repository.ChatRepository
import java.io.File
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
        state: String,
        answer: String
    ): Result<TechnicalChatProgress> {
        return repository.answerTechnicalChat(
            evaluationId = evaluationId,
            state = state,
            answer = answer
        )
    }
}

class AnswerTechnicalChatWithPhotosUseCase @Inject constructor(
    private val repository: ChatRepository
) {

    suspend operator fun invoke(
        evaluationId: Long,
        state: String,
        photos: List<File>
    ): Result<TechnicalChatProgress> {
        return repository.answerTechnicalChatWithPhotos(
            evaluationId = evaluationId,
            state = state,
            photos = photos
        )
    }
}

class AmendTechnicalChatUseCase @Inject constructor(
    private val repository: ChatRepository
) {

    suspend operator fun invoke(
        evaluationId: Long,
        state: String,
        evidenceCode: String? = null,
        evidenceScope: String? = null,
        fields: Map<String, Any?> = emptyMap(),
        clarificationKey: String? = null,
        clarificationAnswer: String? = null
    ): Result<TechnicalChatProgress> = repository.amendTechnicalChat(
        evaluationId, state, evidenceCode, evidenceScope, fields, clarificationKey, clarificationAnswer
    )
}
