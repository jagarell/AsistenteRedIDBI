package com.upc.asistenteredidbi.data.mapper

import com.upc.asistenteredidbi.data.remote.dto.ChatNodeDto
import com.upc.asistenteredidbi.data.remote.dto.TechnicalChatProposalDto
import com.upc.asistenteredidbi.data.remote.dto.TechnicalChatResponseDto
import com.upc.asistenteredidbi.data.remote.dto.TechnicalEquipmentRecommendationDto
import com.upc.asistenteredidbi.data.remote.dto.TopologyDto
import com.upc.asistenteredidbi.domain.model.ChatEvidenceResult
import com.upc.asistenteredidbi.domain.model.ChatFollowUp
import com.upc.asistenteredidbi.domain.model.ChatNodePrompt
import com.upc.asistenteredidbi.domain.model.ChatOption
import com.upc.asistenteredidbi.domain.model.ChatTopology
import com.upc.asistenteredidbi.domain.model.ChatTopologyLink
import com.upc.asistenteredidbi.domain.model.ChatTopologyNode
import com.upc.asistenteredidbi.domain.model.TechnicalChatInputType
import com.upc.asistenteredidbi.domain.model.TechnicalChatProgress
import com.upc.asistenteredidbi.domain.model.TechnicalChatProposal
import com.upc.asistenteredidbi.domain.model.TechnicalEquipmentRecommendation

fun TechnicalChatResponseDto.toDomain(): TechnicalChatProgress {
    return TechnicalChatProgress(
        evaluationId = evaluationId,
        state = state,
        node = node?.toDomain(),
        answeredQuestions = answeredQuestions,
        totalQuestions = totalQuestions,
        progressPercent = progressPercent,
        completed = completed,
        answers = answers,
        proposal = proposal?.toDomain(),
        validationError = validationError,
        lastEvidence = lastEvidence?.let {
            ChatEvidenceResult(
                code = it.code,
                scope = it.scope.orEmpty(),
                area = it.area,
                equipo = it.equipo,
                count = it.count ?: 0,
                extracted = it.extracted.orEmpty()
            )
        },
        crossChecks = crossChecks.orEmpty(),
        followUps = followUps.orEmpty().map { ChatFollowUp(it.key, it.text, it.options.orEmpty()) },
        processedImages = processedImages.orEmpty()
    )
}

fun ChatNodeDto.toDomain(): ChatNodePrompt = ChatNodePrompt(
    nodeId = nodeId,
    scope = scope.orEmpty(),
    kind = kind,
    inputType = TechnicalChatInputType.fromApiValue(inputType),
    text = text,
    options = options.orEmpty().map { ChatOption(it.value, it.label) },
    required = required ?: true,
    block = block.orEmpty(),
    blockLabel = blockLabel.orEmpty(),
    blockIndex = blockIndex ?: 0,
    blockCount = blockCount ?: 0,
    questionNumber = questionNumber ?: 0,
    questionTotal = questionTotal ?: 0,
    evidenceNumber = evidenceNumber ?: 0,
    evidenceTotal = evidenceTotal ?: 0,
    defaultValue = defaultValue,
    evidenceCode = evidenceCode,
    skippable = skippable ?: true,
    maxFiles = maxFiles ?: 3,
    severity = severity,
    // Moshi decodifica los números de un Map<String, Any?> como Double.
    minSelected = (validation?.get("minSelected") as? Number)?.toInt() ?: 0,
    context = context.orEmpty(),
    keyboard = keyboard ?: "text",
    hint = hint.orEmpty()
)

fun TechnicalChatProposalDto.toDomain(): TechnicalChatProposal {
    return TechnicalChatProposal(
        summary = summary,
        asIsFindings = asIsFindings.orEmpty(),
        recommendations = recommendations,
        equipment = equipment.map { item ->
            item.toDomain()
        },
        topologyText = topologyText,
        topology = topology?.toDomain(),
        score = score
    )
}

fun TechnicalEquipmentRecommendationDto.toDomain():
        TechnicalEquipmentRecommendation {

    return TechnicalEquipmentRecommendation(
        name = name,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice
    )
}

fun TopologyDto.toDomain(): ChatTopology = ChatTopology(
    nodes = nodes.map { ChatTopologyNode(it.id, it.label, it.type, it.level, it.detail, it.pending ?: false) },
    links = links.map { ChatTopologyLink(it.source, it.target, it.connectionType, it.status) }
)