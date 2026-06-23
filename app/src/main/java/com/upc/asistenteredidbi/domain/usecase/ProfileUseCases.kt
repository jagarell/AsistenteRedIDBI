package com.upc.asistenteredidbi.domain.usecase

import android.net.Uri
import com.upc.asistenteredidbi.domain.model.BrandAsset
import com.upc.asistenteredidbi.domain.model.ProfileStats
import com.upc.asistenteredidbi.domain.model.User
import com.upc.asistenteredidbi.domain.repository.ProfileRepository
import javax.inject.Inject

class GetProfileUseCase @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(): Result<User> = repository.getProfile()
}

class UpdateProfileUseCase @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(fullName: String?, phone: String?, company: String?, city: String?): Result<User> =
        repository.updateProfile(fullName, phone, company, city)
}

class GetProfileStatsUseCase @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(): Result<ProfileStats> = repository.getStats()
}

class GetBrandUseCase @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(): Result<BrandAsset> = repository.getBrand()
}

/** "Gestionar marca" en Mi Perfil: logo usado luego en el membrete del PDF de la Propuesta. */
class UpdateBrandUseCase @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(companyName: String?, logoUri: Uri?): Result<BrandAsset> =
        repository.updateBrand(companyName, logoUri)
}
