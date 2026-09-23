package com.upc.asistenteredidbi.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.data.remote.AuthApiService
import com.upc.asistenteredidbi.data.remote.dto.RefreshRequestDto
import com.upc.asistenteredidbi.data.session.SessionManager
import com.upc.asistenteredidbi.domain.model.toDisplayLabel
import com.upc.asistenteredidbi.domain.usecase.GetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionDestination {
    data object Loading : SessionDestination
    data object Login : SessionDestination
    data object Home : SessionDestination
}

data class DrawerProfile(
    val fullName: String,
    val roleLabel: String,
    val company: String
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val getProfileUseCase: GetProfileUseCase,
    private val authApiService: AuthApiService
) : ViewModel() {

    private val _destination =
        MutableStateFlow<SessionDestination>(SessionDestination.Loading)

    val destination: StateFlow<SessionDestination> =
        _destination.asStateFlow()

    private val _drawerProfile = MutableStateFlow<DrawerProfile?>(null)
    val drawerProfile: StateFlow<DrawerProfile?> = _drawerProfile.asStateFlow()

    private var sessionChecked = false

    init {
        // El AuthInterceptor de OkHttp avisa acá cuando el backend rechaza el
        // JWT (401) desde cualquier pantalla — se reusa el mismo logout()
        // que ya usa el botón manual del drawer, para no duplicar lógica.
        viewModelScope.launch {
            sessionManager.sessionExpiredEvents.collect {
                logout()
            }
        }
    }

    fun checkSession() {
        if (sessionChecked) return

        sessionChecked = true

        viewModelScope.launch {
            _destination.value =
                if (sessionManager.isLoggedIn()) {
                    SessionDestination.Home
                } else {
                    SessionDestination.Login
                }
        }
    }

    fun consumeDestination() {
        _destination.value = SessionDestination.Loading
    }

    /** Datos del header del drawer de navegación; se cargan una sola vez por sesión. */
    fun loadDrawerProfile() {
        if (_drawerProfile.value != null) return

        viewModelScope.launch {
            getProfileUseCase().onSuccess { user ->
                _drawerProfile.value = DrawerProfile(
                    fullName = user.fullName,
                    roleLabel = user.role.toDisplayLabel(),
                    company = user.company.orEmpty()
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val refreshToken = sessionManager.getRefreshToken()
            sessionManager.clearSession()
            _drawerProfile.value = null
            _destination.value = SessionDestination.Login

            // Revoca el refresh token en el servidor para que uno filtrado no
            // siga sirviendo tras un logout intencional. Best-effort: si falla
            // (sin red, backend caído), la sesión local ya se cerró igual.
            if (!refreshToken.isNullOrBlank()) {
                try {
                    authApiService.logout(RefreshRequestDto(refreshToken))
                } catch (e: Exception) {
                    // Nada que hacer: la sesión local ya se cerró.
                }
            }
        }
    }
}