package com.upc.asistenteredidbi.data.repository

import com.upc.asistenteredidbi.data.mapper.toDomain
import com.upc.asistenteredidbi.data.mapper.toRegisterResult
import com.upc.asistenteredidbi.data.remote.AuthApiService
import com.upc.asistenteredidbi.data.remote.dto.LoginRequestDto
import com.upc.asistenteredidbi.data.remote.dto.RegisterRequestDto
import com.upc.asistenteredidbi.domain.model.AuthSession
import com.upc.asistenteredidbi.domain.model.RegisterResult
import com.upc.asistenteredidbi.domain.model.User
import com.upc.asistenteredidbi.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
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
        confirmPassword: String
    ): Result<RegisterResult> = safeCall {
        api.register(
            RegisterRequestDto(
                fullName = fullName,
                email = email,
                phone = phone,
                company = company,
                city = city,
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

    private suspend fun <T> safeCall(
        block: suspend () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (exception: HttpException) {
            Result.failure(
                IllegalArgumentException(
                    extractErrorMessage(exception)
                )
            )
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private fun extractErrorMessage(
        exception: HttpException
    ): String {
        return try {
            val errorBody = exception.response()
                ?.errorBody()
                ?.string()

            if (errorBody.isNullOrBlank()) {
                defaultErrorMessage(exception.code())
            } else {
                JSONObject(errorBody).optString(
                    "message",
                    defaultErrorMessage(exception.code())
                )
            }
        } catch (_: Exception) {
            defaultErrorMessage(exception.code())
        }
    }

    private fun defaultErrorMessage(code: Int): String {
        return when (code) {
            400 -> "Correo o contraseña incorrectos"
            401 -> "Tu sesión no está autorizada"
            403 -> "No tienes permisos para realizar esta acción"
            404 -> "El servicio solicitado no fue encontrado"
            409 -> "Los datos ingresados ya existen"
            500 -> "Ocurrió un error en el servidor"
            else -> "No se pudo completar la solicitud"
        }
    }
}