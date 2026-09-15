package com.upc.asistenteredidbi.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val getProfileUseCase: GetProfileUseCase
) : ViewModel() {

    private val _destination =
        MutableStateFlow<SessionDestination>(SessionDestination.Loading)

    val destination: StateFlow<SessionDestination> =
        _destination.asStateFlow()

    private val _drawerProfile = MutableStateFlow<DrawerProfile?>(null)
    val drawerProfile: StateFlow<DrawerProfile?> = _drawerProfile.asStateFlow()

    private var sessionChecked = false

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
            sessionManager.clearSession()
            _drawerProfile.value = null
            _destination.value = SessionDestination.Login
        }
    }
}