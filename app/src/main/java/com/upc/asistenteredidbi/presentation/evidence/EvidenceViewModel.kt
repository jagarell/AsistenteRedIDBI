package com.upc.asistenteredidbi.presentation.evidence

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.domain.model.EvidenceChecklist
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
    // Diálogo "Crear más evidencias" (Fase A)
    val showAddDialog: Boolean = false,
    val addDialogIsArea: Boolean = true,
    val addDialogText: String = "",
    val addDialogEquipmentType: String = "router",
    // Detalle de captura (Fase B): qué ítem está abierto + borrador de comentario
    val selectedAreaId: String? = null,
    val selectedEquipmentId: String? = null,
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val evaluationId: String = checkNotNull(savedStateHandle["evaluationId"])

    private val _uiState = MutableStateFlow(EvidenceUiState())
    val uiState: StateFlow<EvidenceUiState> = _uiState.asStateFlow()

    fun loadChecklist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getChecklistUseCase(evaluationId)
                .onSuccess { checklist -> _uiState.update { it.copy(isLoading = false, checklist = checklist) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    // --- Fase A: selección ---

    fun openAddDialog(isArea: Boolean) = _uiState.update {
        it.copy(showAddDialog = true, addDialogIsArea = isArea, addDialogText = "")
    }

    fun dismissAddDialog() = _uiState.update { it.copy(showAddDialog = false) }

    fun onAddDialogTextChange(value: String) = _uiState.update { it.copy(addDialogText = value) }

    fun onAddDialogEquipmentTypeChange(type: String) = _uiState.update { it.copy(addDialogEquipmentType = type) }

    fun confirmAddDialog() {
        val state = _uiState.value
        if (state.addDialogText.isBlank()) return
        viewModelScope.launch {
            val result = if (state.addDialogIsArea) {
                addCustomAreaUseCase(evaluationId, state.addDialogText)
            } else {
                addCustomEquipmentUseCase(evaluationId, state.addDialogEquipmentType, state.addDialogText)
            }
            result.onSuccess {
                _uiState.update { it.copy(showAddDialog = false) }
                loadChecklist()
            }.onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun deleteArea(areaId: String) {
        viewModelScope.launch {
            deleteAreaUseCase(evaluationId, areaId)
                .onSuccess { loadChecklist() }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun deleteEquipment(equipmentId: String) {
        viewModelScope.launch {
            deleteEquipmentUseCase(evaluationId, equipmentId)
                .onSuccess { loadChecklist() }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    /** Botón "Cerrar selección": pasa de Fase A a Fase B. */
    fun lockSelection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            lockSelectionUseCase(evaluationId)
                .onSuccess { checklist -> _uiState.update { it.copy(isLoading = false, checklist = checklist) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    // --- Fase B: captura ---

    fun openAreaDetail(areaId: String) = _uiState.update { it.copy(selectedAreaId = areaId, selectedEquipmentId = null, draftComment = "") }
    fun openEquipmentDetail(equipmentId: String) = _uiState.update { it.copy(selectedEquipmentId = equipmentId, selectedAreaId = null, draftComment = "") }
    fun closeDetail() = _uiState.update { it.copy(selectedAreaId = null, selectedEquipmentId = null, draftComment = "") }
    fun onDraftCommentChange(value: String) = _uiState.update { it.copy(draftComment = value) }

    /** Sube una foto (puede haber varias) para el ítem actualmente abierto. */
    fun uploadPhotoForSelectedItem(uri: Uri) {
        val state = _uiState.value
        val areaId = state.selectedAreaId
        val equipmentId = state.selectedEquipmentId
        if (areaId == null && equipmentId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true, errorMessage = null) }
            val result = if (areaId != null) {
                uploadAreaPhotoUseCase(evaluationId, areaId, uri, state.draftComment.ifBlank { null })
            } else {
                uploadEquipmentPhotoUseCase(evaluationId, equipmentId!!, uri, state.draftComment.ifBlank { null })
            }
            result.onSuccess {
                _uiState.update { it.copy(isUploadingPhoto = false, draftComment = "") }
                loadChecklist()
            }.onFailure { error -> _uiState.update { it.copy(isUploadingPhoto = false, errorMessage = error.message) } }
        }
    }

    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            deletePhotoUseCase(evaluationId, photoId)
                .onSuccess { loadChecklist() }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }
}
