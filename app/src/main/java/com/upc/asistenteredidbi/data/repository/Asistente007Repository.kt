package com.upc.asistenteredidbi.data.repository

import com.upc.asistenteredidbi.data.remote.Api007Service
import com.upc.asistenteredidbi.data.remote.ReportesDto
import javax.inject.Inject
import javax.inject.Singleton

/** Acceso a las rutas 0.0.7 (Reportes, Asistente IA, soporte, aprendizaje). Los errores de red suben como excepción. */
@Singleton
class Asistente007Repository @Inject constructor(private val api: Api007Service) {

    data class Filtros(
        val desde: String? = null, val hasta: String? = null,
        val tecnico: Set<String> = emptySet(), val supervisor: Set<String> = emptySet(), val rubro: Set<String> = emptySet(),
        val estado: Set<String> = emptySet(), val criticidad: Set<String> = emptySet(), val esfuerzo: Set<String> = emptySet(),
        val plazo: Set<String> = emptySet()
    ) {
        fun count(): Int = listOf(tecnico, supervisor, rubro, estado, criticidad, esfuerzo, plazo).sumOf { it.size } + if (desde != null || hasta != null) 1 else 0
    }

    suspend fun perfil() = api.miPerfilReportes()

    suspend fun reportes(vista: String?, f: Filtros): ReportesDto = api.reportes(
        vista, f.desde, f.hasta, f.tecnico.toList().ifEmpty { null }, f.supervisor.toList().ifEmpty { null }, f.rubro.toList().ifEmpty { null },
        f.estado.toList().ifEmpty { null }, f.criticidad.toList().ifEmpty { null }, f.esfuerzo.toList().ifEmpty { null }, f.plazo.toList().ifEmpty { null }
    )

    suspend fun minuta(minutaId: Long) = api.minuta(minutaId)
    suspend fun versiones(minutaId: Long) = api.versiones(minutaId)
    suspend fun plan(minutaId: Long, texto: String) = api.planCambios(minutaId, mapOf("texto" to texto))
    suspend fun aplicar(minutaId: Long, items: List<Any?>) = api.aplicarCambios(minutaId, mapOf("items" to items))
    suspend fun antecedentes(cliente: String) = api.antecedentes(cliente)
    suspend fun evaluarSoporte(motivo: String, hechos: List<String>, evidencias: List<String>) =
        api.evaluarSoporte(mapOf("motivo" to motivo, "hechos" to hechos, "evidencias" to evidencias))
    suspend fun crearSoporte(body: Map<String, Any?>) = api.crearSoporte(body)
    suspend fun mensaje(texto: String) = api.mensaje(mapOf("texto" to texto))
    suspend fun panel(dias: Int) = api.panel(dias)
    suspend fun equipo() = api.equipo()
    suspend fun cambiarRol(id: Long, rol: String) = api.equipoRol(id, mapOf("rol" to rol))
    suspend fun asignarSupervisor(id: Long, supervisorId: Long?) = api.equipoSupervisor(id, mapOf("supervisorId" to supervisorId))
    suspend fun aprendizajeHoy() = api.aprendizajeHoy()
    suspend fun aprendizajeAplicar(casos: List<Any?>, solo: String?) = api.aprendizajeAplicar(mapOf("casos" to casos, "solo" to solo))
    suspend fun fichas(casos: List<Any?>, empresa: String = "IDBI") = api.fichas(mapOf("casos" to casos, "empresa" to empresa))
}
