package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.BrandAssetDto
import com.upc.asistenteredidbi.data.remote.dto.ProfileStatsDto
import com.upc.asistenteredidbi.data.remote.dto.UpdateProfileRequestDto
import com.upc.asistenteredidbi.data.remote.dto.UserDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Part

/** Servicio Retrofit — refleja `profile_router.py` (Mi Perfil, Gestionar marca). */
interface ProfileApiService {

    @GET("api/v1/profile/me")
    suspend fun getProfile(): UserDto

    @PATCH("api/v1/profile/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): UserDto

    @GET("api/v1/profile/me/stats")
    suspend fun getProfileStats(): ProfileStatsDto

    @GET("api/v1/profile/me/brand")
    suspend fun getBrand(): BrandAssetDto

    @Multipart
    @PUT("api/v1/profile/me/brand")
    suspend fun updateBrand(
        @Part("company_name") companyName: RequestBody?,
        @Part logo: MultipartBody.Part?
    ): BrandAssetDto
}
