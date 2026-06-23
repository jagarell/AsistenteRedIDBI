package com.upc.asistenteredidbi.domain.repository

import com.upc.asistenteredidbi.domain.model.AuthSession
import com.upc.asistenteredidbi.domain.model.User

interface AuthRepository {
    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        company: String,
        city: String,
        password: String,
        confirmPassword: String
    ): Result<AuthSession>

    suspend fun login(email: String, password: String, rememberMe: Boolean): Result<AuthSession>
    suspend fun getCurrentUser(): Result<User>
    suspend fun requestPasswordReset(email: String): Result<String>
    suspend fun resetPassword(token: String, newPassword: String, confirmPassword: String): Result<String>
    suspend fun logout()
}
