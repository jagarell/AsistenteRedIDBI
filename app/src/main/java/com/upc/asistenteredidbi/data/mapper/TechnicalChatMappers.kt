package com.upc.asistenteredidbi.data.mapper

import com.upc.asistenteredidbi.data.remote.dto.TechnicalChatProposalDto
import com.upc.asistenteredidbi.data.remote.dto.TechnicalChatResponseDto
import com.upc.asistenteredidbi.data.remote.dto.TechnicalEquipmentRecommendationDto
import com.upc.asistenteredidbi.data.remote.dto.TopologyDto
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
        currentStep = currentStep,
        currentQuestionKey = currentQuestionKey,
        currentQuestion = currentQuestion,
        currentInputType = TechnicalChatInputType.fromApiValue(currentInputType),
        currentOptions = currentOptions.orEmpty(),
        answeredQuestions = answeredQuestions,
        totalQuestions = totalQuestions,
        progressPercent = progressPercent,
        completed = completed,
        answers = answers,
        proposal = proposal?.toDomain()
    )
}

fun TechnicalChatProposalDto.toDomain(): TechnicalChatProposal {
    return TechnicalChatProposal(
        summary = summary,
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
        quantity = quantity
    )
}

fun TopologyDto.toDomain(): ChatTopology = ChatTopology(
    nodes = nodes.map { ChatTopologyNode(it.id, it.label, it.type, it.level) },
    links = links.map { ChatTopologyLink(it.source, it.target, it.connectionType, it.status) }
)