package com.upc.asistenteredidbi.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatNodeDto(
    @Json(name = "node_key") val nodeKey: String,
    @Json(name = "prompt_text") val promptText: String,
    @Json(name = "input_type") val inputType: String,
    @Json(name = "category") val category: String?,
    @Json(name = "choices") val choices: List<String>?,
    @Json(name = "count_fields") val countFields: List<String>?,
    @Json(name = "select_options") val selectOptions: List<String>?,
    @Json(name = "connection_map_items") val connectionMapItems: List<String>?,
    @Json(name = "guide_link_label") val guideLinkLabel: String?,
    @Json(name = "guide_link_url") val guideLinkUrl: String?,
    @Json(name = "info_tip") val infoTip: String?,
    @Json(name = "requires_photo_analysis") val requiresPhotoAnalysis: Boolean
)

@JsonClass(generateAdapter = true)
data class ChatProgressDto(
    @Json(name = "evaluation_id") val evaluationId: String,
    @Json(name = "next_node") val nextNode: ChatNodeDto?,
    @Json(name = "is_complete") val isComplete: Boolean,
    @Json(name = "answered_count") val answeredCount: Int
)

@JsonClass(generateAdapter = true)
data class SaveChatResponseRequestDto(
    @Json(name = "node_key") val nodeKey: String,
    @Json(name = "value") val value: String
)

@JsonClass(generateAdapter = true)
data class ChatResponseExtractedDataDto(
    @Json(name = "extracted_fields") val extractedFields: Map<String, String?>,
    @Json(name = "confidence") val confidence: Float,
    @Json(name = "notes") val notes: String?
)

@JsonClass(generateAdapter = true)
data class ChatResponseDto(
    @Json(name = "node_key") val nodeKey: String,
    @Json(name = "value") val value: String,
    @Json(name = "extracted_data") val extractedData: ChatResponseExtractedDataDto?
)

@JsonClass(generateAdapter = true)
data class ChatClosingSummaryDto(
    @Json(name = "evaluation_id") val evaluationId: String,
    @Json(name = "summary_text") val summaryText: String,
    @Json(name = "suggested_local_areas") val suggestedLocalAreas: List<String>,
    @Json(name = "suggested_equipment_count") val suggestedEquipmentCount: Map<String, Int>
)

@JsonClass(generateAdapter = true)
data class ConfirmClosingSummaryRequestDto(
    @Json(name = "additional_notes") val additionalNotes: String?
)
