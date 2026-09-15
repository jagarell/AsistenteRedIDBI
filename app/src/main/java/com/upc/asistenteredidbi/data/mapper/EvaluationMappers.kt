package com.upc.asistenteredidbi.data.mapper

import com.upc.asistenteredidbi.data.remote.dto.EvaluationDto
import com.upc.asistenteredidbi.data.remote.dto.EvaluationListItemDto
import com.upc.asistenteredidbi.domain.model.Evaluation
import com.upc.asistenteredidbi.domain.model.EvaluationStatus
import com.upc.asistenteredidbi.domain.model.EvaluationSummaryItem

/** Estados reales de `EvaluationStatus` en el gateway (COMPLETADO/BORRADOR/ENVIADO/EN_ANALISIS) —
 *  distintos de los que usa `EvaluationStatus.fromApiValue` para el listado mock de Historial. */
private fun gatewayStatusToDomain(value: String): EvaluationStatus = when (value.uppercase()) {
    "COMPLETADO" -> EvaluationStatus.GENERADA
    "ENVIADO", "EN_ANALISIS" -> EvaluationStatus.EN_PROGRESO
    else -> EvaluationStatus.BORRADOR
}

fun EvaluationDto.toDomain(): Evaluation = Evaluation(
    id = id.toString(),
    establishmentName = restaurantName,
    establishmentAddress = address,
    status = gatewayStatusToDomain(status),
    createdAt = createdAt
)

fun EvaluationListItemDto.toDomain(): EvaluationSummaryItem = EvaluationSummaryItem(
    id = id,
    establishmentName = establishmentName,
    clientName = clientName,
    status = EvaluationStatus.fromApiValue(status),
    overallScore = overallScore,
    createdAt = createdAt
)
