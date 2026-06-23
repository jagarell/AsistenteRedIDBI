package com.upc.asistenteredidbi.domain.model

/**
 * Modelos de dominio del chat conversacional guiado (HU03), dirigido
 * por el árbol de decisión del backend (`conversation_engine.py`).
 * Reemplaza el antiguo `ChatQuestion` (lista plana de 12 pasos).
 */

enum class ChatInputType {
    TEXT, NUMBER, CHOICE, YES_NO, PHOTO, MULTI_COUNT, MULTI_SELECT, CONNECTION_MAP, SINGLE_SELECT, IMAGE_CAPTURE;

    companion object {
        fun fromApiValue(value: String): ChatInputType = when (value) {
            "text" -> TEXT
            "number" -> NUMBER
            "choice" -> CHOICE
            "yes_no" -> YES_NO
            "photo" -> PHOTO
            "multi_count" -> MULTI_COUNT
            "multi_select" -> MULTI_SELECT
            "connection_map" -> CONNECTION_MAP
            else -> TEXT
        }
    }
}

/** Valor por defecto de conexión: "se conecta directo al router principal". */
const val ROOT_CONNECTION_LABEL: String = "Router principal"

data class ChatNode(
    val nodeKey: String,
    val promptText: String,
    val inputType: ChatInputType,
    val category: String?,
    val choices: List<String>?,
    val countFields: List<String>?,
    val selectOptions: List<String>?,
    // Para CONNECTION_MAP: los equipos que el técnico puede conectar entre
    // sí (calculado por el backend a partir de los conteos ya respondidos).
    val connectionMapItems: List<String>?,
    val guideLinkLabel: String?,
    val guideLinkUrl: String?,
    val infoTip: String?,
    val requiresPhotoAnalysis: Boolean
)

data class ChatProgress(
    val evaluationId: String,
    val nextNode: ChatNode?,
    val isComplete: Boolean,
    val answeredCount: Int
)

data class ChatExtractedData(
    val extractedFields: Map<String, String?>,
    val confidence: Float,
    val notes: String?
)

data class ChatResponseAnswer(
    val nodeKey: String,
    val value: String,
    val extractedData: ChatExtractedData?
)

data class ChatClosingSummary(
    val evaluationId: String,
    val summaryText: String,
    val suggestedLocalAreas: List<String>,
    val suggestedEquipmentCount: Map<String, Int>
)
