package com.upc.asistenteredidbi.data.mapper

import com.upc.asistenteredidbi.data.remote.dto.EvaluationDto
import com.upc.asistenteredidbi.data.remote.dto.EvaluationListItemDto
import com.upc.asistenteredidbi.domain.model.Evaluation
import com.upc.asistenteredidbi.domain.model.EvaluationStatus
import com.upc.asistenteredidbi.domain.model.EvaluationSummaryItem

fun EvaluationDto.toDomain(): Evaluation = Evaluation(
    id = id,
    establishmentId = establishmentId,
    establishmentName = establishmentName,
    establishmentAddress = establishmentAddress,
    googleMapsUrl = googleMapsUrl,
    status = EvaluationStatus.fromApiValue(status),
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
