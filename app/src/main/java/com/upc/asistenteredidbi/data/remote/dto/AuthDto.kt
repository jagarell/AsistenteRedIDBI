package com.upc.asistenteredidbi.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequestDto(
    @Json(name = "full_name") val fullName: String,
    @Json(name = "email") val email: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "company") val company: String,
    @Json(name = "city") val city: String,
    @Json(name = "password") val password: String,
    @Json(name = "confirm_password") val confirmPassword: String
)

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "remember_me") val rememberMe: Boolean
)

@JsonClass(generateAdapter = true)
data class TokenResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "expires_in_minutes") val expiresInMinutes: Int
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "email") val email: String,
    @Json(name = "phone") val phone: String?,
    @Json(name = "company") val company: String?,
    @Json(name = "city") val city: String?,
    @Json(name = "role") val role: String
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
    @Json(name = "total_evaluations") val totalEvaluations: Int,
    @Json(name = "evaluations_this_month") val evaluationsThisMonth: Int,
    @Json(name = "total_proposals") val totalProposals: Int
)

@JsonClass(generateAdapter = true)
data class BrandAssetDto(
    @Json(name = "logo_url") val logoUrl: String?,
    @Json(name = "company_name") val companyName: String?
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequestDto(@Json(name = "email") val email: String)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequestDto(
    @Json(name = "token") val token: String,
    @Json(name = "new_password") val newPassword: String,
    @Json(name = "confirm_password") val confirmPassword: String
)

@JsonClass(generateAdapter = true)
data class GenericMessageDto(@Json(name = "message") val message: String)
