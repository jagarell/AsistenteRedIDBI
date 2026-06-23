package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.ForgotPasswordRequestDto
import com.upc.asistenteredidbi.data.remote.dto.GenericMessageDto
import com.upc.asistenteredidbi.data.remote.dto.LoginRequestDto
import com.upc.asistenteredidbi.data.remote.dto.RegisterRequestDto
import com.upc.asistenteredidbi.data.remote.dto.ResetPasswordRequestDto
import com.upc.asistenteredidbi.data.remote.dto.TokenResponseDto
import com.upc.asistenteredidbi.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Servicio Retrofit — refleja exactamente `auth_router.py` (HU01). */
interface AuthApiService {

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): TokenResponseDto

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): TokenResponseDto

    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): UserDto

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequestDto): GenericMessageDto

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequestDto): GenericMessageDto
}