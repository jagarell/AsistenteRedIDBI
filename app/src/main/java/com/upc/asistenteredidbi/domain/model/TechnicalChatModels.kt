package com.upc.asistenteredidbi.domain.model

/** Tipos de nodo del flujo de minuta técnica (ver `app/chat/flow_engine.py` en idbi-fastapi). */
enum class TechnicalChatInputType {
    TEXT, NUMBER, CHOICE, MULTI_SELECT, YES_NO, EVIDENCE, ALERT, SUMMARY,
    /** Cuadro de datos que se valida (router): Correcto / Corregir. */
    READ_CARD,
    /** Enlaces de video del Anexo B (varios). */
    LINKS,
    /** Pasos a seguir editables. */
    STEPS,
    /** Medición de WiFi por zona. */
    WIFI_MEASURE;

    companion object {
        fun fromApiValue(value: String?): TechnicalChatInputType =
            values().firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: TEXT
    }
}

/** Opción de una pregunta: `value` es lo que se manda al motor, `label` lo que se muestra. */
data class ChatOption(
    val value: String,
    val label: String,
    /** "Otro (escribir)": al elegirla se pide el texto. */
    val other: Boolean = false,
    /** El texto de "Otro" es un número (ej. cantidad de cajas). */
    val numeric: Boolean = false
)

/** Campo editable de una lectura: `type` = number | text | sel. */
data class ChatFieldSpec(
    val key: String,
    val label: String,
    val type: String = "number",
    val options: List<ChatOption> = emptyList()
)

/** Estado de la foto que se pide: WAITING, REVIEW (hay lectura por validar), IL (no legible), NC (no corresponde) o WRONG. */
data class ChatEvidencePrompt(
    val read: String = "",
    val name: String = "",
    val status: String = "WAITING",
    val message: String? = null,
    val fields: List<ChatFieldSpec> = emptyList(),
    val data: Map<String, Any?> = emptyMap(),
    val label: String = "",
    val unavailable: Boolean = false,
    val mode: String = "subir",
    val lastTry: String? = null
)

/** Aviso del asistente entre la respuesta y la siguiente pregunta. `tone`: info, warn o bad. */
data class ChatNotice(val id: String = "", val tone: String = "info", val tag: String = "", val text: String)

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
    /** false en las evidencias obligatorias: no se muestra "Omitir". */
    val skippable: Boolean = true,
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
    val hint: String = "",
    /** Respuestas rápidas de una pregunta de texto (ej. "Saltar"). */
    val suggestions: List<String> = emptyList(),
    /** MULTI_SELECT: opciones que vienen marcadas por el rubro. */
    val selected: List<String> = emptyList(),
    /** MULTI_SELECT: texto del botón "ninguno" (ej. "Ninguna"). */
    val noneLabel: String? = null,
    val allowOther: Boolean = false,
    val multiline: Boolean = false,
    val unit: String? = null,
    /** Foto opcional (zonas de atención): se puede saltar. */
    val optional: Boolean = false,
    val evidence: ChatEvidencePrompt? = null,
    /** READ_CARD: campos del cuadro. */
    val fields: List<ChatFieldSpec> = emptyList(),
    /** READ_CARD y STEPS: valor inicial como JSON. */
    val valueJson: String? = null,
    val zones: List<String> = emptyList(),
    val videoTypes: List<String> = emptyList(),
    val evidencesTotal: Int = 0
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
    /** Solo la etiqueta del router: fotos con credenciales desenfocadas (base64). Transitorio: el
     *  ViewModel las escribe a disco y las quita antes de guardar el snapshot. */
    val processedImages: List<String> = emptyList(),
    /** Avisos para mostrar entre la respuesta y la siguiente pregunta. */
    val notices: List<ChatNotice> = emptyList()
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