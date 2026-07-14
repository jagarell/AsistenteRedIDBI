package com.upc.asistenteredidbi.domain.repository

import com.upc.asistenteredidbi.data.remote.dto.RegisterResponseDto
import com.upc.asistenteredidbi.domain.model.AuthSession
import com.upc.asistenteredidbi.domain.model.RegisterResult
import com.upc.asistenteredidbi.domain.model.User

interface AuthRepository {

    suspend fun login(
        email: String,
        password: String
    ): Result<AuthSession>

    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        company: String,
        city: String,
        password: String,
        confirmPassword: String
    ): Result<RegisterResult>

    suspend fun getCurrentUser(): Result<User>
    suspend fun requestPasswordReset(email: String): Result<String>
    suspend fun resetPassword(token: String, newPassword: String, confirmPassword: String): Result<String>
    suspend fun logout()
}
