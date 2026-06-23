package com.upc.asistenteredidbi.domain.model

/**
 * Modelos de dominio de Evidencias Técnicas (HU04, dos fases).
 * Reemplaza el antiguo `EvidenceItem` (1 foto por ítem, 7 tipos fijos).
 */

data class EvidencePhoto(
    val id: String,
    val fileUrl: String,
    val comment: String?,
    val capturedAt: String
)

data class EvidenceAreaItem(
    val id: String,
    val name: String,
    val isCustom: Boolean,
    val photos: List<EvidencePhoto>
)

data class EvidenceEquipmentItem(
    val id: String,
    val equipmentType: String,
    val label: String,
    val isCustom: Boolean,
    val extractedSpecs: Map<String, Any?>?,
    val technicianNotes: String?,
    val photos: List<EvidencePhoto>
)

data class EvidenceChecklist(
    val evaluationId: String,
    val selectionLocked: Boolean,  // false = Fase A (selección), true = Fase B (captura obligatoria)
    val areas: List<EvidenceAreaItem>,
    val equipment: List<EvidenceEquipmentItem>,
    val allItemsHavePhoto: Boolean  // habilita "Analizar con IA" cuando selectionLocked=true
)

/** Catálogo de tipos de equipo reconocidos (igual al backend, EQUIPMENT_TYPES). */
object EquipmentTypeCatalog {
    val TYPES: List<Pair<String, String>> = listOf(
        "router" to "Router",
        "switch" to "Switch",
        "impresora_ticketera" to "Impresora ticketera",
        "servidor" to "Servidor",
        "pc" to "PC",
        "laptop" to "Laptop",
        "tablet" to "Tablet",
        "repetidor" to "Repetidor",
        "terminal_impresora" to "Terminal con impresora integrada",
        "raspberry" to "Raspberry",
        "otro" to "Otro equipo"
    )
}

/** Opciones predefinidas de áreas del local (igual al backend, PRESET_AREA_NAMES). */
object LocalAreaCatalog {
    val PRESET_NAMES: List<String> = listOf(
        "Caja", "Barra", "Cocina", "Piso 01", "Piso 02", "Piso 03",
        "Zona de Jugos", "Cafetería", "Zona Calientes", "Zona de Fríos"
    )
}
