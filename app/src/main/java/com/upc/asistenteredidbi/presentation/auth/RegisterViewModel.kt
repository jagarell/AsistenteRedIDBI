package com.upc.asistenteredidbi.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val isLoading: Boolean = false,
    val isRegistered: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(
        fullName: String,
        email: String,
        phone: String,
        company: String,
        city: String,
        password: String,
        confirmPassword: String
    ) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isRegistered = false,
                    successMessage = null,
                    errorMessage = null
                )
            }

            registerUseCase(
                fullName = fullName,
                email = email,
                phone = phone,
                company = company,
                city = city,
                password = password,
                confirmPassword = confirmPassword
            ).onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRegistered = true,
                        successMessage = response.message
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message
                            ?: "No se pudo registrar el usuario"
                    )
                }
            }
        }
    }

    fun consumeRegistration() {
        _uiState.update {
            it.copy(
                isRegistered = false,
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