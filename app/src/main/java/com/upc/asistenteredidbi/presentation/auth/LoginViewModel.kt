package com.upc.asistenteredidbi.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.data.session.SessionManager
import com.upc.asistenteredidbi.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val rememberMe: Boolean = true,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                errorMessage = null
            )
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                errorMessage = null
            )
        }
    }

    fun onToggleRememberMe() {
        _uiState.update {
            it.copy(rememberMe = !it.rememberMe)
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update {
            it.copy(isPasswordVisible = !it.isPasswordVisible)
        }
    }

    fun login() {
        val state = _uiState.value

        if (state.isLoading) return

        when {
            state.email.isBlank() -> {
                _uiState.update {
                    it.copy(errorMessage = "Ingresa tu correo")
                }
                return
            }

            state.password.isBlank() -> {
                _uiState.update {
                    it.copy(errorMessage = "Ingresa tu contraseña")
                }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    loginSuccess = false
                )
            }

            loginUseCase(
                email = state.email.trim(),
                password = state.password
            ).onSuccess { session ->

                sessionManager.saveSession(
                    accessToken = session.accessToken,
                    expiresInMinutes = session.expiresInMinutes
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loginSuccess = true
                    )
                }

            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loginSuccess = false,
                        errorMessage = error.message
                            ?: "Correo o contraseña incorrectos"
                    )
                }
            }
        }
    }

    fun consumeLoginSuccess() {
        _uiState.update {
            it.copy(loginSuccess = false)
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }
}