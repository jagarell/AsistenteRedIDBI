package com.upc.asistenteredidbi.data.session

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Estado de la conexión del celular (WiFi o datos móviles, doc 0.0.7 §34): alimenta la barra «Sin conexión» y vacía la cola al volver la señal. */
@Singleton
class ConnectivityMonitor @Inject constructor(@ApplicationContext private val ctx: Context) {

    private val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _online = MutableStateFlow(hayRed())
    val online: StateFlow<Boolean> = _online.asStateFlow()
    private var iniciado = false

    private fun hayRed(): Boolean = runCatching {
        cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }.getOrDefault(true)

    fun iniciar() {
        if (iniciado) return
        iniciado = true
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { _online.value = true }
                // onLost de la red por defecto = no queda ninguna (si hay otra, llega onAvailable enseguida). No se consulta
                // activeNetwork aquí: durante unos instantes todavía devuelve la red que se acaba de perder.
                override fun onLost(network: Network) { _online.value = false }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _online.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            })
        }
    }
}
