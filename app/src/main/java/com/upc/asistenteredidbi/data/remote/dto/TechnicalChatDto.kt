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
    val currentUnit: String? = null,
    val answeredQuestions: Int,
    val totalQuestions: Int,
    val progressPercent: Int,
    val completed: Boolean,
    val answers: Map<String, String>,
    val proposal: TechnicalChatProposalDto?,
    /** Campos leídos por IA de la foto recién respondida (ej. Mbps/ping/ISP
     *  de una captura de speedtest) — solo viene poblado justo después de
     *  responder un nodo PHOTO. */
    val lastPhotoResult: Map<String, Any?>? = null,
    /** Aviso cuando lo leído en la foto no coincide con lo ya respondido
     *  antes en el chat (ej. ISP de la captura vs. proveedor tecleado). */
    val crossValidationWarning: String? = null
)

data class TechnicalChatProposalDto(
    val summary: String,
    val asIsFindings: List<String>? = null,
    val recommendations: List<String>,
    val equipment: List<TechnicalEquipmentRecommendationDto>,
    val topologyText: String,
    val topology: TopologyDto? = null,
    val score: Int? = null
)

data class TechnicalEquipmentRecommendationDto(
    val name: String,
    val description: String,
    val quantity: Int,
    val unitPrice: Double? = null
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