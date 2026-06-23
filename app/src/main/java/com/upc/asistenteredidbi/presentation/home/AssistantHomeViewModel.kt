package com.upc.asistenteredidbi.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.domain.model.EvaluationSummaryItem
import com.upc.asistenteredidbi.domain.model.ProfileStats
import com.upc.asistenteredidbi.domain.usecase.GetCurrentUserUseCase
import com.upc.asistenteredidbi.domain.usecase.GetProfileStatusUseCase
import com.upc.asistenteredidbi.domain.usecase.ListEvaluationsUseCase
import com.upc.asistenteredidbi.domain.usecase.StartEvaluationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssistantHomeUiState(
    val userFirstName: String = "",
    val userCompany: String = "",
    val isLoading: Boolean = false,
    val stats: ProfileStats? = null,
    val inProgressEvaluations: List<EvaluationSummaryItem> = emptyList(),
    val recentEvaluations: List<EvaluationSummaryItem> = emptyList(),
    val errorMessage: String? = null,
    val newEvaluationId: String? = null
)

@HiltViewModel
class AssistantHomeViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getProfileStatusUseCase: GetProfileStatusUseCase,
    private val listEvaluationsUseCase: ListEvaluationsUseCase,
    private val startEvaluationUseCase: StartEvaluationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantHomeUiState())
    val uiState: StateFlow<AssistantHomeUiState> = _uiState.asStateFlow()

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            getCurrentUserUseCase().onSuccess { user ->
                _uiState.update { it.copy(userFirstName = user.fullName.substringBefore(" "), userCompany = user.company.orEmpty()) }
            }

            getProfileStatusUseCase().onSuccess { stats -> _uiState.update { it.copy(stats = stats) } }

            listEvaluationsUseCase()
                .onSuccess { evaluations ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            inProgressEvaluations = evaluations.filter { e -> e.status.name != "GENERADA" },
                            recentEvaluations = evaluations.take(5)
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    /** Tarjeta "+ Nueva Evaluación": la dirección puede venir de GPS o de texto libre (HU02). */
    fun startNewEvaluation(
        establishmentName: String,
        establishmentAddress: String?,
        latitude: Double?,
        longitude: Double?,
        clientName: String?,
        clientPhone: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            startEvaluationUseCase(establishmentName, establishmentAddress, latitude, longitude, clientName, clientPhone)
                .onSuccess { evaluation -> _uiState.update { it.copy(isLoading = false, newEvaluationId = evaluation.id) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    fun consumeNewEvaluationEvent() {
        _uiState.update { it.copy(newEvaluationId = null) }
    }
}
