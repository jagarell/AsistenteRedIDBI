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

    /** Reporte del periodo en CSV (se abre en Excel). */
    @retrofit2.http.Streaming
    @GET("api/asistente/reporte.csv")
    suspend fun reporteCsv(@Query("desde") desde: String, @Query("hasta") hasta: String): okhttp3.ResponseBody

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
}

data class ReportProfileDto(val all: Boolean = false, val tecs: String = "none", val own: Boolean = true)

data class ReportesDto(
    val pestanas: List<String> = emptyList(),
    val vista: String? = null,
    val indicadores: IndicadoresDto? = null,
    val minutas: List<FilaReporteDto> = emptyList(),
    val total: Int = 0,
    val tecnicos: List<TecnicoReporteDto> = emptyList(),
    val plazosEmpresa: Boolean = false
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
