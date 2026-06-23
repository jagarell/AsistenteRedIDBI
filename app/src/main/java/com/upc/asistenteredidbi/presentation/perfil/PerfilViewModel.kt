package com.upc.asistenteredidbi.presentation.perfil

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.domain.model.BrandAsset
import com.upc.asistenteredidbi.domain.model.ProfileStats
import com.upc.asistenteredidbi.domain.model.User
import com.upc.asistenteredidbi.domain.usecase.GetBrandUseCase
import com.upc.asistenteredidbi.domain.usecase.GetProfileStatusUseCase
import com.upc.asistenteredidbi.domain.usecase.GetProfileUseCase
import com.upc.asistenteredidbi.domain.usecase.UpdateBrandUseCase
import com.upc.asistenteredidbi.domain.usecase.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PerfilUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val stats: ProfileStats? = null,
    val brand: BrandAsset? = null,
    val isEditing: Boolean = false,
    val draftFullName: String = "",
    val draftPhone: String = "",
    val draftCity: String = "",
    val isUploadingLogo: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val getProfileStatusUseCase: GetProfileStatusUseCase,
    private val getBrandUseCase: GetBrandUseCase,
    private val updateBrandUseCase: UpdateBrandUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getProfileUseCase()
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false, user = user,
                            draftFullName = user.fullName, draftPhone = user.phone.orEmpty(), draftCity = user.city.orEmpty()
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }

            getProfileStatusUseCase().onSuccess { stats -> _uiState.update { it.copy(stats = stats) } }
            getBrandUseCase().onSuccess { brand -> _uiState.update { it.copy(brand = brand) } }
        }
    }

    fun startEditing() = _uiState.update { it.copy(isEditing = true) }
    fun cancelEditing() = _uiState.update {
        it.copy(isEditing = false, draftFullName = it.user?.fullName.orEmpty(), draftPhone = it.user?.phone.orEmpty(), draftCity = it.user?.city.orEmpty())
    }
    fun onDraftFullNameChange(value: String) = _uiState.update { it.copy(draftFullName = value) }
    fun onDraftPhoneChange(value: String) = _uiState.update { it.copy(draftPhone = value) }
    fun onDraftCityChange(value: String) = _uiState.update { it.copy(draftCity = value) }

    fun saveProfile() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            updateProfileUseCase(state.draftFullName, state.draftPhone, state.user?.company, state.draftCity)
                .onSuccess { user -> _uiState.update { it.copy(isLoading = false, isEditing = false, user = user) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    /** "Gestionar marca": el logo se usa luego en el membrete del PDF de la Propuesta (Sprint 2). */
    fun updateBrandLogo(logoUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingLogo = true, errorMessage = null) }
            updateBrandUseCase(_uiState.value.user?.company, logoUri)
                .onSuccess { brand -> _uiState.update { it.copy(isUploadingLogo = false, brand = brand) } }
                .onFailure { error -> _uiState.update { it.copy(isUploadingLogo = false, errorMessage = error.message) } }
        }
    }
}
