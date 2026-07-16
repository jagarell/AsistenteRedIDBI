package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.MinutaRecordRequestDto
import com.upc.asistenteredidbi.data.remote.dto.MinutaRecordResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Servicio Retrofit — refleja `MinutaController` del gateway (`/api/minutas`). */
interface MinutaApiService {

    @GET("api/minutas")
    suspend fun listMinutas(
        @Query("status") status: String? = null,
        @Query("technicianId") technicianId: Long? = null
    ): List<MinutaRecordResponseDto>

    @GET("api/minutas/{id}")
    suspend fun getMinuta(@Path("id") id: Long): MinutaRecordResponseDto

    /** Crea la minuta en estado BORRADOR (técnico autor = usuario autenticado). */
    @POST("api/minutas")
    suspend fun createMinuta(@Body request: MinutaRecordRequestDto): MinutaRecordResponseDto

    /** Marca la minuta como COMPLETA (lista para validación). */
    @POST("api/minutas/{id}/completar")
    suspend fun completeMinuta(@Path("id") id: Long): MinutaRecordResponseDto

    /** Valida una minuta COMPLETA. El backend exige rol SUPERVISOR. */
    @POST("api/minutas/{id}/validar")
    suspend fun validateMinuta(@Path("id") id: Long): MinutaRecordResponseDto
}
