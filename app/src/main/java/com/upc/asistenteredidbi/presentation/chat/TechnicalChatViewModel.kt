package com.upc.asistenteredidbi.presentation.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.domain.model.ChatClosingSummary
import com.upc.asistenteredidbi.domain.model.ChatInputType
import com.upc.asistenteredidbi.domain.model.ChatNode
import com.upc.asistenteredidbi.domain.model.ROOT_CONNECTION_LABEL
import com.upc.asistenteredidbi.domain.usecase.ConfirmClosingSummaryUseCase
import com.upc.asistenteredidbi.domain.usecase.GetChatProgressUseCase
import com.upc.asistenteredidbi.domain.usecase.GetClosingSummaryUseCase
import com.upc.asistenteredidbi.domain.usecase.SaveChatPhotoResponseUseCase
import com.upc.asistenteredidbi.domain.usecase.SaveChatResponseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import kotlin.collections.plus

/** Un mensaje en la conversación (asistente o usuario), para renderizar las burbujas de chat. */
data class ChatMessage(val text: String, val isFromUser: Boolean)

data class TechnicalChatUiState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val currentNode: ChatNode? = null,
    val draftAnswer: String = "",
    val draftCounts: Map<String, String> = emptyMap(),
    val draftSelectedOptions: Set<String> = emptySet(),
    val draftCustomOption: String = "",
    val draftConnections: Map<String, String> = emptyMap(),
    val isComplete: Boolean = false,
    val closingSummary: ChatClosingSummary? = null,
    val closingNotes: String = "",
    val isClosingConfirmed: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TechnicalChatViewModel @Inject constructor(
    private val getChatProgressUseCase: GetChatProgressUseCase,
    private val saveChatResponseUseCase: SaveChatResponseUseCase,
    private val saveChatPhotoResponseUseCase: SaveChatPhotoResponseUseCase,
    private val getClosingSummaryUseCase: GetClosingSummaryUseCase,
    private val confirmClosingSummaryUseCase: ConfirmClosingSummaryUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val evaluationId: String = checkNotNull(savedStateHandle["evaluationId"])

    private val _uiState = MutableStateFlow(TechnicalChatUiState())
    val uiState: StateFlow<TechnicalChatUiState> = _uiState.asStateFlow()

    /** Carga el progreso actual al entrar (soporta reanudar una evaluación en pausa). */
    fun loadProgress() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getChatProgressUseCase(evaluationId)
                .onSuccess { progress -> applyProgress(progress.nextNode, progress.isComplete, appendPrompt = true) }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    private fun applyProgress(nextNode: ChatNode?, isComplete: Boolean, appendPrompt: Boolean) {
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                currentNode = nextNode,
                isComplete = isComplete,
                draftAnswer = "", draftCounts = emptyMap(), draftSelectedOptions = emptySet(), draftCustomOption = "",
                // Cada ítem arranca conectado al router principal por defecto;
                // el técnico solo cambia los que de verdad cuelgan de otro equipo.
                draftConnections = nextNode?.connectionMapItems.orEmpty().associateWith { ROOT_CONNECTION_LABEL },
                messages = if (nextNode != null && appendPrompt) state.messages + ChatMessage(nextNode.promptText, isFromUser = false)
                else state.messages
            )
        }
        if (isComplete) loadClosingSummary()
    }

    fun onDraftAnswerChange(value: String) = _uiState.update { it.copy(draftAnswer = value) }

    /** Texto libre, número, opción de 'choice', o Sí/No — todos convergen aquí. */
    fun submitAnswer(rawValue: String, displayText: String = rawValue) {
        val node = _uiState.value.currentNode ?: return
        if (rawValue.isBlank()) return
        submitToNode(node.nodeKey, rawValue, displayText)
    }

    // --- multi_count: un campo numérico por cada entrada de countFields ---
    fun onCountFieldChange(field: String, value: String) {
        _uiState.update { it.copy(draftCounts = it.draftCounts + (field to value)) }
    }

    fun submitMultiCount() {
        val node = _uiState.value.currentNode ?: return
        val counts = node.countFields.orEmpty().associateWith { field ->
            (_uiState.value.draftCounts[field]?.toIntOrNull() ?: 0)
        }
        val json = JSONObject(counts as Map<*, *>).toString()
        val display = counts.entries.filter { it.value > 0 }.joinToString(", ") { "${it.value} ${it.key}" }
            .ifBlank { "Ninguno" }
        submitToNode(node.nodeKey, json, display)
    }

    // --- multi_select: chips predefinidos + opción de agregar otra ---
    fun toggleSelectOption(option: String) {
        _uiState.update { state ->
            val current = state.draftSelectedOptions
            state.copy(draftSelectedOptions = if (option in current) current - option else current + option)
        }
    }

    fun onDraftCustomOptionChange(value: String) = _uiState.update { it.copy(draftCustomOption = value) }

    fun addCustomSelectOption() {
        val custom = _uiState.value.draftCustomOption.trim()
        if (custom.isBlank()) return
        _uiState.update { it.copy(draftSelectedOptions = it.draftSelectedOptions + custom, draftCustomOption = "") }
    }

    fun submitMultiSelect() {
        val node = _uiState.value.currentNode ?: return
        val selected = _uiState.value.draftSelectedOptions.toList()
        val json = JSONArray(selected).toString()
        val display = if (selected.isEmpty()) "Ninguna" else selected.joinToString(", ")
        submitToNode(node.nodeKey, json, display)
    }

    // --- connection_map: a qué está conectado cada equipo (router / otro equipo) ---
    fun onConnectionItemParentChange(item: String, parent: String) {
        _uiState.update { it.copy(draftConnections = it.draftConnections + (item to parent)) }
    }

    fun submitConnectionMap() {
        val node = _uiState.value.currentNode ?: return
        val connections = _uiState.value.draftConnections
        val json = JSONObject(connections as Map<*, *>).toString()
        val grouped = connections.entries.groupBy({ it.value }, { it.key })
        val display = grouped.entries.joinToString("; ") { (parent, children) -> "$parent: ${children.joinToString(", ")}" }
            .ifBlank { "Todo conectado al router principal" }
        submitToNode(node.nodeKey, json, display)
    }

    // --- photo: CameraX/galería entrega un Uri ---
    fun submitPhoto(uri: Uri) {
        val node = _uiState.value.currentNode ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, messages = it.messages + ChatMessage("📷 Foto capturada", isFromUser = true))
            }
            saveChatPhotoResponseUseCase(evaluationId, node.nodeKey, uri)
                .onSuccess { progress -> applyProgress(progress.nextNode, progress.isComplete, appendPrompt = true) }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    private fun submitToNode(nodeKey: String, rawValue: String, displayText: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true, errorMessage = null,
                    messages = it.messages + ChatMessage(displayText, isFromUser = true),
                    draftAnswer = ""
                )
            }
            saveChatResponseUseCase(evaluationId, nodeKey, rawValue)
                .onSuccess { progress -> applyProgress(progress.nextNode, progress.isComplete, appendPrompt = true) }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    // --- Cierre: resumen generado por el asistente al completar el árbol (HU03 §4.5) ---
    private fun loadClosingSummary() {
        viewModelScope.launch {
            getClosingSummaryUseCase(evaluationId)
                .onSuccess { summary -> _uiState.update { it.copy(closingSummary = summary) } }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun onClosingNotesChange(value: String) = _uiState.update { it.copy(closingNotes = value) }

    /** El técnico confirma el resumen (con o sin nota adicional) -> habilita navegar a Evidencias. */
    fun confirmClosingSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            confirmClosingSummaryUseCase(evaluationId, _uiState.value.closingNotes.ifBlank { null })
                .onSuccess { _uiState.update { it.copy(isLoading = false, isClosingConfirmed = true) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }
}

/** Helper de UI: decide qué controles mostrar según el tipo de nodo. */
fun ChatInputType.requiresFreeText(): Boolean = this == ChatInputType.TEXT || this == ChatInputType.NUMBER
