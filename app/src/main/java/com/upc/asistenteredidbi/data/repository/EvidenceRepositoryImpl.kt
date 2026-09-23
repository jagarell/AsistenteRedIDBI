package com.upc.asistenteredidbi.data.repository

import android.content.Context
import android.net.Uri
import com.upc.asistenteredidbi.data.mapper.toDomain
import com.upc.asistenteredidbi.data.remote.EvidenceApiService
import com.upc.asistenteredidbi.data.remote.dto.CreateCustomAreaRequestDto
import com.upc.asistenteredidbi.data.remote.dto.CreateCustomEquipmentRequestDto
import com.upc.asistenteredidbi.data.remote.dto.UpdateEquipmentNotesRequestDto
import com.upc.asistenteredidbi.data.remote.toFriendlyMessage
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

    override suspend fun getChecklist(evaluationId: Long): Result<EvidenceChecklist> = safeCall {
        api.getChecklist(evaluationId).toDomain()
    }

    override suspend fun addCustomArea(evaluationId: Long, name: String): Result<EvidenceAreaItem> = safeCall {
        api.addCustomArea(evaluationId, CreateCustomAreaRequestDto(name)).toDomain()
    }

    override suspend fun deleteArea(evaluationId: Long, areaId: Long): Result<Unit> = safeCall {
        api.deleteArea(evaluationId, areaId)
    }

    override suspend fun addCustomEquipment(evaluationId: Long, equipmentType: String, label: String): Result<EvidenceEquipmentItem> = safeCall {
        api.addCustomEquipment(evaluationId, CreateCustomEquipmentRequestDto(equipmentType, label)).toDomain()
    }

    override suspend fun deleteEquipment(evaluationId: Long, equipmentId: Long): Result<Unit> = safeCall {
        api.deleteEquipment(evaluationId, equipmentId)
    }

    override suspend fun lockSelection(evaluationId: Long): Result<EvidenceChecklist> = safeCall {
        api.lockSelection(evaluationId).toDomain()
    }

    override suspend fun uploadAreaPhoto(evaluationId: Long, areaId: Long, imageUri: Uri, comment: String?): Result<EvidencePhoto> = safeCall {
        val tempFile = MultipartUtils.uriToTempFile(context, imageUri, prefix = "area_$areaId")
        val commentPart = comment?.takeIf { it.isNotBlank() }?.let { MultipartUtils.textPart(it) }
        val result = api.uploadAreaPhoto(evaluationId, areaId, commentPart, MultipartUtils.filePart(tempFile))
        tempFile.delete()
        result.toDomain()
    }

    override suspend fun uploadEquipmentPhoto(evaluationId: Long, equipmentId: Long, imageUri: Uri, comment: String?): Result<EvidencePhoto> = safeCall {
        val tempFile = MultipartUtils.uriToTempFile(context, imageUri, prefix = "equipment_$equipmentId")
        val commentPart = comment?.takeIf { it.isNotBlank() }?.let { MultipartUtils.textPart(it) }
        val result = api.uploadEquipmentPhoto(evaluationId, equipmentId, commentPart, MultipartUtils.filePart(tempFile))
        tempFile.delete()
        result.toDomain()
    }

    override suspend fun deletePhoto(evaluationId: Long, photoId: Long): Result<Unit> = safeCall {
        api.deletePhoto(evaluationId, photoId)
    }

    override suspend fun updateEquipmentNotes(evaluationId: Long, equipmentId: Long, technicianNotes: String?): Result<EvidenceEquipmentItem> = safeCall {
        api.updateEquipmentNotes(evaluationId, equipmentId, UpdateEquipmentNotesRequestDto(technicianNotes)).toDomain()
    }

    override suspend fun getMinuta(evaluationId: Long): Result<Minuta> = safeCall {
        api.getMinuta(evaluationId).toDomain()
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(Exception(e.toFriendlyMessage(), e))
        }
    }
}
