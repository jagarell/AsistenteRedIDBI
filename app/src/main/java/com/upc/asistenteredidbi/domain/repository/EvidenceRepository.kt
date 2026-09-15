package com.upc.asistenteredidbi.domain.repository

import android.net.Uri
import com.upc.asistenteredidbi.domain.model.EvidenceAreaItem
import com.upc.asistenteredidbi.domain.model.EvidenceChecklist
import com.upc.asistenteredidbi.domain.model.EvidenceEquipmentItem
import com.upc.asistenteredidbi.domain.model.EvidencePhoto
import com.upc.asistenteredidbi.domain.model.Minuta

/** Contrato del dominio del checklist dinámico de evidencias (Fase A
 * selección / Fase B captura) y la minuta consolidada de solo lectura. */
interface EvidenceRepository {

    suspend fun getChecklist(evaluationId: Long): Result<EvidenceChecklist>

    // --- Fase A: selección (libre, nada bloquea) ---
    suspend fun addCustomArea(evaluationId: Long, name: String): Result<EvidenceAreaItem>
    suspend fun deleteArea(evaluationId: Long, areaId: Long): Result<Unit>
    suspend fun addCustomEquipment(evaluationId: Long, equipmentType: String, label: String): Result<EvidenceEquipmentItem>
    suspend fun deleteEquipment(evaluationId: Long, equipmentId: Long): Result<Unit>

    /** Cierra la Fase A: de aquí en más, cada ítem necesita ≥1 foto. */
    suspend fun lockSelection(evaluationId: Long): Result<EvidenceChecklist>

    // --- Fase B: captura (multi-foto + comentario por ítem) ---
    suspend fun uploadAreaPhoto(evaluationId: Long, areaId: Long, imageUri: Uri, comment: String?): Result<EvidencePhoto>
    suspend fun uploadEquipmentPhoto(evaluationId: Long, equipmentId: Long, imageUri: Uri, comment: String?): Result<EvidencePhoto>
    suspend fun deletePhoto(evaluationId: Long, photoId: Long): Result<Unit>
    suspend fun updateEquipmentNotes(evaluationId: Long, equipmentId: Long, technicianNotes: String?): Result<EvidenceEquipmentItem>

    /** Vista consolidada de solo lectura (tabla de equipos + áreas + respuestas). */
    suspend fun getMinuta(evaluationId: Long): Result<Minuta>
}
