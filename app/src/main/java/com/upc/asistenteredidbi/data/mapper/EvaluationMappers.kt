package com.upc.asistenteredidbi.data.mapper

import com.upc.asistenteredidbi.data.remote.dto.EvaluationDto
import com.upc.asistenteredidbi.data.remote.dto.EvaluationListItemDto
import com.upc.asistenteredidbi.domain.model.Evaluation
import com.upc.asistenteredidbi.domain.model.EvaluationStatus
import com.upc.asistenteredidbi.domain.model.EvaluationSummaryItem

/** Estados reales de `EvaluationStatus` en el gateway (COMPLETADO/BORRADOR/ENVIADO/EN_ANALISIS). */
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
    id = id.toString(),
    establishmentName = restaurantName,
    // El gateway no tiene un concepto de "cliente" separado del
    // establecimiento para el listado — nunca se fabrica un valor.
    clientName = null,
    status = gatewayStatusToDomain(status),
    overallScore = score?.toFloat(),
    createdAt = createdAt
)
