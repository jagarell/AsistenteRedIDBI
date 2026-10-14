package com.upc.asistenteredidbi.presentation.historial

import com.upc.asistenteredidbi.domain.model.EvaluationStage

data class HistoryItem(
    val id: String,
    val establishmentName: String,
    val location: String,
    val date: String,
    val stage: EvaluationStage,
    val minutaId: Long?,
    val rejected: Boolean,
    val progress: Int
)
