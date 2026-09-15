package com.upc.asistenteredidbi.data.remote.dto

import com.squareup.moshi.JsonClass

/**
 * DTOs del checklist dinámico de evidencias — reflejan
 * `EvidenceChecklistController`/`EvidenceChecklistService` en
 * idbi-api-gateway (`com.upc.idbi.gateway.evidence.checklist`). Sin
 * anotaciones @Json: Moshi mapea camelCase directo, igual que el resto de
 * DTOs de la app (ver TechnicalChatDto.kt).
 */
@JsonClass(generateAdapter = true)
data class EvidencePhotoDto(
    val id: Long,
    val fileUrl: String,
    val comment: String?,
    val capturedAt: String
)

@JsonClass(generateAdapter = true)
data class EvidenceAreaItemDto(
    val id: Long,
    val name: String,
    val isCustom: Boolean,
    val photos: List<EvidencePhotoDto>
)

@JsonClass(generateAdapter = true)
data class EvidenceEquipmentItemDto(
    val id: Long,
    val equipmentType: String,
    val label: String,
    val isCustom: Boolean,
    val extractedSpecs: Map<String, Any?>?,
    val technicianNotes: String?,
    val photos: List<EvidencePhotoDto>
)

@JsonClass(generateAdapter = true)
data class EvidenceChecklistDto(
    val evaluationId: Long,
    val selectionLocked: Boolean,
    val areas: List<EvidenceAreaItemDto>,
    val equipment: List<EvidenceEquipmentItemDto>,
    val allItemsHavePhoto: Boolean
)

@JsonClass(generateAdapter = true)
data class CreateCustomAreaRequestDto(val name: String)

@JsonClass(generateAdapter = true)
data class CreateCustomEquipmentRequestDto(
    val equipmentType: String,
    val label: String
)

@JsonClass(generateAdapter = true)
data class UpdateEquipmentNotesRequestDto(
    val technicianNotes: String?
)

@JsonClass(generateAdapter = true)
data class ChatAnswerDto(
    val nodeKey: String,
    val question: String,
    val answer: String
)

@JsonClass(generateAdapter = true)
data class MinutaDto(
    val evaluationId: Long,
    val establishmentName: String,
    val establishmentAddress: String?,
    val conversationResponses: List<ChatAnswerDto>,
    val areas: List<EvidenceAreaItemDto>,
    val equipment: List<EvidenceEquipmentItemDto>,
    val equipmentTable: List<EquipmentTableRowDto>
)

@JsonClass(generateAdapter = true)
data class EquipmentTableRowDto(
    val label: String,
    val equipmentType: String,
    val extractedSpecs: Map<String, Any?>?,
    val technicianNotes: String?,
    val photoCount: Int
)
