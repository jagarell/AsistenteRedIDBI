package com.upc.asistenteredidbi.domain.repository

import android.net.Uri
import com.upc.asistenteredidbi.domain.model.EvidenceAreaItem
import com.upc.asistenteredidbi.domain.model.EvidenceChecklist
import com.upc.asistenteredidbi.domain.model.EvidenceEquipmentItem
import com.upc.asistenteredidbi.domain.model.EvidencePhoto
import com.upc.asistenteredidbi.domain.model.Minuta

/** Contrato del dominio para HU04 (Evidencias, dos fases) y HU05 (Minuta). */
interface EvidenceRepository {

    suspend fun getChecklist(evaluationId: String): Result<EvidenceChecklist>

    // --- Fase A: selección (libre, nada bloquea) ---
    suspend fun addCustomArea(evaluationId: String, name: String): Result<EvidenceAreaItem>
    suspend fun deleteArea(evaluationId: String, areaId: String): Result<Unit>
    suspend fun addCustomEquipment(evaluationId: String, equipmentType: String, label: String): Result<EvidenceEquipmentItem>
    suspend fun deleteEquipment(evaluationId: String, equipmentId: String): Result<Unit>

    /** Cierra la Fase A: de aquí en más, cada ítem necesita ≥1 foto. */
    suspend fun lockSelection(evaluationId: String): Result<EvidenceChecklist>

    // --- Fase B: captura (multi-foto + comentario por ítem) ---
    suspend fun uploadAreaPhoto(evaluationId: String, areaId: String, imageUri: Uri, comment: String?): Result<EvidencePhoto>
    suspend fun uploadEquipmentPhoto(evaluationId: String, equipmentId: String, imageUri: Uri, comment: String?): Result<EvidencePhoto>
    suspend fun deletePhoto(evaluationId: String, photoId: String): Result<Unit>
    suspend fun updateEquipmentNotes(evaluationId: String, equipmentId: String, technicianNotes: String?): Result<EvidenceEquipmentItem>

    /** HU05: vista consolidada de solo lectura (tabla de equipos + áreas + respuestas). */
    suspend fun getMinuta(evaluationId: String): Result<Minuta>
}
