package com.upc.asistenteredidbi.presentation.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.domain.model.User
import com.upc.asistenteredidbi.domain.usecase.GetProfileUseCase
import com.upc.asistenteredidbi.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class ConfiguracionUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val twoFactorEnabled: Boolean = false,
    val notifyNewEvaluations: Boolean = true,
    val notifyProposalUpdates: Boolean = true,
    val notifyWeeklySummary: Boolean = false,
    val didLogout: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ConfiguracionViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfiguracionUiState())
    val uiState: StateFlow<ConfiguracionUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getProfileUseCase()
                .onSuccess { user -> _uiState.update { it.copy(isLoading = false, user = user) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    fun onToggleTwoFactor(enabled: Boolean) = _uiState.update { it.copy(twoFactorEnabled = enabled) }
    fun onToggleNewEvaluations(enabled: Boolean) = _uiState.update { it.copy(notifyNewEvaluations = enabled) }
    fun onToggleProposalUpdates(enabled: Boolean) = _uiState.update { it.copy(notifyProposalUpdates = enabled) }
    fun onToggleWeeklySummary(enabled: Boolean) = _uiState.update { it.copy(notifyWeeklySummary = enabled) }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.update { it.copy(didLogout = true) }
        }
    }
}
