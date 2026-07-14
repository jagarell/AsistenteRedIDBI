package com.upc.asistenteredidbi.data.remote.dto

data class RegisterRequestDto(
    val fullName: String,
    val email: String,
    val phone: String,
    val company: String,
    val city: String,
    val password: String,
    val confirmPassword: String
)

data class RegisterResponseDto(
    val id: Long,
    val fullName: String,
    val email: String,
    val phone: String,
    val company: String,
    val city: String,
    val role: String,
    val message: String
)