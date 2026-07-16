package com.upc.asistenteredidbi.data.mapper

import com.upc.asistenteredidbi.data.remote.dto.EquipmentLineDto
import com.upc.asistenteredidbi.data.remote.dto.ProposalPdfRequestDto
import com.upc.asistenteredidbi.domain.model.ProposalPdfData

fun ProposalPdfData.toDto(): ProposalPdfRequestDto = ProposalPdfRequestDto(
    establishmentName = establishmentName,
    address = address,
    technicianName = technicianName,
    score = score,
    summary = summary,
    recommendations = recommendations,
    equipment = equipment.map { EquipmentLineDto(it.name, it.description, it.quantity) },
    topologyText = topologyText
)
