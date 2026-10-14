package com.upc.asistenteredidbi.presentation.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.data.session.ChatLocalFiles
import com.upc.asistenteredidbi.data.session.ChatProgressStore
import com.upc.asistenteredidbi.domain.model.EvaluationFilters
import com.upc.asistenteredidbi.domain.model.EvaluationStage
import com.upc.asistenteredidbi.domain.model.EvaluationSummaryItem
import com.upc.asistenteredidbi.domain.usecase.AnnulEvaluationUseCase
import com.upc.asistenteredidbi.domain.usecase.ListEvaluationsUseCase
import com.upc.asistenteredidbi.domain.usecase.StartEvaluationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HistorialTab(val label: String, val matches: (EvaluationStage) -> Boolean) {
    TODOS("Todos", { true }),
    COMPLETADOS("Completados", { it == EvaluationStage.COMPLETA || it == EvaluationStage.VALIDADA }),
    BORRADORES("Borradores", { it == EvaluationStage.BORRADOR }),
    EN_ANALISIS("En Análisis", { it == EvaluationStage.EN_CURSO })
}

/** Filtros rápidos de fecha del prototipo; con rango propio se usan [HistorialUiState.dateFrom] y [HistorialUiState.dateTo]. */
enum class DateQuick(val label: String) { TODAS("Todas las fechas"), HOY("Hoy"), SIETE("Últimos 7 días"), MES("Este mes"), RANGO("Rango") }

data class HistorialUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedTab: HistorialTab = HistorialTab.TODOS,
    val allEvaluations: List<EvaluationSummaryItem> = emptyList(),
    val errorMessage: String? = null,
    val isStartingEvaluation: Boolean = false,
    val newEvaluationId: Long? = null,
    /** Filtro por fecha de creación (inclusive), "yyyy-MM-dd"; null = sin filtro. */
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val dateQuick: DateQuick = DateQuick.TODAS
) {
    /** Lo que entra por el filtro de fecha: base de los contadores y de la lista. */
    val inDateRange: List<EvaluationSummaryItem>
        get() = allEvaluations.sortedByDescending { it.createdAt }.filter { item ->
            val day = item.createdAt.take(10)
            (dateFrom == null || day >= dateFrom) && (dateTo == null || day <= dateTo)
        }

    val filteredEvaluations: List<EvaluationSummaryItem>
        get() = inDateRange
            .filter { selectedTab.matches(it.stage) }
            .filter { searchQuery.isBlank() || it.establishmentName.contains(searchQuery, ignoreCase = true) || it.clientName?.contains(searchQuery, ignoreCase = true) == true }
}

@HiltViewModel
class HistorialViewModel @Inject constructor(
    private val listEvaluationsUseCase: ListEvaluationsUseCase,
    private val startEvaluationUseCase: StartEvaluationUseCase,
    private val annulEvaluationUseCase: AnnulEvaluationUseCase,
    private val chatProgressStore: ChatProgressStore,
    private val chatLocalFiles: ChatLocalFiles
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

    fun setDateRange(from: String?, to: String?, quick: DateQuick = DateQuick.RANGO) =
        _uiState.update { it.copy(dateFrom = from, dateTo = to, dateQuick = if (from == null && to == null) DateQuick.TODAS else quick) }

    fun clearDateRange() = setDateRange(null, null)

    /** Filtros "Hoy", "Últimos 7 días" y "Este mes", calculados con el día de hoy del teléfono. */
    fun setQuickDate(quick: DateQuick) {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        val today = fmt.format(cal.time)
        when (quick) {
            DateQuick.TODAS, DateQuick.RANGO -> clearDateRange()
            DateQuick.HOY -> setDateRange(today, today, quick)
            DateQuick.SIETE -> { cal.add(java.util.Calendar.DAY_OF_YEAR, -6); setDateRange(fmt.format(cal.time), today, quick) }
            DateQuick.MES -> { cal.set(java.util.Calendar.DAY_OF_MONTH, 1); setDateRange(fmt.format(cal.time), today, quick) }
        }
    }

    /** Anula una evaluación en borrador; si era la del chat guardado, también se descarta ese chat. */
    fun annul(evaluationId: String) {
        viewModelScope.launch {
            annulEvaluationUseCase(evaluationId)
                .onSuccess {
                    evaluationId.toLongOrNull()?.let { id ->
                        if (chatProgressStore.resumableEvaluationId() == id) {
                            chatProgressStore.clear(id)
                            chatLocalFiles.clear(id)
                        }
                    }
                    _uiState.update { it.copy(allEvaluations = it.allEvaluations.filterNot { e -> e.id == evaluationId }) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "No se pudo anular la evaluación") }
                    loadHistorial()
                }
        }
    }

    /** El FAB "Nueva evaluación": si hay un chat previo guardado se pide confirmar antes de borrarlo. */
    fun requestNewEvaluation(confirmDiscard: () -> Unit) {
        viewModelScope.launch {
            if (chatProgressStore.resumableEvaluationId() != null) confirmDiscard() else startNewEvaluation()
        }
    }

    fun discardPreviousAndStartNew() {
        viewModelScope.launch {
            chatProgressStore.resumableEvaluationId()?.let { previous ->
                chatProgressStore.clear(previous)
                chatLocalFiles.clear(previous)
            }
            startNewEvaluation()
        }
    }

    private fun startNewEvaluation() {
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
