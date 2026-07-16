package com.upc.asistenteredidbi.data.remote.dto

data class ChatAnswerRequestDto(
    val evaluationId: String,
    val currentStep: Int,
    val answer: String,
    val answers: Map<String, String>
)

data class ChatProposalDto(
    val summary: String,
    val recommendations: List<String>,
    val equipment: List<EquipmentRecommendationDto>,
    val topologyText: String
)

data class EquipmentRecommendationDto(
    val name: String,
    val description: String,
    val quantity: Int
)