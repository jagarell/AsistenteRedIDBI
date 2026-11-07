package com.upc.asistenteredidbi.data.repository

import com.upc.asistenteredidbi.data.remote.Api007Service
import com.upc.asistenteredidbi.data.remote.ReportesDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
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
    suspend fun aplicar(minutaId: Long, items: List<Any?>, nuevaVisita: Boolean? = null) = api.aplicarCambios(minutaId, mapOf("items" to items, "nuevaVisita" to nuevaVisita))
    suspend fun antecedentes(cliente: String) = api.antecedentes(cliente)
    suspend fun evaluarSoporte(motivo: String, hechos: List<String>, evidencias: List<String>) =
        api.evaluarSoporte(mapOf("motivo" to motivo, "hechos" to hechos, "evidencias" to evidencias))
    suspend fun crearSoporte(body: Map<String, Any?>) = api.crearSoporte(body)
    suspend fun mensaje(texto: String) = api.mensaje(mapOf("texto" to texto))
    suspend fun mensaje(body: Map<String, Any?>) = api.mensaje(body)
    suspend fun redactar(datos: Any?) = api.redactar(mapOf("datos" to datos))
    suspend fun reporteXlsx(consulta: Map<String, Any?>): ByteArray = api.reporteXlsx(consulta).use { it.bytes() }
    suspend fun prompts(): List<String> = (api.prompts()["prompts"] as? List<*>)?.map { it.toString() }.orEmpty()
    suspend fun guardarPrompts(l: List<String>) = api.guardarPrompts(mapOf("prompts" to l))
    suspend fun subirFoto(minutaId: Long, tipo: String, jpeg: ByteArray) = api.subirFoto(
        minutaId, okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), tipo),
        okhttp3.MultipartBody.Part.createFormData("archivo", "foto.jpg", okhttp3.RequestBody.create("image/jpeg".toMediaTypeOrNull(), jpeg))
    )
    suspend fun foto(minutaId: Long, archivo: String): ByteArray = api.foto(minutaId, archivo).use { it.bytes() }
    suspend fun panel(dias: Int) = api.panel(dias)
    suspend fun panelResumen(periodo: String, desde: String?, hasta: String?) = api.panelResumen(periodo, desde, hasta)
    suspend fun equipo() = api.equipo()
    suspend fun cambiarRol(id: Long, rol: String) = api.equipoRol(id, mapOf("rol" to rol))
    suspend fun asignarSupervisor(id: Long, supervisorId: Long?) = api.equipoSupervisor(id, mapOf("supervisorId" to supervisorId))
    suspend fun reglas() = api.reglas()
    suspend fun reglaCrear(body: Map<String, Any?>) = api.reglaCrear(body)
    suspend fun reglaEditar(id: String, body: Map<String, Any?>) = api.reglaEditar(id, body)
    suspend fun reglaEliminar(id: String) = api.reglaEliminar(id)
    suspend fun preguntasAgregadas() = api.preguntasAgregadas()
    suspend fun aprendizajeHoy() = api.aprendizajeHoy()
    suspend fun aprendizajeAplicar(casos: List<Any?>, solo: String?) = api.aprendizajeAplicar(mapOf("casos" to casos, "solo" to solo))
    suspend fun fichas(casos: List<Any?>, empresa: String = "IDBI") = api.fichas(mapOf("casos" to casos, "empresa" to empresa))
}
