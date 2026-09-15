package com.upc.asistenteredidbi.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.domain.model.MinutaRecord
import com.upc.asistenteredidbi.domain.model.ProfileStats
import com.upc.asistenteredidbi.domain.usecase.GetProfileStatusUseCase
import com.upc.asistenteredidbi.domain.usecase.GetProfileUseCase
import com.upc.asistenteredidbi.domain.usecase.ListMinutasUseCase
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
    val recentMinutas: List<MinutaRecord> = emptyList(),
    val errorMessage: String? = null,

    val isStartingEvaluation: Boolean = false,
    val newEvaluationId: Long? = null
)

@HiltViewModel
class AssistantHomeViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val getProfileStatusUseCase: GetProfileStatusUseCase,
    private val listMinutasUseCase: ListMinutasUseCase,
    private val startEvaluationUseCase: StartEvaluationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantHomeUiState())
    val uiState: StateFlow<AssistantHomeUiState> = _uiState.asStateFlow()

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            getProfileUseCase().onSuccess { user ->
                _uiState.update {
                    it.copy(
                        userFirstName = user.fullName.substringBefore(" "),
                        userCompany = user.company.orEmpty()
                    )
                }
            }

            getProfileStatusUseCase().onSuccess { stats ->
                _uiState.update { it.copy(stats = stats) }
            }

            listMinutasUseCase()
                .onSuccess { minutas ->
                    _uiState.update {
                        it.copy(isLoading = false, recentMinutas = minutas.take(5))
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
        }
    }

    /** "Nueva Evaluación": crea la evaluación real en el gateway antes de entrar al chat,
     *  para que no todas las evaluaciones nuevas terminen compartiendo el mismo ID. */
    fun startNewEvaluation() {
        if (_uiState.value.isStartingEvaluation) return

        viewModelScope.launch {
            _uiState.update { it.copy(isStartingEvaluation = true, errorMessage = null) }

            startEvaluationUseCase()
                .onSuccess { evaluation ->
                    _uiState.update {
                        it.copy(
                            isStartingEvaluation = false,
                            newEvaluationId = evaluation.id.toLongOrNull()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isStartingEvaluation = false,
                            errorMessage = error.message ?: "No se pudo iniciar la evaluación"
                        )
                    }
                }
        }
    }

    fun consumeNewEvaluationId() {
        _uiState.update { it.copy(newEvaluationId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
