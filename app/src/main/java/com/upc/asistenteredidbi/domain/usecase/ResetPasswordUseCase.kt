package com.upc.asistenteredidbi.domain.usecase

import com.upc.asistenteredidbi.domain.model.ResetPasswordResult
import com.upc.asistenteredidbi.domain.repository.AuthRepository
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(
        email: String,
        code: String,
        newPassword: String,
        confirmPassword: String
    ): Result<ResetPasswordResult> {
        return repository.resetPassword(
            email = email,
            code = code,
            newPassword = newPassword,
            confirmPassword = confirmPassword
        )
    }
}