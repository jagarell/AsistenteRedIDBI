package com.upc.asistenteredidbi.presentation.home

import com.upc.asistenteredidbi.domain.model.EvaluationStage

data class HomeRecentItem(
    val evaluationId: String,
    val title: String,
    val date: String,
    val stage: EvaluationStage,
    val rejected: Boolean = false,
    /** Minutas del equipo o de la empresa (supervisor y administrador): estado en texto y minuta de origen. */
    val estadoTexto: String? = null,
    val minutaId: Long? = null
)
