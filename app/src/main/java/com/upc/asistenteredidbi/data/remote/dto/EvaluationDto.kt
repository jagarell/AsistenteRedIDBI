package com.upc.asistenteredidbi.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StartEvaluationRequestDto(
    @Json(name = "establishment_name") val establishmentName: String,
    @Json(name = "establishment_address") val establishmentAddress: String?,
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "client_name") val clientName: String?,
    @Json(name = "client_phone") val clientPhone: String?
)

@JsonClass(generateAdapter = true)
data class EvaluationDto(
    @Json(name = "id") val id: String,
    @Json(name = "establishment_id") val establishmentId: String,
    @Json(name = "establishment_name") val establishmentName: String,
    @Json(name = "establishment_address") val establishmentAddress: String?,
    @Json(name = "google_maps_url") val googleMapsUrl: String?,
    @Json(name = "status") val status: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class EvaluationListItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "establishment_name") val establishmentName: String,
    @Json(name = "client_name") val clientName: String?,
    @Json(name = "status") val status: String,
    @Json(name = "overall_score") val overallScore: Float?,
    @Json(name = "created_at") val createdAt: String
)
