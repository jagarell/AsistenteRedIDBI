package com.upc.asistenteredidbi.presentation.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.data.session.ChatLocalFiles
import com.upc.asistenteredidbi.domain.model.NetworkMap
import com.upc.asistenteredidbi.domain.usecase.GenerateMapUseCase
import com.upc.asistenteredidbi.domain.usecase.GetMapUseCase
import com.upc.asistenteredidbi.domain.usecase.SaveMapUseCase
import com.upc.asistenteredidbi.presentation.chat.ChatEvidenceItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopologyEditorState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val map: NetworkMap = NetworkMap(),
    val canUndo: Boolean = false,
    val evidences: List<ChatEvidenceItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class TopologyEditorViewModel @Inject constructor(
    private val getMap: GetMapUseCase,
    private val saveMap: SaveMapUseCase,
    private val generateMap: GenerateMapUseCase,
    private val localFiles: ChatLocalFiles,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val evaluationId: Long = savedStateHandle.get<String>("evaluationId")?.toLongOrNull() ?: -1L

    private val _state = MutableStateFlow(TopologyEditorState())
    val state: StateFlow<TopologyEditorState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<NetworkMap>()

    init {
        viewModelScope.launch {
            val evidences = localFiles.loadEvidenceIndex(evaluationId)
            _state.update { it.copy(evidences = evidences) }
            getMap(evaluationId)
                .onSuccess { saved ->
                    if (saved != null) {
                        _state.update { it.copy(isLoading = false, map = saved) }
                    } else {
                        // Todavía no hay mapa: se arma el automático a partir del chat.
                        generateMap(evaluationId)
                            .onSuccess { generated -> _state.update { it.copy(isLoading = false, map = generated) } }
                            .onFailure { e -> _state.update { it.copy(isLoading = false, errorMessage = e.message) } }
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, errorMessage = e.message) } }
        }
    }

    /** Guarda el estado actual para "Deshacer" (antes de cada cambio). */
    fun pushUndo(snapshot: NetworkMap = _state.value.map) {
        undoStack.addLast(snapshot)
        if (undoStack.size > 50) undoStack.removeFirst()
        _state.update { it.copy(canUndo = true) }
    }

    fun setMap(map: NetworkMap) {
        _state.update { it.copy(map = map) }
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        _state.update { it.copy(map = previous, canUndo = undoStack.isNotEmpty()) }
    }

    fun save() {
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            saveMap(evaluationId, _state.value.map)
                .onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _state.update { it.copy(isSaving = false, errorMessage = e.message ?: "No se pudo guardar el mapa") } }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
