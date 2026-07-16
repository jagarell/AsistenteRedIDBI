package com.upc.asistenteredidbi.data.mapper

import com.upc.asistenteredidbi.data.remote.dto.BrandAssetDto
import com.upc.asistenteredidbi.data.remote.dto.ForgotPasswordResponseDto
import com.upc.asistenteredidbi.data.remote.dto.LoginResponseDto
import com.upc.asistenteredidbi.data.remote.dto.ProfileStatsDto
import com.upc.asistenteredidbi.data.remote.dto.RegisterResponseDto
import com.upc.asistenteredidbi.data.remote.dto.ResetPasswordResponseDto
import com.upc.asistenteredidbi.data.remote.dto.UserDto
import com.upc.asistenteredidbi.domain.model.AuthSession
import com.upc.asistenteredidbi.domain.model.BrandAsset
import com.upc.asistenteredidbi.domain.model.ForgotPasswordResult
import com.upc.asistenteredidbi.domain.model.ProfileStats
import com.upc.asistenteredidbi.domain.model.RegisterResult
import com.upc.asistenteredidbi.domain.model.ResetPasswordResult
import com.upc.asistenteredidbi.domain.model.Role
import com.upc.asistenteredidbi.domain.model.User


fun UserDto.toDomain(): User = User(
    id = id,
    fullName = fullName,
    email = email,
    phone = phone,
    company = company,
    city = city,
    role = Role.fromString(role)
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

fun RegisterResponseDto.toRegisterResult(): RegisterResult {
    return RegisterResult(
        id = id,
        fullName = fullName,
        email = email,
        phone = phone,
        company = company,
        city = city,
        role = Role.fromString(role),
        message = message
    )
}

fun LoginResponseDto.toDomain(): AuthSession {
    return AuthSession(
        accessToken = accessToken,
        expiresInMinutes = expiresInMinutes,
        userId = userId,
        fullName = fullName,
        role = Role.fromString(role)
    )
}

fun ResetPasswordResponseDto.toDomain() =
    ResetPasswordResult(
        message = message
    )

fun ForgotPasswordResponseDto.toDomain(): ForgotPasswordResult {
    return ForgotPasswordResult(
        message = message,
        expiresInMinutes = expiresInMinutes
    )
}