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
        clarificationAnswer: String? = null,
        nodeId: String? = null,
        deleted: List<String>? = null,
        restore: Boolean? = null
    ): Result<TechnicalChatProgress>

    /** `photos` ya vienen comprimidas (ver ChatPhotoStore). `mode`: "tomar" (cámara) o "subir" (galería). */
    suspend fun answerTechnicalChatWithPhotos(
        evaluationId: Long,
        state: String,
        photos: List<File>,
        mode: String = "subir"
    ): Result<TechnicalChatProgress>

    /** Editar una respuesta anterior. `mode`: rewind (chat en curso) o reopen (evaluación terminada). */
    suspend fun editTechnicalChat(
        evaluationId: Long,
        state: String,
        nodeId: String,
        mode: String
    ): Result<TechnicalChatProgress>
}
