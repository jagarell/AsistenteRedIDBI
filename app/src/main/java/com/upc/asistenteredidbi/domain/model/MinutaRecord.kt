package com.upc.asistenteredidbi.domain.model

/**
 * Minuta técnica (documento del levantamiento de red de un cliente), tal como
 * la gestiona el gateway (`com.upc.idbi.gateway.minuta`). No confundir con
 * [Minuta] (vista de solo lectura de UNA evaluación): esta es la entidad con
 * ciclo de vida BORRADOR → COMPLETA → VALIDADA que puede listarse y validarse.
 *
 * Visibilidad por diseño: todas las minutas (incluidas las BORRADOR) son
 * visibles para cualquier técnico o supervisor, para que un técnico pueda
 * continuar la minuta que otro dejó a medias. Sólo un SUPERVISOR puede
 * pasar una minuta COMPLETA a VALIDADA.
 */
enum class MinutaRecordStatus {
    BORRADOR,
    COMPLETA,
    VALIDADA;

    companion object {
        fun fromApiValue(value: String?): MinutaRecordStatus =
            values().firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }
                ?: BORRADOR
    }
}

data class MinutaRecord(
    val id: Long,
    val evaluationId: Long?,
    val clientName: String,
    val address: String?,
    val technicianId: Long?,
    val technicianName: String?,
    val status: MinutaRecordStatus,
    val summary: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val validatedById: Long?,
    val validatedByName: String?,
    val validatedAt: String?
)
