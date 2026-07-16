package com.upc.asistenteredidbi.domain.usecase

import com.upc.asistenteredidbi.domain.model.ForgotPasswordResult
import com.upc.asistenteredidbi.domain.repository.AuthRepository
import javax.inject.Inject

class ForgotPasswordUseCase @Inject constructor(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(
        email: String
    ): Result<ForgotPasswordResult> {
        return repository.forgotPassword(email)
    }
}