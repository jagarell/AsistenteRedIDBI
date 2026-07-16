package com.upc.asistenteredidbi.domain.usecase

import android.util.Patterns
import com.upc.asistenteredidbi.domain.model.AuthSession
import com.upc.asistenteredidbi.domain.model.RegisterResult
import com.upc.asistenteredidbi.domain.model.User
import com.upc.asistenteredidbi.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(
        email: String,
        password: String
    ): Result<AuthSession> {
        return repository.login(email, password)
    }
}

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(
        fullName: String,
        email: String,
        phone: String,
        company: String,
        city: String,
        password: String,
        confirmPassword: String,
        role: String = "TECNICO"
    ): Result<RegisterResult> {
        return repository.register(
            fullName = fullName,
            email = email,
            phone = phone,
            company = company,
            city = city,
            password = password,
            confirmPassword = confirmPassword,
            role = role
        )
    }
}

class RequestPasswordResetUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<String> {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido"))
        }
        return repository.requestPasswordReset(email)
    }
}

class GetCurrentUserUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<User> = repository.getCurrentUser()
}

class LogoutUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.logout()
}
