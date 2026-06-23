package com.upc.asistenteredidbi.data.mapper

import com.upc.asistenteredidbi.data.remote.dto.EquipmentTableRowDto
import com.upc.asistenteredidbi.data.remote.dto.EvidenceAreaItemDto
import com.upc.asistenteredidbi.data.remote.dto.EvidenceChecklistDto
import com.upc.asistenteredidbi.data.remote.dto.EvidenceEquipmentItemDto
import com.upc.asistenteredidbi.data.remote.dto.EvidencePhotoDto
import com.upc.asistenteredidbi.data.remote.dto.MinutaDto
import com.upc.asistenteredidbi.domain.model.EquipmentTableRow
import com.upc.asistenteredidbi.domain.model.EvidenceAreaItem
import com.upc.asistenteredidbi.domain.model.EvidenceChecklist
import com.upc.asistenteredidbi.domain.model.EvidenceEquipmentItem
import com.upc.asistenteredidbi.domain.model.EvidencePhoto
import com.upc.asistenteredidbi.domain.model.Minuta

fun EvidencePhotoDto.toDomain(): EvidencePhoto = EvidencePhoto(
    id = id, fileUrl = fileUrl, comment = comment, capturedAt = capturedAt
)

fun EvidenceAreaItemDto.toDomain(): EvidenceAreaItem = EvidenceAreaItem(
    id = id, name = name, isCustom = isCustom, photos = photos.map { it.toDomain() }
)

fun EvidenceEquipmentItemDto.toDomain(): EvidenceEquipmentItem = EvidenceEquipmentItem(
    id = id, equipmentType = equipmentType, label = label, isCustom = isCustom,
    extractedSpecs = extractedSpecs, technicianNotes = technicianNotes,
    photos = photos.map { it.toDomain() }
)

fun EvidenceChecklistDto.toDomain(): EvidenceChecklist = EvidenceChecklist(
    evaluationId = evaluationId,
    selectionLocked = selectionLocked,
    areas = areas.map { it.toDomain() },
    equipment = equipment.map { it.toDomain() },
    allItemsHavePhoto = allItemsHavePhoto
)

fun EquipmentTableRowDto.toDomain(): EquipmentTableRow = EquipmentTableRow(
    label = label, equipmentType = equipmentType, extractedSpecs = extractedSpecs,
    technicianNotes = technicianNotes, photoCount = photoCount
)

fun MinutaDto.toDomain(): Minuta = Minuta(
    evaluationId = evaluationId,
    establishmentName = establishmentName,
    establishmentAddress = establishmentAddress,
    conversationResponses = conversationResponses.map { it.toDomain() },
    areas = areas.map { it.toDomain() },
    equipment = equipment.map { it.toDomain() },
    equipmentTable = equipmentTable.map { it.toDomain() }
)
