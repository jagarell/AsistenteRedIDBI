package com.upc.asistenteredidbi.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class LoginResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresInMinutes: Int,
    val userId: Long,
    val fullName: String,
    val email: String,
    val role: String
)

data class RefreshRequestDto(
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: Long,
    val fullName: String,
    val email: String,
    val phone: String?,
    val company: String?,
    val city: String?,
    val role: String
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequestDto(
    @Json(name = "full_name") val fullName: String?,
    @Json(name = "phone") val phone: String?,
    @Json(name = "company") val company: String?,
    @Json(name = "city") val city: String?
)

@JsonClass(generateAdapter = true)
data class ProfileStatsDto(
    val totalEvaluations: Int,
    val evaluationsThisMonth: Int = 0,
    val totalProposals: Int,
    val sentProposals: Int = 0
)

@JsonClass(generateAdapter = true)
data class BrandAssetDto(
    @Json(name = "logo_url") val logoUrl: String?,
    @Json(name = "company_name") val companyName: String?
)

@JsonClass(generateAdapter = true)
data class GenericMessageDto(@Json(name = "message") val message: String)
