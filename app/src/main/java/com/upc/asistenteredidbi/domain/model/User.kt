package com.upc.asistenteredidbi.domain.model

/** Modelos de dominio para HU01 (autenticación) y Perfil. */

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String?,
    val company: String?,
    val city: String?,
    val role: String
)

data class AuthSession(
    val accessToken: String,
    val expiresInMinutes: Int
)

data class ProfileStats(
    val totalEvaluations: Int,
    val evaluationsThisMonth: Int,
    val totalProposals: Int
)

data class BrandAsset(
    val logoUrl: String?,
    val companyName: String?
)
