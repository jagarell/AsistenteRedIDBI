package com.upc.asistenteredidbi.data.repository

import com.upc.asistenteredidbi.data.mapper.toDomain
import com.upc.asistenteredidbi.data.mapper.toRegisterResult
import com.upc.asistenteredidbi.data.remote.AuthApiService
import com.upc.asistenteredidbi.data.remote.dto.ForgotPasswordRequestDto
import com.upc.asistenteredidbi.data.remote.dto.LoginRequestDto
import com.upc.asistenteredidbi.data.remote.dto.RegisterRequestDto
import com.upc.asistenteredidbi.data.remote.dto.ResetPasswordRequestDto
import com.upc.asistenteredidbi.data.remote.toFriendlyMessage
import com.upc.asistenteredidbi.domain.model.AuthSession
import com.upc.asistenteredidbi.domain.model.ForgotPasswordResult
import com.upc.asistenteredidbi.domain.model.RegisterResult
import com.upc.asistenteredidbi.domain.model.ResetPasswordResult
import com.upc.asistenteredidbi.domain.model.User
import com.upc.asistenteredidbi.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): Result<AuthSession> = safeCall {
        api.login(
            LoginRequestDto(
                email = email,
                password = password
            )
        ).toDomain()
    }

    override suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        company: String,
        city: String,
        password: String,
        confirmPassword: String,
        role: String
    ): Result<RegisterResult> = safeCall {
        api.register(
            RegisterRequestDto(
                fullName = fullName,
                email = email,
                phone = phone,
                company = company,
                city = city,
                role = role,
                password = password,
                confirmPassword = confirmPassword
            )
        ).toRegisterResult()
    }

    override suspend fun getCurrentUser(): Result<User> {
        TODO("Not yet implemented")
    }

    override suspend fun requestPasswordReset(email: String): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun resetPassword(
        token: String,
        newPassword: String,
        confirmPassword: String
    ): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun logout() {
        TODO("Not yet implemented")
    }

    override suspend fun forgotPassword(
        email: String
    ): Result<ForgotPasswordResult> = safeCall {
        api.forgotPassword(
            ForgotPasswordRequestDto(
                email = email
            )
        ).toDomain()
    }

    override suspend fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
        confirmPassword: String
    ): Result<ResetPasswordResult> = safeCall {
        api.resetPassword(
            ResetPasswordRequestDto(
                email = email,
                code = code,
                newPassword = newPassword,
                confirmPassword = confirmPassword
            )
        ).toDomain()
    }

    private suspend fun <T> safeCall(
        block: suspend () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (exception: Exception) {
            Result.failure(
                IllegalArgumentException(
                    exception.toFriendlyMessage(codeOverrides = LOGIN_ERROR_OVERRIDES),
                    exception
                )
            )
        }
    }

    private companion object {
        // Mensajes específicos del contexto de login/registro — acá un
        // 401/403 significa credenciales inválidas, no sesión expirada
        // (todavía no hay sesión), así que no usan el default genérico.
        val LOGIN_ERROR_OVERRIDES = mapOf(
            400 to "Correo o contraseña incorrectos",
            401 to "Tu sesión no está autorizada",
            403 to "No tienes permisos para realizar esta acción",
            404 to "El servicio solicitado no fue encontrado",
            409 to "Los datos ingresados ya existen",
            500 to "Ocurrió un error en el servidor",
        )
    }
}