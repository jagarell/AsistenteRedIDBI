package com.upc.asistenteredidbi.domain.model

/** Tipos de nodo del motor de 20 nodos (ver `app/chat/nodes.py` en idbi-fastapi). */
enum class TechnicalChatInputType {
    TEXT, NUMBER, CHOICE, MULTI_SELECT, YES_NO, PHOTO, LOCATION, CONNECTION_MAP;

    companion object {
        fun fromApiValue(value: String?): TechnicalChatInputType =
            values().firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: TEXT
    }
}

data class TechnicalChatProgress(
    val evaluationId: String,
    val currentStep: Int,
    val currentQuestionKey: String?,
    val currentQuestion: String?,
    val currentInputType: TechnicalChatInputType = TechnicalChatInputType.TEXT,
    val currentOptions: List<String> = emptyList(),
    val answeredQuestions: Int,
    val totalQuestions: Int,
    val progressPercent: Int,
    val completed: Boolean,
    val answers: Map<String, String>,
    val proposal: TechnicalChatProposal?
)

data class TechnicalChatProposal(
    val summary: String,
    val recommendations: List<String>,
    val equipment: List<TechnicalEquipmentRecommendation>,
    val topologyText: String,
    val topology: ChatTopology? = null,
    val score: Int? = null
)

data class TechnicalEquipmentRecommendation(
    val name: String,
    val description: String,
    val quantity: Int
)

/** Topología de red estructurada, construida por el motor a partir del chat. */
data class ChatTopology(
    val nodes: List<ChatTopologyNode>,
    val links: List<ChatTopologyLink>
)

data class ChatTopologyNode(
    val id: String,
    val label: String,
    val type: String,
    val level: Int
)

data class ChatTopologyLink(
    val source: String,
    val target: String,
    val connectionType: String,
    val status: String
)