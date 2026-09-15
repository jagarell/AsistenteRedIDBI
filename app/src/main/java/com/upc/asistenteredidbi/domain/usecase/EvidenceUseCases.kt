package com.upc.asistenteredidbi.domain.usecase

import android.net.Uri
import com.upc.asistenteredidbi.domain.model.EvidenceAreaItem
import com.upc.asistenteredidbi.domain.model.EvidenceChecklist
import com.upc.asistenteredidbi.domain.model.EvidenceEquipmentItem
import com.upc.asistenteredidbi.domain.model.EvidencePhoto
import com.upc.asistenteredidbi.domain.model.Minuta
import com.upc.asistenteredidbi.domain.repository.EvidenceRepository
import javax.inject.Inject

class GetEvidenceChecklistUseCase @Inject constructor(private val repository: EvidenceRepository) {
    suspend operator fun invoke(evaluationId: Long): Result<EvidenceChecklist> = repository.getChecklist(evaluationId)
}

/** Fase A — agregar una zona/equipo fuera de lo sembrado desde el chat. */
class AddCustomAreaUseCase @Inject constructor(private val repository: EvidenceRepository) {
    suspend operator fun invoke(evaluationId: Long, name: String): Result<EvidenceAreaItem> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("El nombre del área no puede estar vacío"))
        return repository.addCustomArea(evaluationId, name.trim())
    }
}

class DeleteAreaUseCase @Inject constructor(private val repository: EvidenceRepository) {
    suspend operator fun invoke(evaluationId: Long, areaId: Long): Result<Unit> = repository.deleteArea(evaluationId, areaId)
}

class AddCustomEquipmentUseCase @Inject constructor(private val repository: EvidenceRepository) {
    suspend operator fun invoke(evaluationId: Long, equipmentType: String, label: String): Result<EvidenceEquipmentItem> {
        if (label.isBlank()) return Result.failure(IllegalArgumentException("El nombre del equipo no puede estar vacío"))
        return repository.addCustomEquipment(evaluationId, equipmentType, label.trim())
    }
}

class DeleteEquipmentUseCase @Inject constructor(private val repository: EvidenceRepository) {
    suspend operator fun invoke(evaluationId: Long, equipmentId: Long): Result<Unit> = repository.deleteEquipment(evaluationId, equipmentId)
}

/** Cierra la Fase A ("Confirmar selección"): de aquí en más cada ítem necesita ≥1 foto. */
class LockEvidenceSelectionUseCase @Inject constructor(private val repository: EvidenceRepository) {
    suspend operator fun invoke(evaluationId: Long): Result<EvidenceChecklist> = repository.lockSelection(evaluationId)
}

/** Fase B — captura (multi-foto + comentario por ítem). */
class UploadAreaPhotoUseCase @Inject constructor(private val repository: EvidenceRepository) {
    suspend operator fun invoke(evaluationId: Long, areaId: Long, imageUri: Uri, comment: String?): Result<EvidencePhoto> =
        repository.uploadAreaPhoto(evaluationId, areaId, imageUri, comment)
}

class UploadEquipmentPhotoUseCase @Inject constructor(private val repository: EvidenceRepository) {
    suspend operator fun invoke(evaluationId: Long, equipmentId: Long, imageUri: Uri, comment: String?): Result<EvidencePhoto> =
        repository.uploadEquipmentPhoto(evaluationId, equipmentId, imageUri, comment)
}

class DeleteEvidencePhotoUseCase @Inject constructor(private val repository: EvidenceRepository) {
    suspend operator fun invoke(evaluationId: Long, photoId: Long): Result<Unit> = repository.deletePhoto(evaluationId, photoId)
}

class UpdateEquipmentNotesUseCase @Inject constructor(private val repository: EvidenceRepository) {
    suspend operator fun invoke(evaluationId: Long, equipmentId: Long, notes: String?): Result<EvidenceEquipmentItem> =
        repository.updateEquipmentNotes(evaluationId, equipmentId, notes)
}

/** Vista consolidada de solo lectura (tabla de equipos + áreas + respuestas). */
class GetMinutaUseCase @Inject constructor(private val repository: EvidenceRepository) {
    suspend operator fun invoke(evaluationId: Long): Result<Minuta> = repository.getMinuta(evaluationId)
}
