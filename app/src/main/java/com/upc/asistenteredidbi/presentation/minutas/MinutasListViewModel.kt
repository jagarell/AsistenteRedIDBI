package com.upc.asistenteredidbi.presentation.minutas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.data.session.SessionManager
import com.upc.asistenteredidbi.domain.model.MinutaRecord
import com.upc.asistenteredidbi.domain.usecase.CompleteMinutaUseCase
import com.upc.asistenteredidbi.domain.usecase.ListMinutasUseCase
import com.upc.asistenteredidbi.domain.usecase.ValidateMinutaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MinutasListUiState(
    val isLoading: Boolean = false,
    val minutas: List<MinutaRecord> = emptyList(),
    val isSupervisor: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Lista todas las minutas (incluidos los borradores de cualquier técnico) y
 * expone las acciones "Completar" y "Validar". El gating de "Validar" se
 * aplica aquí con [isSupervisor] (persistido en [SessionManager] tras el
 * login); el backend además lo exige con `@PreAuthorize`.
 */
@HiltViewModel
class MinutasListViewModel @Inject constructor(
    private val listMinutasUseCase: ListMinutasUseCase,
    private val completeMinutaUseCase: CompleteMinutaUseCase,
    private val validateMinutaUseCase: ValidateMinutaUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MinutasListUiState())
    val uiState: StateFlow<MinutasListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.isSupervisorFlow.collect { isSupervisor ->
                _uiState.update { it.copy(isSupervisor = isSupervisor) }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            listMinutasUseCase()
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false, minutas = list) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudieron cargar las minutas"
                        )
                    }
                }
        }
    }

    fun complete(id: Long) {
        viewModelScope.launch {
            completeMinutaUseCase(id)
                .onSuccess { updated -> replaceInList(updated) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "No se pudo completar la minuta")
                    }
                }
        }
    }

    fun validate(id: Long) {
        viewModelScope.launch {
            validateMinutaUseCase(id)
                .onSuccess { updated -> replaceInList(updated) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "No se pudo validar la minuta")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun replaceInList(updated: MinutaRecord) {
        _uiState.update { state ->
            state.copy(minutas = state.minutas.map { if (it.id == updated.id) updated else it })
        }
    }
}
