package com.upc.asistenteredidbi.data.repository

import com.upc.asistenteredidbi.data.mapper.toDomain
import com.upc.asistenteredidbi.data.remote.AuthApiService
import com.upc.asistenteredidbi.data.remote.dto.ForgotPasswordRequestDto
import com.upc.asistenteredidbi.data.remote.dto.LoginRequestDto
import com.upc.asistenteredidbi.data.remote.dto.RegisterRequestDto
import com.upc.asistenteredidbi.data.remote.dto.ResetPasswordRequestDto
import com.upc.asistenteredidbi.data.session.SessionManager
import com.upc.asistenteredidbi.domain.model.AuthSession
import com.upc.asistenteredidbi.domain.model.User
import com.upc.asistenteredidbi.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        company: String,
        city: String,
        password: String,
        confirmPassword: String
    ): Result<AuthSession> = safeCall {
        val dto = api.register(
            RegisterRequestDto(
                fullName,
                email,
                phone,
                company,
                city,
                password,
                confirmPassword
            )
        )
        sessionManager.saveAccessToken(dto.accessToken)
        dto.toDomain()
    }

    override suspend fun login(email: String, password: String, rememberMe: Boolean): Result<AuthSession> = safeCall {
        val dto = api.login(LoginRequestDto(email, password, rememberMe))
        sessionManager.saveAccessToken(dto.accessToken)
        dto.toDomain()
    }

    override suspend fun getCurrentUser(): Result<User> = safeCall { api.getCurrentUser().toDomain() }

    override suspend fun requestPasswordReset(email: String): Result<String> = safeCall {
        api.forgotPassword(ForgotPasswordRequestDto(email)).message
    }

    override suspend fun resetPassword(token: String, newPassword: String, confirmPassword: String): Result<String> = safeCall {
        api.resetPassword(ResetPasswordRequestDto(token, newPassword, confirmPassword)).message
    }

    override suspend fun logout() {
        sessionManager.clearSession()
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
