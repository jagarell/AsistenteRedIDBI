package com.upc.asistenteredidbi.data.remote.dto

data class TechnicalChatAnswerRequestDto(
    val evaluationId: String,
    val state: String,
    val answer: String,
    /** Solo con fotos: "tomar" (cámara) o "subir" (galería). */
    val mode: String? = null
)

/** Editar una respuesta anterior: `rewind` (chat en curso) o `reopen` (evaluación terminada). */
data class ChatEditRequestDto(
    val evaluationId: String,
    val state: String,
    val nodeId: String,
    val mode: String
)

/** Corrige lo leído en una evidencia y/o responde una confirmación (ver ChatController.amend). */
data class ChatAmendRequestDto(
    val evaluationId: String,
    val state: String,
    val evidenceCode: String? = null,
    val evidenceScope: String? = null,
    val fields: Map<String, Any?> = emptyMap(),
    val clarificationKey: String? = null,
    val clarificationAnswer: String? = null,
    val nodeId: String? = null,
    val deleted: List<String>? = null,
    val restore: Boolean? = null
)

data class ChatFollowUpDto(
    val key: String,
    val text: String,
    val options: List<String>? = null
)

data class ChatOptionDto(
    val value: String,
    val label: String,
    /** "Otro (escribir)": pide el texto. */
    val other: Boolean? = null,
    /** El texto de "Otro" es un número. */
    val numeric: Boolean? = null
)

data class ChatFieldDto(
    val key: String,
    val label: String,
    /** number | text | sel */
    val type: String? = null,
    val options: List<ChatOptionDto>? = null
)

data class ChatEvidenceNodeDto(
    val read: String? = null,
    val name: String? = null,
    /** WAITING | REVIEW | IL (no legible) | NC (no corresponde) | WRONG (no es correcto) */
    val status: String? = null,
    val message: String? = null,
    val fields: List<ChatFieldDto>? = null,
    val data: Map<String, Any?>? = null,
    val label: String? = null,
    val unavailable: Boolean? = null,
    val mode: String? = null,
    val lastTry: String? = null
)

data class ChatNoticeDto(
    val id: String? = null,
    val tone: String? = null,
    val tag: String? = null,
    val text: String
)

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
    val questionNumber: Int? = null,
    val questionTotal: Int? = null,
    val evidenceNumber: Int? = null,
    val evidenceTotal: Int? = null,
    val defaultValue: String? = null,
    val evidenceCode: String? = null,
    /** false en las evidencias obligatorias (la foto general del local). */
    val skippable: Boolean? = null,
    val maxFiles: Int? = null,
    val severity: String? = null,
    val context: String? = null,
    val keyboard: String? = null,
    val hint: String? = null,
    val suggestions: List<String>? = null,
    val selected: List<String>? = null,
    val noneLabel: String? = null,
    val allowOther: Boolean? = null,
    val multiline: Boolean? = null,
    val unit: String? = null,
    val optional: Boolean? = null,
    val evidence: ChatEvidenceNodeDto? = null,
    val fields: List<ChatFieldDto>? = null,
    /** READ_CARD: mapa de campos; STEPS: lista de {p, t}. */
    val value: Any? = null,
    val zones: List<String>? = null,
    val videoTypes: List<String>? = null,
    val questionsTotal: Int? = null,
    val evidencesTotal: Int? = null
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
    /** Solo la etiqueta del router: fotos con las credenciales ya desenfocadas (base64). */
    val processedImages: List<String>? = null,
    /** Avisos entre la respuesta y la siguiente pregunta (no piden confirmación). */
    val notices: List<ChatNoticeDto>? = null
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