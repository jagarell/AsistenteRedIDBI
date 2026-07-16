package com.upc.asistenteredidbi.domain.model

/**
 * Modelos de dominio de una respuesta individual del chat, tal como llegan en
 * la minuta consolidada de solo lectura (HU03-HU04, `Minuta.conversationResponses`).
 * El motor de árbol de decisión que originalmente acompañaba a estos modelos
 * (ChatNode/ChatProgress/ChatClosingSummary/ChatInputType) nunca se conectó a
 * ningún backend real y fue retirado; el motor de chat vigente es el de 20
 * nodos en TechnicalChatModels.kt.
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
