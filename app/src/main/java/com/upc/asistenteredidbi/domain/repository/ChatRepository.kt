package com.upc.asistenteredidbi.domain.repository

import android.net.Uri
import com.upc.asistenteredidbi.domain.model.ChatClosingSummary
import com.upc.asistenteredidbi.domain.model.ChatProgress
import com.upc.asistenteredidbi.domain.model.ChatResponseAnswer

/** Contrato del dominio para HU03 (chat conversacional, árbol de decisión). */
interface ChatRepository {

    /** Permite reanudar exactamente en el nodo donde el técnico dejó el chat. */
    suspend fun getChatProgress(evaluationId: String): Result<ChatProgress>

    /** Guarda la respuesta de un nodo de tipo texto/número/choice/yes_no/multi_count/multi_select. */
    suspend fun saveChatResponse(evaluationId: String, nodeKey: String, value: String): Result<ChatProgress>

    /** Sube una foto requerida por un nodo de tipo 'photo' (puede disparar análisis de visión IA). */
    suspend fun saveChatPhotoResponse(evaluationId: String, nodeKey: String, imageUri: Uri): Result<ChatProgress>

    suspend fun listChatResponses(evaluationId: String): Result<List<ChatResponseAnswer>>

    /** Resumen de cierre que el asistente muestra dentro del chat al completar el árbol. */
    suspend fun getClosingSummary(evaluationId: String): Result<ChatClosingSummary>

    /** El técnico valida (o corrige con una nota) el resumen de cierre. */
    suspend fun confirmClosingSummary(evaluationId: String, additionalNotes: String?): Result<ChatProgress>
}
