package com.upc.asistenteredidbi.presentation.evidence

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.data.remote.dto.AnalysisResponseDto
import com.upc.asistenteredidbi.domain.model.EvidenceChecklist
import com.upc.asistenteredidbi.domain.repository.EvaluationRepository
import com.upc.asistenteredidbi.domain.usecase.AddCustomAreaUseCase
import com.upc.asistenteredidbi.domain.usecase.AddCustomEquipmentUseCase
import com.upc.asistenteredidbi.domain.usecase.DeleteAreaUseCase
import com.upc.asistenteredidbi.domain.usecase.DeleteEquipmentUseCase
import com.upc.asistenteredidbi.domain.usecase.DeleteEvidencePhotoUseCase
import com.upc.asistenteredidbi.domain.usecase.GetEvidenceChecklistUseCase
import com.upc.asistenteredidbi.domain.usecase.LockEvidenceSelectionUseCase
import com.upc.asistenteredidbi.domain.usecase.UploadAreaPhotoUseCase
import com.upc.asistenteredidbi.domain.usecase.UploadEquipmentPhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EvidenceUiState(
    val isLoading: Boolean = false,
    val checklist: EvidenceChecklist? = null,
    val errorMessage: String? = null,

    val isAnalyzing: Boolean = false,
    val analysis: AnalysisResponseDto? = null,

    val showAddDialog: Boolean = false,
    val addDialogIsArea: Boolean = true,
    val addDialogText: String = "",
    val addDialogEquipmentType: String = "router",

    val selectedAreaId: Long? = null,
    val selectedEquipmentId: Long? = null,
    val draftComment: String = "",
    val isUploadingPhoto: Boolean = false
) {
    val selectionLocked: Boolean get() = checklist?.selectionLocked == true
    val canAnalyze: Boolean get() = selectionLocked && checklist?.allItemsHavePhoto == true
    val hasItemOpen: Boolean get() = selectedAreaId != null || selectedEquipmentId != null
}

@HiltViewModel
class EvidenceViewModel @Inject constructor(
    private val getChecklistUseCase: GetEvidenceChecklistUseCase,
    private val addCustomAreaUseCase: AddCustomAreaUseCase,
    private val deleteAreaUseCase: DeleteAreaUseCase,
    private val addCustomEquipmentUseCase: AddCustomEquipmentUseCase,
    private val deleteEquipmentUseCase: DeleteEquipmentUseCase,
    private val lockSelectionUseCase: LockEvidenceSelectionUseCase,
    private val uploadAreaPhotoUseCase: UploadAreaPhotoUseCase,
    private val uploadEquipmentPhotoUseCase: UploadEquipmentPhotoUseCase,
    private val deletePhotoUseCase: DeleteEvidencePhotoUseCase,
    private val evaluationRepository: EvaluationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val evaluationId: String = savedStateHandle["evaluationId"] ?: ""

    /** Null si el evaluationId de navegación no es un ID real parseable — en vez de
     *  asumir la evaluación 1, las funciones que lo requieren fallan explícito. */
    private val evaluationIdLong: Long? get() = evaluationId.toLongOrNull()

    private val _uiState = MutableStateFlow(EvidenceUiState())
    val uiState: StateFlow<EvidenceUiState> = _uiState.asStateFlow()

    private fun reportMissingEvaluationId() {
        _uiState.update { it.copy(errorMessage = "No se pudo identificar la evaluación.") }
    }

    /** Trae el checklist — la primera vez, el backend lo siembra solo a
     * partir de las respuestas reales del chat (ya persistidas al completar
     * la evaluación), sin que la app tenga que reenviarlas. */
    fun loadChecklist() {
        val id = evaluationIdLong ?: return reportMissingEvaluationId()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            getChecklistUseCase(id)
                .onSuccess { checklist ->
                    _uiState.update {
                        it.copy(isLoading = false, checklist = checklist)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
        }
    }

    fun analyzeEvaluation() {
        val id = evaluationIdLong ?: return reportMissingEvaluationId()

        viewModelScope.launch {
            _uiState.update {
                it.copy(isAnalyzing = true, errorMessage = null, analysis = null)
            }

            evaluationRepository
                .analyzeEvaluation(id)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(isAnalyzing = false, analysis = response)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            errorMessage = error.message ?: "Error al analizar con IA"
                        )
                    }
                }
        }
    }

    fun clearResult() {
        _uiState.update {
            it.copy(analysis = null, errorMessage = null, isAnalyzing = false)
        }
    }

    fun openAddDialog(isArea: Boolean) = _uiState.update {
        it.copy(showAddDialog = true, addDialogIsArea = isArea, addDialogText = "")
    }

    fun dismissAddDialog() = _uiState.update {
        it.copy(showAddDialog = false)
    }

    fun onAddDialogTextChange(value: String) = _uiState.update {
        it.copy(addDialogText = value)
    }

    fun onAddDialogEquipmentTypeChange(type: String) = _uiState.update {
        it.copy(addDialogEquipmentType = type)
    }

    fun confirmAddDialog() {
        val id = evaluationIdLong ?: return reportMissingEvaluationId()
        val state = _uiState.value
        if (state.addDialogText.isBlank()) return

        viewModelScope.launch {
            val result = if (state.addDialogIsArea) {
                addCustomAreaUseCase(id, state.addDialogText)
            } else {
                addCustomEquipmentUseCase(id, state.addDialogEquipmentType, state.addDialogText)
            }

            result
                .onSuccess {
                    _uiState.update { it.copy(showAddDialog = false) }
                    loadChecklist()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
        }
    }

    fun deleteArea(areaId: Long) {
        val id = evaluationIdLong ?: return reportMissingEvaluationId()
        viewModelScope.launch {
            deleteAreaUseCase(id, areaId)
                .onSuccess { loadChecklist() }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun deleteEquipment(equipmentId: Long) {
        val id = evaluationIdLong ?: return reportMissingEvaluationId()
        viewModelScope.launch {
            deleteEquipmentUseCase(id, equipmentId)
                .onSuccess { loadChecklist() }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun lockSelection() {
        val id = evaluationIdLong ?: return reportMissingEvaluationId()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            lockSelectionUseCase(id)
                .onSuccess { checklist ->
                    _uiState.update { it.copy(isLoading = false, checklist = checklist) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
        }
    }

    fun openAreaDetail(areaId: Long) = _uiState.update {
        it.copy(selectedAreaId = areaId, selectedEquipmentId = null, draftComment = "")
    }

    fun openEquipmentDetail(equipmentId: Long) = _uiState.update {
        it.copy(selectedEquipmentId = equipmentId, selectedAreaId = null, draftComment = "")
    }

    fun closeDetail() = _uiState.update {
        it.copy(selectedAreaId = null, selectedEquipmentId = null, draftComment = "")
    }

    fun onDraftCommentChange(value: String) = _uiState.update {
        it.copy(draftComment = value)
    }

    fun uploadPhotoForSelectedItem(uri: Uri) {
        val id = evaluationIdLong ?: return reportMissingEvaluationId()
        val state = _uiState.value
        val areaId = state.selectedAreaId
        val equipmentId = state.selectedEquipmentId

        if (areaId == null && equipmentId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true, errorMessage = null) }

            val result = if (areaId != null) {
                uploadAreaPhotoUseCase(id, areaId, uri, state.draftComment.ifBlank { null })
            } else {
                uploadEquipmentPhotoUseCase(id, equipmentId!!, uri, state.draftComment.ifBlank { null })
            }

            result
                .onSuccess {
                    _uiState.update { it.copy(isUploadingPhoto = false, draftComment = "") }
                    loadChecklist()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isUploadingPhoto = false, errorMessage = error.message) }
                }
        }
    }

    fun deletePhoto(photoId: Long) {
        val id = evaluationIdLong ?: return reportMissingEvaluationId()
        viewModelScope.launch {
            deletePhotoUseCase(id, photoId)
                .onSuccess { loadChecklist() }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }
}
