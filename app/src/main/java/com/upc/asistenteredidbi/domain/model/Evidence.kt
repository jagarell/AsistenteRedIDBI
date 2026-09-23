package com.upc.asistenteredidbi.domain.model

/**
 * Modelos de dominio del checklist dinámico de Evidencias Técnicas (Fase A
 * selección / Fase B captura multi-foto), sembrado desde las respuestas
 * reales del chat técnico — ver `EvidenceChecklistService` en
 * idbi-api-gateway y `app.chat.checklist` en idbi-fastapi.
 */

data class EvidencePhoto(
    val id: Long,
    val fileUrl: String,
    val comment: String?,
    val capturedAt: String,
    /** Descripción libre del análisis de IA para esta foto (null en fotos de
     *  área, que no pasan por análisis). */
    val analysisResult: String?
)

data class EvidenceAreaItem(
    val id: Long,
    val name: String,
    val isCustom: Boolean,
    val photos: List<EvidencePhoto>
)

data class EvidenceEquipmentItem(
    val id: Long,
    val equipmentType: String,
    val label: String,
    val isCustom: Boolean,
    val extractedSpecs: Map<String, Any?>?,
    val technicianNotes: String?,
    val photos: List<EvidencePhoto>
)

data class EvidenceChecklist(
    val evaluationId: Long,
    val selectionLocked: Boolean,  // false = Fase A (selección), true = Fase B (captura obligatoria)
    val areas: List<EvidenceAreaItem>,
    val equipment: List<EvidenceEquipmentItem>,
    val allItemsHavePhoto: Boolean  // habilita "Analizar con IA" cuando selectionLocked=true
)

/** Catálogo de tipos de equipo reconocidos — igual al backend
 * (app.vision._EQUIPMENT_CATEGORIES en idbi-fastapi, más "otro"). */
object EquipmentTypeCatalog {
    val TYPES: List<Pair<String, String>> = listOf(
        "router" to "Router",
        "switch" to "Switch",
        "pos" to "POS / caja",
        "printer" to "Impresora / ticketera",
        "camera" to "Cámara de seguridad",
        "computer" to "Computadora / laptop",
        "access_point" to "Access point WiFi",
        "otro" to "Otro equipo"
    )
}
