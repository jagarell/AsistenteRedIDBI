package com.upc.asistenteredidbi.domain.repository

import android.net.Uri
import com.upc.asistenteredidbi.domain.model.BrandAsset
import com.upc.asistenteredidbi.domain.model.ProfileStats
import com.upc.asistenteredidbi.domain.model.User

/** Contrato del dominio para "Mi Perfil" y "Gestionar marca". */
interface ProfileRepository {
    suspend fun getProfile(): Result<User>
    suspend fun updateProfile(fullName: String?, phone: String?, company: String?, city: String?): Result<User>
    suspend fun getStats(): Result<ProfileStats>
    suspend fun getBrand(): Result<BrandAsset>
    suspend fun updateBrand(companyName: String?, logoUri: Uri?): Result<BrandAsset>
}
