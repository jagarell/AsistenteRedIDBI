package com.upc.asistenteredidbi.presentation.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.domain.model.EvaluationFilters
import com.upc.asistenteredidbi.domain.model.EvaluationStatus
import com.upc.asistenteredidbi.domain.model.EvaluationSummaryItem
import com.upc.asistenteredidbi.domain.usecase.ListEvaluationsUseCase
import com.upc.asistenteredidbi.domain.usecase.StartEvaluationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistorialTab(val label: String, val status: EvaluationStatus?) {
    TODOS("Todos", null),
    COMPLETADOS("Completados", EvaluationStatus.GENERADA),
    BORRADORES("Borradores", EvaluationStatus.BORRADOR),
    EN_ANALISIS("En análisis", EvaluationStatus.EN_PROGRESO)
}

data class HistorialUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedTab: HistorialTab = HistorialTab.TODOS,
    val allEvaluations: List<EvaluationSummaryItem> = emptyList(),
    val errorMessage: String? = null,
    val isStartingEvaluation: Boolean = false,
    val newEvaluationId: Long? = null
) {
    val filteredEvaluations: List<EvaluationSummaryItem>
        get() = allEvaluations
            .filter { selectedTab.status == null || it.status == selectedTab.status }
            .filter { searchQuery.isBlank() || it.establishmentName.contains(searchQuery, ignoreCase = true) || it.clientName?.contains(searchQuery, ignoreCase = true) == true }
}

@HiltViewModel
class HistorialViewModel @Inject constructor(
    private val listEvaluationsUseCase: ListEvaluationsUseCase,
    private val startEvaluationUseCase: StartEvaluationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState: StateFlow<HistorialUiState> = _uiState.asStateFlow()

    fun loadHistorial() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            listEvaluationsUseCase(EvaluationFilters())
                .onSuccess { evaluations -> _uiState.update { it.copy(isLoading = false, allEvaluations = evaluations) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    fun onSearchQueryChange(value: String) = _uiState.update { it.copy(searchQuery = value) }
    fun onTabSelected(tab: HistorialTab) = _uiState.update { it.copy(selectedTab = tab) }

    fun startNewEvaluation() {
        if (_uiState.value.isStartingEvaluation) return

        viewModelScope.launch {
            _uiState.update { it.copy(isStartingEvaluation = true, errorMessage = null) }
            startEvaluationUseCase()
                .onSuccess { evaluation ->
                    _uiState.update {
                        it.copy(isStartingEvaluation = false, newEvaluationId = evaluation.id.toLongOrNull())
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isStartingEvaluation = false, errorMessage = error.message ?: "No se pudo iniciar la evaluación")
                    }
                }
        }
    }

    fun consumeNewEvaluationId() = _uiState.update { it.copy(newEvaluationId = null) }
}
