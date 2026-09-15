package com.upc.asistenteredidbi.presentation.configuration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.data.session.AppPreferences
import com.upc.asistenteredidbi.domain.model.User
import com.upc.asistenteredidbi.domain.usecase.GetProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfiguracionUiState(
    val user: User? = null,
    val pushNotifications: Boolean = true,
    val emailNotifications: Boolean = true,
    val systemAlerts: Boolean = false,
    val twoFactorAuth: Boolean = false
)

@HiltViewModel
class ConfiguracionViewModel @Inject constructor(
    private val getProfileUseCase: GetProfileUseCase,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfiguracionUiState())
    val uiState: StateFlow<ConfiguracionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                appPreferences.pushNotificationsFlow,
                appPreferences.emailNotificationsFlow,
                appPreferences.systemAlertsFlow,
                appPreferences.twoFactorAuthFlow
            ) { push, email, alerts, twoFactor ->
                Quad(push, email, alerts, twoFactor)
            }.collect { (push, email, alerts, twoFactor) ->
                _uiState.update {
                    it.copy(
                        pushNotifications = push,
                        emailNotifications = email,
                        systemAlerts = alerts,
                        twoFactorAuth = twoFactor
                    )
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            getProfileUseCase().onSuccess { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun setPushNotifications(enabled: Boolean) = viewModelScope.launch { appPreferences.setPushNotifications(enabled) }
    fun setEmailNotifications(enabled: Boolean) = viewModelScope.launch { appPreferences.setEmailNotifications(enabled) }
    fun setSystemAlerts(enabled: Boolean) = viewModelScope.launch { appPreferences.setSystemAlerts(enabled) }
    fun setTwoFactorAuth(enabled: Boolean) = viewModelScope.launch { appPreferences.setTwoFactorAuth(enabled) }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
