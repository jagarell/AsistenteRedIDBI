package com.upc.asistenteredidbi.data.mapper

import com.upc.asistenteredidbi.data.remote.dto.MinutaRecordResponseDto
import com.upc.asistenteredidbi.domain.model.MinutaRecord
import com.upc.asistenteredidbi.domain.model.MinutaRecordStatus

fun MinutaRecordResponseDto.toDomain(): MinutaRecord = MinutaRecord(
    id = id,
    evaluationId = evaluationId,
    clientName = clientName,
    address = address,
    technicianId = technicianId,
    technicianName = technicianName,
    status = MinutaRecordStatus.fromApiValue(status),
    summary = summary,
    createdAt = createdAt,
    updatedAt = updatedAt,
    validatedById = validatedById,
    validatedByName = validatedByName,
    validatedAt = validatedAt
)
