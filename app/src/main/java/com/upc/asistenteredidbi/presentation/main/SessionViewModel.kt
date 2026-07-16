package com.upc.asistenteredidbi.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.data.session.SessionManager
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

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _destination =
        MutableStateFlow<SessionDestination>(SessionDestination.Loading)

    val destination: StateFlow<SessionDestination> =
        _destination.asStateFlow()

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
}