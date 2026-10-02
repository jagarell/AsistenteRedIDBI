package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.domain.model.NetworkMap
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Refleja `MapController` del gateway. */
interface MapApiService {

    @GET("api/evaluations/{evaluationId}/map")
    suspend fun getMap(@Path("evaluationId") evaluationId: Long): MapEnvelopeDto

    @PUT("api/evaluations/{evaluationId}/map")
    suspend fun saveMap(@Path("evaluationId") evaluationId: Long, @Body map: NetworkMap): NetworkMap

    @POST("api/evaluations/{evaluationId}/map/generate")
    suspend fun generateMap(@Path("evaluationId") evaluationId: Long): NetworkMap

    @POST("api/evaluations/{evaluationId}/map/command")
    suspend fun mapCommand(
        @Path("evaluationId") evaluationId: Long,
        @Body request: MapCommandRequestDto
    ): MapCommandResponseDto
}

data class MapEnvelopeDto(val map: NetworkMap? = null)

data class MapCommandRequestDto(val map: NetworkMap, val command: String)

data class MapCommandResponseDto(val map: NetworkMap, val reply: String)
