package com.upc.asistenteredidbi.data.remote.dto

data class TechnicalChatAnswerRequestDto(
    val evaluationId: String,
    val currentStep: Int,
    val answer: String,
    val answers: Map<String, String>
)

data class TechnicalChatResponseDto(
    val evaluationId: String,
    val currentStep: Int,
    val currentQuestionKey: String?,
    val currentQuestion: String?,
    val currentInputType: String? = null,
    val currentOptions: List<String>? = null,
    val answeredQuestions: Int,
    val totalQuestions: Int,
    val progressPercent: Int,
    val completed: Boolean,
    val answers: Map<String, String>,
    val proposal: TechnicalChatProposalDto?
)

data class TechnicalChatProposalDto(
    val summary: String,
    val recommendations: List<String>,
    val equipment: List<TechnicalEquipmentRecommendationDto>,
    val topologyText: String,
    val topology: TopologyDto? = null,
    val score: Int? = null
)

data class TechnicalEquipmentRecommendationDto(
    val name: String,
    val description: String,
    val quantity: Int
)

/** Topología estructurada construida por el motor de chat (FastAPI) a partir
 * de las respuestas — ver `app/chat/topology.py` en idbi-fastapi. */
data class TopologyDto(
    val nodes: List<TopologyNodeDto>,
    val links: List<TopologyLinkDto>
)

data class TopologyNodeDto(
    val id: String,
    val label: String,
    val type: String,
    val level: Int
)

data class TopologyLinkDto(
    val source: String,
    val target: String,
    val connectionType: String,
    val status: String
)