package com.upc.asistenteredidbi.data.remote.dto

data class ForgotPasswordRequestDto(
    val email: String
)

data class ForgotPasswordResponseDto(
    val message: String,
    val expiresInMinutes: Int
)

data class ResetPasswordRequestDto(
    val email: String,
    val code: String,
    val newPassword: String,
    val confirmPassword: String
)

data class ResetPasswordResponseDto(
    val message: String
)