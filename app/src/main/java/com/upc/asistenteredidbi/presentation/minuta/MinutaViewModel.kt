package com.upc.asistenteredidbi.presentation.minuta

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.data.remote.dto.AnalysisResponseDto
import com.upc.asistenteredidbi.domain.model.Minuta
import com.upc.asistenteredidbi.domain.repository.EvaluationRepository
import com.upc.asistenteredidbi.domain.usecase.GetMinutaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MinutaTab {
    EQUIPOS,
    AREAS,
    DETALLE_EQUIPOS
}

data class MinutaUiState(
    val isLoading: Boolean = false,
    val minuta: Minuta? = null,
    val selectedTab: MinutaTab = MinutaTab.EQUIPOS,
    val errorMessage: String? = null,

    val isAnalyzing: Boolean = false,
    val analysis: AnalysisResponseDto? = null,
    val analysisErrorMessage: String? = null
)

@HiltViewModel
class MinutaViewModel @Inject constructor(
    private val getMinutaUseCase: GetMinutaUseCase,
    private val evaluationRepository: EvaluationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val evaluationId: String =
        savedStateHandle.get<Any>("evaluationId")?.toString() ?: "1"

    private val _uiState = MutableStateFlow(MinutaUiState())
    val uiState: StateFlow<MinutaUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            getMinutaUseCase(evaluationId)
                .onSuccess { minuta ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            minuta = minuta
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudo cargar la minuta."
                        )
                    }
                }
        }
    }

    fun loadAnalysis() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAnalyzing = true,
                    analysisErrorMessage = null
                )
            }

            evaluationRepository
                .analyzeEvaluation(evaluationId.toLongOrNull() ?: 1L)
                .onSuccess { analysis ->
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            analysis = analysis
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            analysisErrorMessage = error.message ?: "No se pudo obtener el análisis IA."
                        )
                    }
                }
        }
    }

    fun selectTab(tab: MinutaTab) {
        _uiState.update {
            it.copy(selectedTab = tab)
        }
    }

    fun clearAnalysisError() {
        _uiState.update {
            it.copy(analysisErrorMessage = null)
        }
    }

    fun clearGeneralError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }
}