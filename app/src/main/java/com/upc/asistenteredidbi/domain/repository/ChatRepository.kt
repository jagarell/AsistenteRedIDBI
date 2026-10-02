package com.upc.asistenteredidbi.domain.repository

import com.upc.asistenteredidbi.domain.model.TechnicalChatProgress
import java.io.File

interface ChatRepository {

    suspend fun startTechnicalChat(
        evaluationId: Long
    ): Result<TechnicalChatProgress>

    suspend fun answerTechnicalChat(
        evaluationId: Long,
        state: String,
        answer: String
    ): Result<TechnicalChatProgress>

    /** "Corregir" una evidencia (`fields`) y/o responder una confirmación (`clarificationKey`). */
    suspend fun amendTechnicalChat(
        evaluationId: Long,
        state: String,
        evidenceCode: String? = null,
        evidenceScope: String? = null,
        fields: Map<String, Any?> = emptyMap(),
        clarificationKey: String? = null,
        clarificationAnswer: String? = null
    ): Result<TechnicalChatProgress>

    /** `photos` ya vienen comprimidas (ver ChatPhotoStore). */
    suspend fun answerTechnicalChatWithPhotos(
        evaluationId: Long,
        state: String,
        photos: List<File>
    ): Result<TechnicalChatProgress>
}
