package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.CreateCustomAreaRequestDto
import com.upc.asistenteredidbi.data.remote.dto.CreateCustomEquipmentRequestDto
import com.upc.asistenteredidbi.data.remote.dto.EvidenceAreaItemDto
import com.upc.asistenteredidbi.data.remote.dto.EvidenceChecklistDto
import com.upc.asistenteredidbi.data.remote.dto.EvidenceEquipmentItemDto
import com.upc.asistenteredidbi.data.remote.dto.EvidencePhotoDto
import com.upc.asistenteredidbi.data.remote.dto.MinutaDto
import com.upc.asistenteredidbi.data.remote.dto.UpdateEquipmentNotesRequestDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Servicio Retrofit del checklist dinámico de evidencias (Fase A selección /
 * Fase B captura multi-foto) — refleja `EvidenceChecklistController` +
 * `EvidenceChecklistService` en idbi-api-gateway (`com.upc.idbi.gateway.evidence.checklist`).
 */
interface EvidenceApiService {

    @GET("api/v1/evaluations/{evaluationId}/evidence/checklist")
    suspend fun getChecklist(@Path("evaluationId") evaluationId: Long): EvidenceChecklistDto

    // --- Fase A: selección ---

    @POST("api/v1/evaluations/{evaluationId}/evidence/areas")
    suspend fun addCustomArea(
        @Path("evaluationId") evaluationId: Long,
        @Body request: CreateCustomAreaRequestDto
    ): EvidenceAreaItemDto

    @DELETE("api/v1/evaluations/{evaluationId}/evidence/areas/{areaId}")
    suspend fun deleteArea(@Path("evaluationId") evaluationId: Long, @Path("areaId") areaId: Long)

    @POST("api/v1/evaluations/{evaluationId}/evidence/equipment")
    suspend fun addCustomEquipment(
        @Path("evaluationId") evaluationId: Long,
        @Body request: CreateCustomEquipmentRequestDto
    ): EvidenceEquipmentItemDto

    @DELETE("api/v1/evaluations/{evaluationId}/evidence/equipment/{equipmentId}")
    suspend fun deleteEquipment(@Path("evaluationId") evaluationId: Long, @Path("equipmentId") equipmentId: Long)

    @POST("api/v1/evaluations/{evaluationId}/evidence/lock")
    suspend fun lockSelection(@Path("evaluationId") evaluationId: Long): EvidenceChecklistDto

    // --- Fase B: captura ---

    @Multipart
    @POST("api/v1/evaluations/{evaluationId}/evidence/areas/{areaId}/photos")
    suspend fun uploadAreaPhoto(
        @Path("evaluationId") evaluationId: Long,
        @Path("areaId") areaId: Long,
        @Part("comment") comment: RequestBody?,
        @Part file: MultipartBody.Part
    ): EvidencePhotoDto

    @Multipart
    @POST("api/v1/evaluations/{evaluationId}/evidence/equipment/{equipmentId}/photos")
    suspend fun uploadEquipmentPhoto(
        @Path("evaluationId") evaluationId: Long,
        @Path("equipmentId") equipmentId: Long,
        @Part("comment") comment: RequestBody?,
        @Part file: MultipartBody.Part
    ): EvidencePhotoDto

    @DELETE("api/v1/evaluations/{evaluationId}/evidence/photos/{photoId}")
    suspend fun deletePhoto(@Path("evaluationId") evaluationId: Long, @Path("photoId") photoId: Long)

    @PATCH("api/v1/evaluations/{evaluationId}/evidence/equipment/{equipmentId}")
    suspend fun updateEquipmentNotes(
        @Path("evaluationId") evaluationId: Long,
        @Path("equipmentId") equipmentId: Long,
        @Body request: UpdateEquipmentNotesRequestDto
    ): EvidenceEquipmentItemDto

    // --- Minuta (vista consolidada de solo lectura) ---

    @GET("api/v1/evaluations/{evaluationId}/minuta")
    suspend fun getMinuta(@Path("evaluationId") evaluationId: Long): MinutaDto
}
