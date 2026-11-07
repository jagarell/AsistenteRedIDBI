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

    /** `photos` ya vienen comprimidas (ver ChatPhotoStore). `mode`: "tomar" (cámara), "subir" (galería) o "forzar"
     *  («Guardar como correcta», con `nota` opcional). `nodeId`: foto pendiente de «Subir después». */
    suspend fun answerTechnicalChatWithPhotos(
        evaluationId: Long,
        state: String,
        photos: List<File>,
        mode: String = "subir",
        nota: String? = null,
        nodeId: String? = null,
        descripcion: String? = null
    ): Result<TechnicalChatProgress>

    /** 0.0.15: agregar, editar o borrar (texto vacío) la descripción de una evidencia (`nodeId`: E1, EV#Frontis del local…). */
    suspend fun saveEvidenceNote(
        evaluationId: Long,
        state: String,
        nodeId: String,
        descripcion: String
    ): Result<TechnicalChatProgress>

    /** Lee un documento del cliente. Devuelve el JSON del servidor (status, tipo, titulo, nota, campos). */
    suspend fun readChatAttachment(
        evaluationId: Long,
        state: String,
        tipo: String?,
        photos: List<File>
    ): Result<Map<String, Any?>>

    /** Usa los campos marcados (nodeId, valor) del adjunto. */
    suspend fun useChatAttachment(
        evaluationId: Long,
        state: String,
        campos: List<Map<String, String>>
    ): Result<TechnicalChatProgress>

    /** Editar una respuesta anterior. `mode`: rewind (chat en curso) o reopen (evaluación terminada). */
    suspend fun editTechnicalChat(
        evaluationId: Long,
        state: String,
        nodeId: String,
        mode: String
    ): Result<TechnicalChatProgress>
}
