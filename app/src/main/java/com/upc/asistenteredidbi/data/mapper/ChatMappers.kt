package com.upc.asistenteredidbi.data.mapper

import com.upc.asistenteredidbi.data.remote.dto.ChatClosingSummaryDto
import com.upc.asistenteredidbi.data.remote.dto.ChatNodeDto
import com.upc.asistenteredidbi.data.remote.dto.ChatProgressDto
import com.upc.asistenteredidbi.data.remote.dto.ChatResponseDto
import com.upc.asistenteredidbi.data.remote.dto.ChatResponseExtractedDataDto
import com.upc.asistenteredidbi.domain.model.ChatClosingSummary
import com.upc.asistenteredidbi.domain.model.ChatExtractedData
import com.upc.asistenteredidbi.domain.model.ChatInputType
import com.upc.asistenteredidbi.domain.model.ChatNode
import com.upc.asistenteredidbi.domain.model.ChatProgress
import com.upc.asistenteredidbi.domain.model.ChatResponseAnswer

fun ChatNodeDto.toDomain(): ChatNode = ChatNode(
    nodeKey = nodeKey,
    promptText = promptText,
    inputType = ChatInputType.fromApiValue(inputType),
    category = category,
    choices = choices,
    countFields = countFields,
    selectOptions = selectOptions,
    connectionMapItems = connectionMapItems,
    guideLinkLabel = guideLinkLabel,
    guideLinkUrl = guideLinkUrl,
    infoTip = infoTip,
    requiresPhotoAnalysis = requiresPhotoAnalysis
)

fun ChatProgressDto.toDomain(): ChatProgress = ChatProgress(
    evaluationId = evaluationId,
    nextNode = nextNode?.toDomain(),
    isComplete = isComplete,
    answeredCount = answeredCount
)

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

fun ChatClosingSummaryDto.toDomain(): ChatClosingSummary = ChatClosingSummary(
    evaluationId = evaluationId,
    summaryText = summaryText,
    suggestedLocalAreas = suggestedLocalAreas,
    suggestedEquipmentCount = suggestedEquipmentCount
)
