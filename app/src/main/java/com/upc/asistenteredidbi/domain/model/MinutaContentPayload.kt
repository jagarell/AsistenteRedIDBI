package com.upc.asistenteredidbi.domain.model

/**
 * Contenido de una minuta (equipo recomendado, recomendaciones y score) que se
 * serializa a JSON y se guarda en `Minuta.contentJson` al crearla.
 */
data class MinutaContentPayload(
    val equipment: List<TechnicalEquipmentRecommendation>,
    val recommendations: List<String>,
    val score: Int?
)
