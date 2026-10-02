package com.upc.asistenteredidbi.data.remote.dto

data class TechnicalChatAnswerRequestDto(
    val evaluationId: String,
    val state: String,
    val answer: String
)

/** Corrige lo leído en una evidencia y/o responde una confirmación (ver ChatController.amend). */
data class ChatAmendRequestDto(
    val evaluationId: String,
    val state: String,
    val evidenceCode: String? = null,
    val evidenceScope: String? = null,
    val fields: Map<String, Any?> = emptyMap(),
    val clarificationKey: String? = null,
    val clarificationAnswer: String? = null
)

data class ChatFollowUpDto(
    val key: String,
    val text: String,
    val options: List<String>? = null
)

data class ChatOptionDto(val value: String, val label: String)

data class ChatNodeDto(
    val nodeId: String,
    val scope: String? = null,
    val kind: String,
    val inputType: String,
    val text: String,
    val options: List<ChatOptionDto>? = null,
    val required: Boolean? = null,
    val validation: Map<String, Any?>? = null,
    val block: String? = null,
    val blockLabel: String? = null,
    val blockIndex: Int? = null,
    val blockCount: Int? = null,
    val defaultValue: String? = null,
    val evidenceCode: String? = null,
    val maxFiles: Int? = null,
    val severity: String? = null,
    val context: String? = null,
    val keyboard: String? = null,
    val hint: String? = null
)

data class ChatEvidenceDto(
    val code: String,
    val scope: String? = null,
    val area: String? = null,
    val equipo: String? = null,
    val count: Int? = null,
    val extracted: Map<String, Any?>? = null
)

data class TechnicalChatResponseDto(
    val evaluationId: String,
    val answeredQuestions: Int,
    val totalQuestions: Int,
    val progressPercent: Int,
    val completed: Boolean,
    val answers: Map<String, String>,
    val proposal: TechnicalChatProposalDto?,
    val state: String? = null,
    val node: ChatNodeDto? = null,
    val validationError: String? = null,
    val lastEvidence: ChatEvidenceDto? = null,
    val crossChecks: List<String>? = null,
    val followUps: List<ChatFollowUpDto>? = null,
    /** Solo E3: fotos con las credenciales ya desenfocadas (base64). */
    val processedImages: List<String>? = null
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
    val level: Int,
    val detail: String? = null,
    val pending: Boolean? = null
)

data class TopologyLinkDto(
    val source: String,
    val target: String,
    val connectionType: String,
    val status: String
)