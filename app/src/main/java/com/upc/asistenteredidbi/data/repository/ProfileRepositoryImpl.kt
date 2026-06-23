package com.upc.asistenteredidbi.data.repository

import android.content.Context
import android.net.Uri
import com.upc.asistenteredidbi.data.mapper.toDomain
import com.upc.asistenteredidbi.data.remote.ProfileApiService
import com.upc.asistenteredidbi.data.remote.dto.UpdateProfileRequestDto
import com.upc.asistenteredidbi.data.util.MultipartUtils
import com.upc.asistenteredidbi.domain.model.BrandAsset
import com.upc.asistenteredidbi.domain.model.ProfileStats
import com.upc.asistenteredidbi.domain.model.User
import com.upc.asistenteredidbi.domain.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val api: ProfileApiService,
    @ApplicationContext private val context: Context
) : ProfileRepository {

    override suspend fun getProfile(): Result<User> = safeCall { api.getProfile().toDomain() }

    override suspend fun updateProfile(fullName: String?, phone: String?, company: String?, city: String?): Result<User> = safeCall {
        api.updateProfile(UpdateProfileRequestDto(fullName, phone, company, city)).toDomain()
    }

    override suspend fun getStats(): Result<ProfileStats> = safeCall { api.getProfileStats().toDomain() }

    override suspend fun getBrand(): Result<BrandAsset> = safeCall { api.getBrand().toDomain() }

    override suspend fun updateBrand(companyName: String?, logoUri: Uri?): Result<BrandAsset> = safeCall {
        val companyPart = companyName?.let { MultipartUtils.textPart(it) }
        val logoPart = logoUri?.let {
            val tempFile = MultipartUtils.uriToTempFile(context, it, prefix = "brand_logo")
            MultipartUtils.filePart(tempFile, partName = "logo")
        }
        api.updateBrand(companyPart, logoPart).toDomain()
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
