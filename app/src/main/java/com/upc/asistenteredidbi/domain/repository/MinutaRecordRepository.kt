package com.upc.asistenteredidbi.domain.repository

import com.upc.asistenteredidbi.domain.model.MinutaRecord

interface MinutaRecordRepository {

    /** Todas las minutas visibles para el usuario actual (incl. borradores). */
    suspend fun listMinutas(): Result<List<MinutaRecord>>

    suspend fun getMinuta(id: Long): Result<MinutaRecord>

    /** Crea la minuta en BORRADOR (p. ej. al completarse el chat de 20 nodos). */
    suspend fun createMinuta(
        evaluationId: Long?,
        clientName: String,
        address: String?,
        summary: String?,
        topologyJson: String?,
        contentJson: String?
    ): Result<MinutaRecord>

    /**
     * Actualización parcial: los parámetros nulos no se tocan en el servidor
     * (no borra el resumen/topología/equipo ya generados por el chat).
     */
    suspend fun updateMinuta(
        id: Long,
        clientName: String,
        address: String?,
        contactName: String?,
        contactPhone: String?
    ): Result<MinutaRecord>

    suspend fun completeMinuta(id: Long): Result<MinutaRecord>

    /** Sólo debe invocarse si el usuario actual tiene rol SUPERVISOR. */
    suspend fun validateMinuta(id: Long): Result<MinutaRecord>
}
