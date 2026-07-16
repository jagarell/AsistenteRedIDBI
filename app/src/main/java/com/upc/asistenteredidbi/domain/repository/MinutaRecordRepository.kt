package com.upc.asistenteredidbi.domain.repository

import com.upc.asistenteredidbi.domain.model.MinutaRecord

interface MinutaRecordRepository {

    /** Todas las minutas visibles para el usuario actual (incl. borradores). */
    suspend fun listMinutas(): Result<List<MinutaRecord>>

    /** Crea la minuta en BORRADOR (p. ej. al completarse el chat de 20 nodos). */
    suspend fun createMinuta(
        evaluationId: Long?,
        clientName: String,
        address: String?,
        summary: String?,
        topologyJson: String?,
        contentJson: String?
    ): Result<MinutaRecord>

    suspend fun completeMinuta(id: Long): Result<MinutaRecord>

    /** Sólo debe invocarse si el usuario actual tiene rol SUPERVISOR. */
    suspend fun validateMinuta(id: Long): Result<MinutaRecord>
}
