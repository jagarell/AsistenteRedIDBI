package com.upc.asistenteredidbi.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EvidencePhotoDto(
    @Json(name = "id") val id: String,
    @Json(name = "file_url") val fileUrl: String,
    @Json(name = "comment") val comment: String?,
    @Json(name = "captured_at") val capturedAt: String
)

@JsonClass(generateAdapter = true)
data class EvidenceAreaItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "is_custom") val isCustom: Boolean,
    @Json(name = "photos") val photos: List<EvidencePhotoDto>
)

@JsonClass(generateAdapter = true)
data class EvidenceEquipmentItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "equipment_type") val equipmentType: String,
    @Json(name = "label") val label: String,
    @Json(name = "is_custom") val isCustom: Boolean,
    @Json(name = "extracted_specs") val extractedSpecs: Map<String, Any?>?,
    @Json(name = "technician_notes") val technicianNotes: String?,
    @Json(name = "photos") val photos: List<EvidencePhotoDto>
)

@JsonClass(generateAdapter = true)
data class EvidenceChecklistDto(
    @Json(name = "evaluation_id") val evaluationId: String,
    @Json(name = "selection_locked") val selectionLocked: Boolean,
    @Json(name = "areas") val areas: List<EvidenceAreaItemDto>,
    @Json(name = "equipment") val equipment: List<EvidenceEquipmentItemDto>,
    @Json(name = "all_items_have_photo") val allItemsHavePhoto: Boolean
)

@JsonClass(generateAdapter = true)
data class CreateCustomAreaRequestDto(@Json(name = "name") val name: String)

@JsonClass(generateAdapter = true)
data class CreateCustomEquipmentRequestDto(
    @Json(name = "equipment_type") val equipmentType: String,
    @Json(name = "label") val label: String
)

@JsonClass(generateAdapter = true)
data class LockSelectionRequestDto(@Json(name = "confirm") val confirm: Boolean = true)

@JsonClass(generateAdapter = true)
data class UpdateEquipmentNotesRequestDto(
    @Json(name = "technician_notes") val technicianNotes: String?,
    @Json(name = "extracted_specs") val extractedSpecs: Map<String, Any?>?
)

@JsonClass(generateAdapter = true)
data class MinutaDto(
    @Json(name = "evaluation_id") val evaluationId: String,
    @Json(name = "establishment_name") val establishmentName: String,
    @Json(name = "establishment_address") val establishmentAddress: String?,
    @Json(name = "conversation_responses") val conversationResponses: List<ChatResponseDto>,
    @Json(name = "areas") val areas: List<EvidenceAreaItemDto>,
    @Json(name = "equipment") val equipment: List<EvidenceEquipmentItemDto>,
    @Json(name = "equipment_table") val equipmentTable: List<EquipmentTableRowDto>
)

@JsonClass(generateAdapter = true)
data class EquipmentTableRowDto(
    @Json(name = "label") val label: String,
    @Json(name = "equipment_type") val equipmentType: String,
    @Json(name = "extracted_specs") val extractedSpecs: Map<String, Any?>?,
    @Json(name = "technician_notes") val technicianNotes: String?,
    @Json(name = "photo_count") val photoCount: Int
)
