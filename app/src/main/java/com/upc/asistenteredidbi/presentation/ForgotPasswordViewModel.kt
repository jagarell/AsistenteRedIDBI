package com.upc.asistenteredidbi.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.domain.usecase.ForgotPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val codeSent: Boolean = false,
    val expiresInMinutes: Int = 0,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val forgotPasswordUseCase: ForgotPasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> =
        _uiState.asStateFlow()

    fun requestCode(email: String) {
        if (_uiState.value.isLoading) return

        val normalizedEmail = email.trim()

        if (normalizedEmail.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Ingresa tu correo"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    codeSent = false,
                    expiresInMinutes = 0,
                    successMessage = null,
                    errorMessage = null
                )
            }

            forgotPasswordUseCase(normalizedEmail)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            codeSent = true,
                            expiresInMinutes = result.expiresInMinutes,
                            successMessage = result.message,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            codeSent = false,
                            errorMessage = error.message
                                ?: "No se pudo generar el código de recuperación"
                        )
                    }
                }
        }
    }

    fun consumeCodeSent() {
        _uiState.update {
            it.copy(
                codeSent = false,
                successMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }
}