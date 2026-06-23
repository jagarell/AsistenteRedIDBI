package com.upc.asistenteredidbi.data.mapper

import com.upc.asistenteredidbi.data.remote.dto.BrandAssetDto
import com.upc.asistenteredidbi.data.remote.dto.ProfileStatsDto
import com.upc.asistenteredidbi.data.remote.dto.TokenResponseDto
import com.upc.asistenteredidbi.data.remote.dto.UserDto
import com.upc.asistenteredidbi.domain.model.AuthSession
import com.upc.asistenteredidbi.domain.model.BrandAsset
import com.upc.asistenteredidbi.domain.model.ProfileStats
import com.upc.asistenteredidbi.domain.model.User

fun TokenResponseDto.toDomain(): AuthSession = AuthSession(
    accessToken = accessToken,
    expiresInMinutes = expiresInMinutes
)

fun UserDto.toDomain(): User = User(
    id = id,
    fullName = fullName,
    email = email,
    phone = phone,
    company = company,
    city = city,
    role = role
)

fun ProfileStatsDto.toDomain(): ProfileStats = ProfileStats(
    totalEvaluations = totalEvaluations,
    evaluationsThisMonth = evaluationsThisMonth,
    totalProposals = totalProposals
)

fun BrandAssetDto.toDomain(): BrandAsset = BrandAsset(
    logoUrl = logoUrl,
    companyName = companyName
)
