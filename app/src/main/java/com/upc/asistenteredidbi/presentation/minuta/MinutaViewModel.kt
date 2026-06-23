package com.upc.asistenteredidbi.presentation.minuta

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.domain.model.Minuta
import com.upc.asistenteredidbi.domain.usecase.GetMinutaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MinutaTab { EQUIPOS, AREAS, DETALLE_EQUIPOS }

data class MinutaUiState(
    val isLoading: Boolean = false,
    val minuta: Minuta? = null,
    val selectedTab: MinutaTab = MinutaTab.EQUIPOS,
    val errorMessage: String? = null
)

/**
 * Pantalla "Vista de minuta" (HU03-HU04, sección 5 del spec):
 * recupera la vista consolidada de solo lectura (tabla de equipos +
 * áreas con fotos + equipos con fotos/comentarios) justo antes de que
 * el técnico confirme "Analizar con IA".
 */
@HiltViewModel
class MinutaViewModel @Inject constructor(
    private val getMinutaUseCase: GetMinutaUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val evaluationId: String = checkNotNull(savedStateHandle["evaluationId"])

    private val _uiState = MutableStateFlow(MinutaUiState())
    val uiState: StateFlow<MinutaUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getMinutaUseCase(evaluationId)
                .onSuccess { minuta -> _uiState.update { it.copy(isLoading = false, minuta = minuta) } }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "No se pudo cargar la minuta.") }
                }
        }
    }

    fun selectTab(tab: MinutaTab) = _uiState.update { it.copy(selectedTab = tab) }
}
