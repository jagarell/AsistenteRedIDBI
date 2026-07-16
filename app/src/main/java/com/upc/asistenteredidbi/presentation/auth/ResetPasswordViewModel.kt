package com.upc.asistenteredidbi.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.domain.usecase.ResetPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResetPasswordUiState(
    val isLoading: Boolean = false,
    val passwordChanged: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(ResetPasswordUiState())

    val uiState: StateFlow<ResetPasswordUiState> =
        _uiState.asStateFlow()

    fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
        confirmPassword: String
    ) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    passwordChanged = false,
                    successMessage = null,
                    errorMessage = null
                )
            }

            resetPasswordUseCase(
                email = email,
                code = code,
                newPassword = newPassword,
                confirmPassword = confirmPassword
            ).onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        passwordChanged = true,
                        successMessage = response.message
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        passwordChanged = false,
                        errorMessage = error.message
                            ?: "No se pudo actualizar la contraseña"
                    )
                }
            }
        }
    }

    fun consumePasswordChanged() {
        _uiState.update {
            it.copy(
                passwordChanged = false,
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