package com.upc.asistenteredidbi.domain.model

data class ForgotPasswordResult(
    val message: String,
    val expiresInMinutes: Int
)