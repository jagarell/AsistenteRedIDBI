package com.upc.asistenteredidbi.domain.model

/**
 * Modelos de dominio de una respuesta individual del chat, tal como llegan en
 * la minuta consolidada de solo lectura (HU03-HU04, `Minuta.conversationResponses`).
 * El motor de árbol de decisión que originalmente acompañaba a estos modelos
 * (ChatNode/ChatProgress/ChatClosingSummary/ChatInputType) nunca se conectó a
 * ningún backend real y fue retirado; el motor de chat vigente es el de 23
 * nodos en TechnicalChatModels.kt. [ChatResponseAnswer]/[ChatExtractedData]
 * siguen usados por el stack de `ChatApiService`/`ChatRepositoryImpl` (otro
 * remanente ya retirado, sin pantalla real que lo llame) — no tocar.
 */

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

/** Una respuesta del chat de 23 nodos con el texto real de la pregunta —
 * usado por `Minuta.conversationResponses` (checklist dinámico de
 * evidencias), a diferencia de [ChatResponseAnswer] que pertenece al motor
 * de árbol ya retirado. */
data class ChatAnswerItem(
    val nodeKey: String,
    val question: String,
    val answer: String
)
