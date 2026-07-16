package com.upc.asistenteredidbi.data.mapper

import com.upc.asistenteredidbi.data.remote.dto.ChatResponseDto
import com.upc.asistenteredidbi.data.remote.dto.ChatResponseExtractedDataDto
import com.upc.asistenteredidbi.domain.model.ChatExtractedData
import com.upc.asistenteredidbi.domain.model.ChatResponseAnswer

fun ChatResponseExtractedDataDto.toDomain(): ChatExtractedData = ChatExtractedData(
    extractedFields = extractedFields,
    confidence = confidence,
    notes = notes
)

fun ChatResponseDto.toDomain(): ChatResponseAnswer = ChatResponseAnswer(
    nodeKey = nodeKey,
    value = value,
    extractedData = extractedData?.toDomain()
)
