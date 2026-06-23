package com.upc.asistenteredidbi.domain.usecase

import android.net.Uri
import com.upc.asistenteredidbi.domain.model.ChatClosingSummary
import com.upc.asistenteredidbi.domain.model.ChatProgress
import com.upc.asistenteredidbi.domain.model.ChatResponseAnswer
import com.upc.asistenteredidbi.domain.repository.ChatRepository
import javax.inject.Inject

/** HU03: obtiene el progreso del chat para reanudar exactamente donde quedó. */
class GetChatProgressUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(evaluationId: String): Result<ChatProgress> = repository.getChatProgress(evaluationId)
}

/** HU03: guarda la respuesta del nodo actual (texto/número/choice/yes_no/multi_count/multi_select). */
class SaveChatResponseUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(evaluationId: String, nodeKey: String, value: String): Result<ChatProgress> {
        if (value.isBlank()) {
            return Result.failure(IllegalArgumentException("La respuesta no puede estar vacía"))
        }
        return repository.saveChatResponse(evaluationId, nodeKey, value)
    }
}

/** HU03: sube una foto requerida por un nodo (speedtest, ipconfig, IP Scanner, info del sistema). */
class SaveChatPhotoResponseUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(evaluationId: String, nodeKey: String, imageUri: Uri): Result<ChatProgress> =
        repository.saveChatPhotoResponse(evaluationId, nodeKey, imageUri)
}

class ListChatResponsesUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(evaluationId: String): Result<List<ChatResponseAnswer>> = repository.listChatResponses(evaluationId)
}

/** HU03 §4.5: resumen de cierre que el asistente muestra dentro del chat al completar el árbol. */
class GetClosingSummaryUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(evaluationId: String): Result<ChatClosingSummary> = repository.getClosingSummary(evaluationId)
}

/** El técnico valida (o corrige con una nota) el resumen antes de pasar a Evidencias. */
class ConfirmClosingSummaryUseCase @Inject constructor(private val repository: ChatRepository) {
    suspend operator fun invoke(evaluationId: String, additionalNotes: String? = null): Result<ChatProgress> =
        repository.confirmClosingSummary(evaluationId, additionalNotes)
}
