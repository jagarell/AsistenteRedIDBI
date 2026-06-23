package com.upc.asistenteredidbi.domain.usecase

import android.util.Patterns
import com.upc.asistenteredidbi.domain.model.AuthSession
import com.upc.asistenteredidbi.domain.model.User
import com.upc.asistenteredidbi.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, rememberMe: Boolean): Result<AuthSession> {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Ingresa tu contraseña"))
        }
        return repository.login(email, password, rememberMe)
    }
}

class RegisterUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(
        fullName: String,
        email: String,
        phone: String,
        company: String,
        city: String,
        password: String,
        confirmPassword: String,
        acceptedTerms: Boolean
    ): Result<AuthSession> {
        if (fullName.isBlank()) return Result.failure(IllegalArgumentException("Ingresa tu nombre completo"))
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido"))
        }
        if (phone.isBlank()) return Result.failure(IllegalArgumentException("Ingresa tu número de teléfono"))
        if (company.isBlank()) return Result.failure(IllegalArgumentException("Ingresa el nombre de tu empresa"))
        if (city.isBlank()) return Result.failure(IllegalArgumentException("Ingresa tu ciudad"))
        if (password.length < 8) return Result.failure(IllegalArgumentException("La contraseña debe tener al menos 8 caracteres"))
        if (password != confirmPassword) return Result.failure(IllegalArgumentException("Las contraseñas no coinciden"))
        if (!acceptedTerms) return Result.failure(IllegalArgumentException("Debes aceptar los Términos y Condiciones"))

        return repository.register(fullName.trim(), email.trim(), phone.trim(), company.trim(), city.trim(), password, confirmPassword)
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
