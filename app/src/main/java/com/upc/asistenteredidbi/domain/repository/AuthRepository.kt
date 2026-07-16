package com.upc.asistenteredidbi.domain.repository

import com.upc.asistenteredidbi.data.remote.dto.RegisterResponseDto
import com.upc.asistenteredidbi.domain.model.AuthSession
import com.upc.asistenteredidbi.domain.model.ForgotPasswordResult
import com.upc.asistenteredidbi.domain.model.RegisterResult
import com.upc.asistenteredidbi.domain.model.ResetPasswordResult
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
        confirmPassword: String,
        role: String = "TECNICO"
    ): Result<RegisterResult>

    suspend fun getCurrentUser(): Result<User>
    suspend fun requestPasswordReset(email: String): Result<String>
    suspend fun resetPassword(token: String, newPassword: String, confirmPassword: String): Result<String>
    suspend fun logout()
    suspend fun forgotPassword(
        email: String
    ): Result<ForgotPasswordResult>

    suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
        confirmPassword: String
    ): Result<ResetPasswordResult>
}
