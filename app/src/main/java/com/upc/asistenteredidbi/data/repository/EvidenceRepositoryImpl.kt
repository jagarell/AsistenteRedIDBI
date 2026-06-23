package com.upc.asistenteredidbi.data.repository

import android.content.Context
import android.net.Uri
import com.upc.asistenteredidbi.data.mapper.toDomain
import com.upc.asistenteredidbi.data.remote.EvidenceApiService
import com.upc.asistenteredidbi.data.remote.dto.CreateCustomAreaRequestDto
import com.upc.asistenteredidbi.data.remote.dto.CreateCustomEquipmentRequestDto
import com.upc.asistenteredidbi.data.remote.dto.LockSelectionRequestDto
import com.upc.asistenteredidbi.data.remote.dto.UpdateEquipmentNotesRequestDto
import com.upc.asistenteredidbi.data.util.MultipartUtils
import com.upc.asistenteredidbi.domain.model.EvidenceAreaItem
import com.upc.asistenteredidbi.domain.model.EvidenceChecklist
import com.upc.asistenteredidbi.domain.model.EvidenceEquipmentItem
import com.upc.asistenteredidbi.domain.model.EvidencePhoto
import com.upc.asistenteredidbi.domain.model.Minuta
import com.upc.asistenteredidbi.domain.repository.EvidenceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EvidenceRepositoryImpl @Inject constructor(
    private val api: EvidenceApiService,
    @ApplicationContext private val context: Context
) : EvidenceRepository {

    override suspend fun getChecklist(evaluationId: String): Result<EvidenceChecklist> = safeCall {
        api.getChecklist(evaluationId).toDomain()
    }

    override suspend fun addCustomArea(evaluationId: String, name: String): Result<EvidenceAreaItem> = safeCall {
        api.addCustomArea(evaluationId, CreateCustomAreaRequestDto(name)).toDomain()
    }

    override suspend fun deleteArea(evaluationId: String, areaId: String): Result<Unit> = safeCall {
        api.deleteArea(evaluationId, areaId)
    }

    override suspend fun addCustomEquipment(evaluationId: String, equipmentType: String, label: String): Result<EvidenceEquipmentItem> = safeCall {
        api.addCustomEquipment(evaluationId, CreateCustomEquipmentRequestDto(equipmentType, label)).toDomain()
    }

    override suspend fun deleteEquipment(evaluationId: String, equipmentId: String): Result<Unit> = safeCall {
        api.deleteEquipment(evaluationId, equipmentId)
    }

    override suspend fun lockSelection(evaluationId: String): Result<EvidenceChecklist> = safeCall {
        api.lockSelection(evaluationId, LockSelectionRequestDto()).toDomain()
    }

    override suspend fun uploadAreaPhoto(evaluationId: String, areaId: String, imageUri: Uri, comment: String?): Result<EvidencePhoto> = safeCall {
        val tempFile = MultipartUtils.uriToTempFile(context, imageUri, prefix = "area_$areaId")
        val result = api.uploadAreaPhoto(evaluationId, areaId, MultipartUtils.textPart(comment ?: ""), MultipartUtils.filePart(tempFile))
        tempFile.delete()
        result.toDomain()
    }

    override suspend fun uploadEquipmentPhoto(evaluationId: String, equipmentId: String, imageUri: Uri, comment: String?): Result<EvidencePhoto> = safeCall {
        val tempFile = MultipartUtils.uriToTempFile(context, imageUri, prefix = "equipment_$equipmentId")
        val result = api.uploadEquipmentPhoto(evaluationId, equipmentId, MultipartUtils.textPart(comment ?: ""), MultipartUtils.filePart(tempFile))
        tempFile.delete()
        result.toDomain()
    }

    override suspend fun deletePhoto(evaluationId: String, photoId: String): Result<Unit> = safeCall {
        api.deletePhoto(evaluationId, photoId)
    }

    override suspend fun updateEquipmentNotes(evaluationId: String, equipmentId: String, technicianNotes: String?): Result<EvidenceEquipmentItem> = safeCall {
        api.updateEquipmentNotes(evaluationId, equipmentId,
            UpdateEquipmentNotesRequestDto(technicianNotes, null)
        ).toDomain()
    }

    override suspend fun getMinuta(evaluationId: String): Result<Minuta> = safeCall {
        api.getMinuta(evaluationId).toDomain()
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
