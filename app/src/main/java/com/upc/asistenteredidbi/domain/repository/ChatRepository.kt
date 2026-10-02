package com.upc.asistenteredidbi.domain.repository

import android.net.Uri
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

    suspend fun answerTechnicalChatWithPhoto(
        evaluationId: Long,
        currentStep: Int,
        answers: Map<String, String>,
        photoUri: Uri
    ): Result<TechnicalChatProgress>
}