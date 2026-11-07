package com.upc.asistenteredidbi.presentation.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.data.remote.FilaReporteDto
import com.upc.asistenteredidbi.data.remote.ReportesDto
import com.upc.asistenteredidbi.data.repository.Asistente007Repository
import com.upc.asistenteredidbi.data.repository.Asistente007Repository.Filtros
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Pestaña de Reportes: el gateway decide cuáles tiene el rol (General, Por técnico / Mis técnicos, Mis minutas). */
data class ReportesTab(val key: String, val label: String)

data class ReportesUiState(
    val loading: Boolean = true,
    val tabs: List<ReportesTab> = emptyList(),
    val tab: String? = null,
    /** Datos sin filtros (de aquí salen las opciones de cada filtro). */
    val base: ReportesDto? = null,
    val data: ReportesDto? = null,
    /** Filtros por pestaña: General y Mis minutas se filtran por separado. */
    val filtros: Map<String, Filtros> = emptyMap(),
    val selected: FilaReporteDto? = null,
    val versiones: List<Map<String, Any?>> = emptyList(),
    val error: String? = null
) {
    val filtro: Filtros get() = filtros[tab] ?: Filtros()
}

@HiltViewModel
class ReportesViewModel @Inject constructor(private val repo: Asistente007Repository) : ViewModel() {

    private val _state = MutableStateFlow(ReportesUiState())
    val state: StateFlow<ReportesUiState> = _state.asStateFlow()

    fun load(initialTab: String? = null, estado: String? = null, criticidad: String? = null) = viewModelScope.launch {
        try {
            val p = repo.perfil()
            val tabs = buildList {
                if (p.all) add(ReportesTab("general", "General"))
                if (p.tecs == "all") add(ReportesTab("tecnicos", "Por técnico")) else if (p.tecs == "mine") add(ReportesTab("tecnicos", "Mis técnicos"))
                if (p.own) add(ReportesTab("mis", "Mis minutas"))
            }
            val tab = initialTab?.takeIf { t -> tabs.any { it.key == t } } ?: _state.value.tab?.takeIf { t -> tabs.any { it.key == t } } ?: tabs.firstOrNull()?.key
            // Desde los indicadores del Inicio: Reportes ya filtrado («Por validar · Estado: Completa»).
            val inicial = if (estado != null || criticidad != null) Filtros(estado = setOfNotNull(estado), criticidad = setOfNotNull(criticidad)) else null
            _state.update { it.copy(tabs = tabs, tab = tab, filtros = if (inicial != null && tab != null) it.filtros + (tab to inicial) else it.filtros) }
            refresh(base = true)
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = "No se pudo cargar Reportes. Revisa tu conexión.") }
        }
    }

    fun selectTab(key: String) {
        _state.update { it.copy(tab = key, selected = null, versiones = emptyList()) }
        refresh(base = true)
    }

    fun setFiltro(f: Filtros) {
        val tab = _state.value.tab ?: return
        _state.update { it.copy(filtros = it.filtros + (tab to f)) }
        refresh(base = false)
    }

    fun clearFiltros() = setFiltro(Filtros())

    fun select(row: FilaReporteDto) = viewModelScope.launch {
        _state.update { it.copy(selected = row, versiones = emptyList()) }
        val v = runCatching { repo.versiones(row.id) }.getOrDefault(emptyList())
        _state.update { it.copy(versiones = v) }
    }

    private fun refresh(base: Boolean) = viewModelScope.launch {
        val s = _state.value
        val tab = s.tab ?: run { _state.update { it.copy(loading = false) }; return@launch }
        _state.update { it.copy(loading = true, error = null) }
        try {
            val b = if (base || s.base == null || s.base.vista != tab) repo.reportes(tab, Filtros()) else s.base
            val d = if (s.filtro.count() == 0) b else repo.reportes(tab, s.filtro)
            _state.update { it.copy(loading = false, base = b, data = d, selected = it.selected ?: d.minutas.firstOrNull()) }
            _state.value.selected?.let { if (_state.value.versiones.isEmpty()) select(it) }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = "No se pudo cargar Reportes. Revisa tu conexión.") }
        }
    }
}
