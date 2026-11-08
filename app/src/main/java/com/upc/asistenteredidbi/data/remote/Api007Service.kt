package com.upc.asistenteredidbi.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Rutas 0.0.7 del gateway: Reportes por rol, perfil de Reportes, actualizar una minuta con el Asistente IA (plan, aplicar,
 * versiones), minuta de soporte, mensaje al Asistente IA, «¿Qué aprendiste hoy?» y fichas de aprendizaje.
 */
interface Api007Service {

    /** Ubicación GPS del local: dirección, distrito, departamento y coincidencia con el nombre (Google Maps). */
    @POST("api/geo/reverse")
    suspend fun geoReverse(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/roles")
    suspend fun roles(): List<Map<String, @JvmSuppressWildcards Any?>>

    @retrofit2.http.PUT("api/roles/{id}")
    suspend fun rolActualizar(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/roles")
    suspend fun rolCrear(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/roles/mi-perfil-reportes")
    suspend fun miPerfilReportes(): ReportProfileDto

    @GET("api/reportes")
    suspend fun reportes(
        @Query("vista") vista: String?,
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null,
        @Query("tecnico") tecnico: List<String>? = null,
        @Query("supervisor") supervisor: List<String>? = null,
        @Query("rubro") rubro: List<String>? = null,
        @Query("estado") estado: List<String>? = null,
        @Query("criticidad") criticidad: List<String>? = null,
        @Query("esfuerzo") esfuerzo: List<String>? = null,
        @Query("plazo") plazo: List<String>? = null
    ): ReportesDto

    @POST("api/minutas/{id}/cambios/plan")
    suspend fun planCambios(@Path("id") id: Long, @Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/minutas/{id}/cambios/aplicar")
    suspend fun aplicarCambios(@Path("id") id: Long, @Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    /** Minuta guardada (incluye contentJson con el documento de soporte). */
    @GET("api/minutas/{id}")
    suspend fun minuta(@Path("id") id: Long): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/minutas/{id}/versiones")
    suspend fun versiones(@Path("id") id: Long): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("api/clientes/antecedentes")
    suspend fun antecedentes(@Query("cliente") cliente: String): List<Map<String, @JvmSuppressWildcards Any?>>

    @POST("api/asistente/soporte/evaluar")
    suspend fun evaluarSoporte(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/minutas/soporte")
    suspend fun crearSoporte(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/asistente/mensaje")
    suspend fun mensaje(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    /** «Redactar minuta» del borrador que armó el asistente. */
    @POST("api/asistente/redactar")
    suspend fun redactar(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    /** Reporte del Asistente IA en Excel (.xlsx) según su consulta (periodo, estado, persona…). */
    @retrofit2.http.Streaming
    @POST("api/asistente/reporte.xlsx")
    suspend fun reporteXlsx(@Body consulta: Map<String, @JvmSuppressWildcards Any?>): okhttp3.ResponseBody

    /** Indicadores del Inicio según el rol: técnico (Pendientes, En revisión, Esperando validación) o supervisor/administrador. */
    @GET("api/inicio/indicadores")
    suspend fun indicadores(): Map<String, @JvmSuppressWildcards Any?>

    // ---- Empresa, ambiente, copias de seguridad y trazabilidad (doc 0.0.7 §6, §16, §24, §31) ----
    @GET("api/empresa")
    suspend fun empresa(): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.PUT("api/empresa")
    suspend fun empresaNombre(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.PUT("api/empresa/logo")
    suspend fun empresaLogo(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/empresa/restablecer")
    suspend fun empresaRestablecer(): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/empresa/produccion")
    suspend fun empresaProduccion(): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.PUT("api/empresa/copia-seguridad")
    suspend fun empresaCopia(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/copias/ahora")
    suspend fun copiaAhora(): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/copias")
    suspend fun copias(): List<Map<String, @JvmSuppressWildcards Any?>>

    @retrofit2.http.Streaming
    @GET("api/copias/{id}/descargar")
    suspend fun copiaDescargar(@Path("id") id: Long): okhttp3.ResponseBody

    @GET("api/bitacora")
    suspend fun bitacora(): List<Map<String, @JvmSuppressWildcards Any?>>

    /** Prompts frecuentes ★ del usuario (en su cuenta). */
    @GET("api/asistente/prompts")
    suspend fun prompts(): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.PUT("api/asistente/prompts")
    suspend fun guardarPrompts(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    /** Panel v2: un solo filtro general (hoy, ayer, 7, 14, mes, todo o rango). */
    @GET("api/panel/resumen")
    suspend fun panelResumen(@Query("periodo") periodo: String, @Query("desde") desde: String? = null, @Query("hasta") hasta: String? = null): Map<String, @JvmSuppressWildcards Any?>

    /** Foto de una minuta redactada con el Asistente IA (con el tipo de evidencia). */
    @retrofit2.http.Multipart
    @POST("api/minutas/{id}/fotos")
    suspend fun subirFoto(@Path("id") id: Long, @retrofit2.http.Part("tipo") tipo: okhttp3.RequestBody, @retrofit2.http.Part archivo: okhttp3.MultipartBody.Part): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.Streaming
    @GET("api/minutas/{id}/fotos/{archivo}")
    suspend fun foto(@Path("id") id: Long, @Path("archivo") archivo: String): okhttp3.ResponseBody

    @POST("api/asistente/programados/{id}/cancelar")
    suspend fun cancelarProgramado(@Path("id") id: Long): Map<String, @JvmSuppressWildcards Any?>

    // ---- Cambio de contraseña con aprobación del administrador (doc 0.0.7 §36) ----
    @POST("api/auth/recuperacion")
    suspend fun recuperacionSolicitar(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/auth/recuperacion/{id}/respuesta")
    suspend fun recuperacionRespuesta(@Path("id") id: Long, @Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/recuperaciones")
    suspend fun recuperaciones(): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("api/recuperaciones/{id}/codigo")
    suspend fun recuperacionCodigo(@Path("id") id: Long): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/recuperaciones/{id}/rechazar")
    suspend fun recuperacionRechazar(@Path("id") id: Long): Map<String, @JvmSuppressWildcards Any?>

    // ---- Acceso temporal a minutas de otros (doc 0.0.7 §46) ----
    @GET("api/accesos/candidatas")
    suspend fun accesosCandidatas(): List<Map<String, @JvmSuppressWildcards Any?>>

    @POST("api/accesos")
    suspend fun accesoPedir(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/accesos/{id}/generar")
    suspend fun accesoGenerar(@Path("id") id: Long): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/accesos/{id}/rechazar")
    suspend fun accesoRechazar(@Path("id") id: Long): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/accesos/canjear")
    suspend fun accesoCanjear(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/accesos/mios")
    suspend fun accesosMios(): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("api/accesos/{id}/minuta")
    suspend fun accesoMinuta(@Path("id") id: Long): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/panel")
    suspend fun panel(@retrofit2.http.Query("dias") dias: Int): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/equipo")
    suspend fun equipo(): List<Map<String, @JvmSuppressWildcards Any?>>

    @retrofit2.http.PUT("api/equipo/{id}/rol")
    suspend fun equipoRol(@retrofit2.http.Path("id") id: Long, @Body body: Map<String, String>): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.PUT("api/equipo/{id}/supervisor")
    suspend fun equipoSupervisor(@retrofit2.http.Path("id") id: Long, @Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/reglas")
    suspend fun reglas(): List<Map<String, @JvmSuppressWildcards Any?>>

    @POST("api/reglas")
    suspend fun reglaCrear(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.PUT("api/reglas/{id}")
    suspend fun reglaEditar(@retrofit2.http.Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.DELETE("api/reglas/{id}")
    suspend fun reglaEliminar(@retrofit2.http.Path("id") id: String): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/preguntas")
    suspend fun preguntasAgregadas(): List<Map<String, @JvmSuppressWildcards Any?>>

    @GET("api/aprendizaje/hoy")
    suspend fun aprendizajeHoy(): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/aprendizaje/aplicar")
    suspend fun aprendizajeAplicar(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/conocimiento/fichas")
    suspend fun fichas(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    // ---- 0.0.13 ----
    /** Bitácora de la minuta: resumen, quiénes la editaron en las últimas 48 h e historial. */
    @GET("api/minutas/{id}/bitacora")
    suspend fun bitacoraMinuta(@Path("id") id: Long): Map<String, @JvmSuppressWildcards Any?>

    /** El supervisor o administrador empieza a editar (el técnico ve «En edición por …») o cancela. */
    @POST("api/minutas/{id}/bloqueo")
    suspend fun bloquear(@Path("id") id: Long): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.DELETE("api/minutas/{id}/bloqueo")
    suspend fun liberar(@Path("id") id: Long): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/panel/plazos")
    suspend fun plazos(): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.PUT("api/panel/plazos")
    suspend fun plazosGuardar(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/panel/tiempos")
    suspend fun tiempos(@Query("mes") mes: String? = null): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/flujo/catalogo")
    suspend fun catalogoFlujo(): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.PUT("api/preguntas/{id}/mover")
    suspend fun moverPregunta(@Path("id") id: String, @Body body: Map<String, String>): List<Map<String, @JvmSuppressWildcards Any?>>

    @retrofit2.http.Multipart
    @retrofit2.http.PUT("api/profile/foto")
    suspend fun fotoPerfilSubir(@retrofit2.http.Part file: okhttp3.MultipartBody.Part): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/profile/foto")
    suspend fun fotoPerfil(): retrofit2.Response<okhttp3.ResponseBody>

    @retrofit2.http.DELETE("api/profile/foto")
    suspend fun fotoPerfilQuitar(): Map<String, @JvmSuppressWildcards Any?>

    // ---- 0.0.15 ----
    @retrofit2.http.PUT("api/minutas/{id}/topologia")
    suspend fun topologiaEstado(@Path("id") id: Long, @Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/minutas/{id}/eventos")
    suspend fun eventoMinuta(@Path("id") id: Long, @Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/evaluations/{id}/levantamiento")
    suspend fun levantamiento(@Path("id") id: Long): Map<String, @JvmSuppressWildcards Any?>

    @POST("api/asistente/sugerencia")
    suspend fun sugerencia(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.POST("api/auth/change-password")
    suspend fun cambiarClave(@Body body: Map<String, String>): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/notifications/avisos")
    suspend fun avisos(): Map<String, @JvmSuppressWildcards Any?>

    @retrofit2.http.PUT("api/notifications/avisos")
    suspend fun avisosGuardar(@Body body: Map<String, @JvmSuppressWildcards Any?>): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/minutas/archivadas")
    suspend fun archivadas(): List<Map<String, @JvmSuppressWildcards Any?>>

    @POST("api/minutas/{id}/restaurar")
    suspend fun restaurar(@Path("id") id: Long): Map<String, @JvmSuppressWildcards Any?>
}

data class ReportProfileDto(val all: Boolean = false, val tecs: String = "none", val own: Boolean = true)

data class ReportesDto(
    val pestanas: List<String> = emptyList(),
    val vista: String? = null,
    val indicadores: IndicadoresDto? = null,
    val minutas: List<FilaReporteDto> = emptyList(),
    val total: Int = 0,
    val tecnicos: List<TecnicoReporteDto> = emptyList(),
    val plazosEmpresa: Boolean = false,
    /** Evaluaciones sin terminar (Borrador · sin minuta todavía). */
    val borradores: List<Map<String, Any?>> = emptyList()
)

data class IndicadoresDto(
    val completadas: Int = 0,
    val total: Int = 0,
    val aTiempoPct: Long = 0,
    val tiempoPromedioMin: Long? = null,
    val enRiesgo: Long = 0
)

data class FilaReporteDto(
    val id: Long,
    val fecha: String? = null,
    val tecnico: String? = null,
    val supervisor: String? = null,
    val local: String? = null,
    val rubro: String? = null,
    val estado: String? = null,
    val criticidad: String? = null,
    val puntaje: Int? = null,
    val esfuerzo: String? = null,
    val plazo: String? = null,
    val minutos: Int? = null,
    val tipo: String? = null,
    val evaluacionId: Long? = null
)

data class TecnicoReporteDto(
    val tecnico: String,
    val supervisor: String? = null,
    val meta: Int = 10,
    val completadas: Int = 0,
    val total: Int = 0,
    val aTiempoPct: Long = 0,
    val tiempoPromedioMin: Long? = null,
    val enMora: Long = 0,
    val devueltas: Long = 0
)
