package com.upc.asistenteredidbi.domain.model

/** Tipos de nodo del flujo de minuta técnica (ver `app/chat/flow_engine.py` en idbi-fastapi). */
enum class TechnicalChatInputType {
    TEXT, NUMBER, CHOICE, MULTI_SELECT, YES_NO, EVIDENCE, ALERT, SUMMARY;

    companion object {
        fun fromApiValue(value: String?): TechnicalChatInputType =
            values().firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: TEXT
    }
}

/** Opción de una pregunta: `value` es lo que se manda al motor, `label` lo que se muestra. */
data class ChatOption(val value: String, val label: String)

/** Nodo que el motor pide responder ahora (pregunta, evidencia, aviso o resumen). */
data class ChatNodePrompt(
    val nodeId: String,
    val scope: String = "",
    val kind: String,
    val inputType: TechnicalChatInputType,
    val text: String,
    val options: List<ChatOption> = emptyList(),
    val required: Boolean = true,
    val block: String = "",
    val blockLabel: String = "",
    val blockIndex: Int = 0,
    val blockCount: Int = 0,
    /** "Pregunta N de T": T es una estimación que crece con los loops. */
    val questionNumber: Int = 0,
    val questionTotal: Int = 0,
    val evidenceNumber: Int = 0,
    val evidenceTotal: Int = 0,
    /** Prefill (ej. fecha de hoy en P06, técnico en P07). */
    val defaultValue: String? = null,
    val evidenceCode: String? = null,
    val maxFiles: Int = 3,
    /** Para avisos: "warning" / "info". */
    val severity: String? = null,
    /** Mínimo de opciones a elegir en MULTI_SELECT (validation.minSelected). */
    val minSelected: Int = 0,
    /** Dónde va dentro de lo que se repite, ej. "Caja 1 de 2" o "Cocina (1 de 2)". */
    val context: String = "",
    /** Teclado: "text", "number", "phone" o "date". */
    val keyboard: String = "text",
    /** Texto de ayuda del campo (ej. "Número entre 1 y 20"). */
    val hint: String = ""
)

/** Pregunta de confirmación del asistente tras leer una evidencia (ej. "¿cuál es el proveedor correcto?"). */
data class ChatFollowUp(
    val key: String,
    val text: String,
    val options: List<String> = emptyList()
)

/** Lo que la IA leyó de la evidencia recién subida. */
data class ChatEvidenceResult(
    val code: String,
    val scope: String = "",
    val area: String? = null,
    val equipo: String? = null,
    val count: Int = 0,
    val extracted: Map<String, Any?> = emptyMap()
)

data class TechnicalChatProgress(
    val evaluationId: String,
    /** Estado opaco del flujo: se devuelve tal cual en la siguiente respuesta. */
    val state: String?,
    /** Nodo actual; null cuando el flujo terminó. */
    val node: ChatNodePrompt?,
    val answeredQuestions: Int,
    val totalQuestions: Int,
    val progressPercent: Int,
    val completed: Boolean,
    val answers: Map<String, String>,
    val proposal: TechnicalChatProposal?,
    /** Si no es null, la respuesta no era válida y el nodo no avanzó. */
    val validationError: String? = null,
    val lastEvidence: ChatEvidenceResult? = null,
    /** Avisos de validación cruzada tras subir una evidencia. */
    val crossChecks: List<String> = emptyList(),
    val followUps: List<ChatFollowUp> = emptyList(),
    /** Solo E3: fotos con credenciales desenfocadas (base64). Transitorio: el
     *  ViewModel las escribe a disco y las quita antes de guardar el snapshot. */
    val processedImages: List<String> = emptyList()
)

data class TechnicalChatProposal(
    val summary: String,
    val asIsFindings: List<String> = emptyList(),
    val recommendations: List<String>,
    val equipment: List<TechnicalEquipmentRecommendation>,
    val topologyText: String,
    val topology: ChatTopology? = null,
    val score: Int? = null
)

data class TechnicalEquipmentRecommendation(
    val name: String,
    val description: String,
    val quantity: Int,
    /** Pendiente de catálogo real de precios de IDBI — null hasta entonces. */
    val unitPrice: Double? = null
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
    val level: Int,
    val detail: String? = null,
    val pending: Boolean = false
)

data class ChatTopologyLink(
    val source: String,
    val target: String,
    val connectionType: String,
    val status: String
)