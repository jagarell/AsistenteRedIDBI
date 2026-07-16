package com.upc.asistenteredidbi.domain.model

data class RegisterResult(
    val id: Long,
    val fullName: String,
    val email: String,
    val phone: String,
    val company: String,
    val city: String,
    val role: Role,
    val message: String
)