package com.upc.asistenteredidbi.presentation.asistente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.upc.asistenteredidbi.data.repository.Asistente007Repository
import com.upc.asistenteredidbi.data.repository.Asistente007Repository.Filtros
import com.upc.asistenteredidbi.data.session.SessionManager
import com.upc.asistenteredidbi.domain.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Opción que el usuario toca debajo de un mensaje del Asistente IA. */
data class Opcion(val id: String, val label: String, val principal: Boolean = false)

data class Mensaje(
    val deUsuario: Boolean,
    val texto: String,
    val opciones: List<Opcion> = emptyList(),
    /** Documento de minuta de soporte (JSON de bloques) para la vista previa. */
    val doc: String? = null,
    /** Tarjetas que devuelve el servidor (borrador, actualizar minuta, consulta, pendientes, reportes…). */
    val bloques: List<Map<String, Any?>> = emptyList(),
    /** Evidencias que el usuario adjuntó con este mensaje (nombre del tipo). */
    val adjuntos: List<String> = emptyList(),
    /** Tarjeta de progreso «Redactando la minuta». */
    val progreso: Boolean = false
)

/** Evidencia elegida en «Adjuntar evidencias»: tipo (Frontis del local, Speedtest…) y la foto. */
data class Adjunto(val tipo: String, val uri: String)

data class AsistenteUiState(
    val mensajes: List<Mensaje> = emptyList(),
    val cargando: Boolean = false,
    val admin: Boolean = false,
    val pista: String = "Escribe o dicta…",
    val adjuntos: List<Adjunto> = emptyList(),
    val subtitulo: String = "En línea"
)

sealed interface AsistenteEvento {
    /** «Implementación»: vuelve al inicio y abre una evaluación nueva con el flujo de siempre. */
    data object NuevaImplementacion : AsistenteEvento
    data class AbrirPdf(val base64: String, val nombre: String) : AsistenteEvento
    data class AbrirMinuta(val id: Long) : AsistenteEvento
    data class GuardarXlsx(val bytes: ByteArray, val nombre: String) : AsistenteEvento
    data object IrReportes : AsistenteEvento
    data class PrefijarTexto(val texto: String) : AsistenteEvento
}

/**
 * Asistente IA 0.0.7: «Nueva minuta» (Implementación o Soporte / incidencia), flujo de la minuta de soporte, actualizar una
 * minuta con el chat (nueva versión + historial + validación del supervisor), «¿Qué aprendiste hoy?» del administrador y
 * preguntas libres (ChatGPT en la API, con las reglas de la empresa como contexto).
 */
@HiltViewModel
class AsistenteViewModel @Inject constructor(
    private val repo: Asistente007Repository,
    private val session: SessionManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appCtx: android.content.Context
) : ViewModel() {

    private enum class Paso { LIBRE, S_CLIENTE, S_MOTIVO, S_ALCANCE, S_EVID, S_HECHO, S_REGLAS, S_ESTADO, U_ELEGIR, U_TEXTO, U_CONFIRMAR, L_CASOS }

    private data class Soporte(
        var cliente: String = "", var antecedentes: List<Map<String, Any?>> = emptyList(), var motivo: String = "",
        var puntos: List<String> = emptyList(), var evidencias: List<String> = emptyList(), val hechos: MutableList<String> = mutableListOf(),
        var reglas: MutableMap<String, String> = mutableMapOf(), var resultados: Map<String, Any?> = emptyMap(), var estadoSugerido: String = "OP",
        var evidLeidas: List<Any?> = emptyList(), var credenciales: List<Any?> = emptyList()
    )

    private val _state = MutableStateFlow(AsistenteUiState())
    val state: StateFlow<AsistenteUiState> = _state.asStateFlow()
    private val _eventos = Channel<AsistenteEvento>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    private var paso = Paso.LIBRE
    private var sop = Soporte()
    private var minutaId: Long? = null
    private var plan: List<Any?> = emptyList()
    private var casos: List<Any?> = emptyList()
    private var opcionesMinutas: Map<String, Long> = emptyMap()

    init {
        viewModelScope.launch {
            val rol = session.getRole()
            _state.update { it.copy(admin = rol == Role.ADMINISTRADOR) }
            rolActual = rol
            bienvenida()
        }
    }

    private var rolActual: Role? = null

    /** Un solo mensaje, sin botones: explica por escrito en qué ayuda el asistente según el rol (doc 01 §2.1). */
    private fun bienvenida() {
        val tecnico = rolActual == Role.TECNICO || rolActual == null
        val texto = if (tecnico) "Hola. Soy tu Asistente IA y puedo ayudarte a:\n" +
            "• Redactar una minuta nueva con tus fotos y lo que me cuentes por texto o audio (datos del cliente, cómo están conectados los equipos de red y lo que encontraste).\n" +
            "• Actualizar una minuta ya generada: dime el cliente, la fecha y qué cambió.\n" +
            "• Consultar una minuta: fecha de visita, observaciones, recomendaciones e historial.\n" +
            "• Decirte tus pendientes del día, tus minutas por validar y darte reportes.\n" +
            "• Resolver dudas de la visita (router, selftest, AnyDesk…).\n" +
            "¿En qué te ayudo? Escríbeme o mándame un audio."
        else "Hola. Soy tu Asistente IA y puedo ayudarte con la gestión de tu equipo:\n" +
            "• Consultar cualquier minuta: fecha de visita, observaciones, recomendaciones e historial.\n" +
            "• Decirte las minutas por validar y los pendientes del día.\n" +
            "• Darte reportes de los técnicos por periodo (y descargarlos en Excel) o programarlos, por ejemplo cada fin de mes.\n" +
            "• Actualizar una minuta con lo que me cuentes por texto o audio.\n" +
            (if (rolActual == Role.ADMINISTRADOR) "• Contarte qué aprendí de las visitas y guardarlo como regla nueva si me lo pides.\n" else "") +
            "¿En qué te ayudo? Escríbeme o mándame un audio."
        _state.update { it.copy(mensajes = listOf(Mensaje(false, texto))) }
    }

    /** Sin botones de opciones: el asistente solo se maneja escribiendo o dictando. */
    private fun inicio(@Suppress("UNUSED_PARAMETER") admin: Boolean): List<Opcion> = emptyList()

    /** Prompts frecuentes de la cuenta: se leen al abrir y se guardan al cambiarlos. */
    suspend fun cargarPrompts(): List<String>? = runCatching { repo.prompts() }.getOrNull()
    fun guardarPrompts(l: List<String>) { viewModelScope.launch { runCatching { repo.guardarPrompts(l) } } }

    /** «Pedir un cambio al Asistente IA» desde una minuta: se abre con la minuta cargada («Actualizando: local · vN»). */
    fun prepararActualizacion(id: Long, local: String) {
        paso = Paso.LIBRE; modo = "upd"; minutaId = id
        _state.update { it.copy(subtitulo = "Actualizando: $local", pista = "¿Qué cambió? Cuéntalo por texto o audio") }
        bot("Vas a actualizar la minuta de $local. Cuéntame qué cambió (datos, observaciones, recomendaciones, pasos o conexiones) y te muestro «Cambios que haré» antes de aplicar.")
    }

    fun reiniciar() {
        paso = Paso.LIBRE; modo = ""; tipo = ""; contexto.setLength(0); adjuntosAcumulados.clear(); fotosPorSubir.clear(); minutaId = null; sop = Soporte(); consultaReporte = null
        _state.update { it.copy(adjuntos = emptyList(), pista = "Escribe o dicta…", subtitulo = "En línea") }
        bienvenida()
    }

    // ---------------- Adjuntar evidencias ----------------
    fun agregarAdjunto(tipo: String, uri: String) = _state.update { it.copy(adjuntos = it.adjuntos + Adjunto(tipo, uri)) }
    fun quitarAdjunto(i: Int) = _state.update { it.copy(adjuntos = it.adjuntos.filterIndexed { n, _ -> n != i }) }

    private fun bot(t: String, op: List<Opcion> = emptyList(), doc: String? = null) =
        _state.update { it.copy(mensajes = it.mensajes + Mensaje(false, t, op, doc)) }

    private fun yo(t: String) = _state.update { it.copy(mensajes = it.mensajes + Mensaje(true, t)) }

    private fun pista(p: String) = _state.update { it.copy(pista = p) }

    private fun ocupado(b: Boolean) = _state.update { it.copy(cargando = b) }

    // ---------------- Entradas ----------------

    fun opcion(o: Opcion) {
        yo(o.label)
        when {
            o.id == "nueva" -> bot("¿Qué minuta vas a redactar?", listOf(Opcion("imp", "Implementación", true), Opcion("sop", "Soporte / incidencia")))
            o.id == "imp" -> { _eventos.trySend(AsistenteEvento.NuevaImplementacion) }
            o.id == "sop" -> empezarSoporte()
            o.id == "actualizar" -> elegirMinuta()
            o.id == "aprendiste" -> aprendizaje()
            o.id.startsWith("min:") -> { minutaId = opcionesMinutas[o.id]; paso = Paso.U_TEXTO; pista("Ej.: el router quedó en el rack, la clave del WiFi es …")
                bot("Escribe qué cambió en la visita. Ubico cada dato en su sección; si algo cambia el criterio de todas las minutas, te lo indico.") }
            o.id == "u_aplicar" -> aplicarPlan()
            o.id == "u_cancelar" -> { paso = Paso.LIBRE; bot("Listo, no se cambió la minuta.", inicio(_state.value.admin)) }
            o.id == "s_sin_evid" -> { sop.evidencias = emptyList(); siguienteHecho(0) }
            o.id.startsWith("r:") -> alternarRegla(o.id.removePrefix("r:"))
            o.id == "s_reglas_ok" -> pedirEstado()
            o.id.startsWith("e:") -> crearSoporte(o.id.removePrefix("e:"))
            o.id == "l_procede" -> aplicarAprendizaje(null)
            o.id == "l_reglas" -> aplicarAprendizaje("reglas")
            o.id == "l_ficha" -> ficha()
            o.id == "menu" -> { paso = Paso.LIBRE; bot("¿Qué más necesitas?", inicio(_state.value.admin)) }
        }
    }

    fun enviar(texto: String) {
        val t = texto.trim()
        val adj = _state.value.adjuntos
        if (t.isEmpty() && adj.isEmpty()) return
        if (paso == Paso.LIBRE && adj.isNotEmpty()) {
            _state.update { it.copy(adjuntos = emptyList(), mensajes = it.mensajes + Mensaje(true, t, adjuntos = adj.map { a -> a.tipo })) }
            adjuntosAcumulados.addAll(adj.map { it.tipo })
            fotosPorSubir.addAll(adj)
            libre(t)
            return
        }
        yo(t)
        when (paso) {
            Paso.S_CLIENTE -> cliente(t)
            Paso.S_MOTIVO -> { sop.motivo = t; paso = Paso.S_ALCANCE; pista("Un punto por línea")
                bot("¿Qué abarca la visita? Escribe un punto por línea (por ejemplo: «Impresora de cocina sin imprimir», «Revisar el WiFi del salón»).") }
            Paso.S_ALCANCE -> { sop.puntos = t.lines().map { it.trim().trimStart('-', '•', ' ') }.filter { it.isNotEmpty() }; paso = Paso.S_EVID; pista("Nombres de las fotos, separados por coma")
                bot("Adjunta o nombra las evidencias (selftest, router, repetidor…). Las leo para completar IP, conexión y respaldo.", listOf(Opcion("s_sin_evid", "Sin evidencias"))) }
            Paso.S_EVID -> { sop.evidencias = t.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }; siguienteHecho(0) }
            Paso.S_HECHO -> { sop.hechos.add(t); siguienteHecho(sop.hechos.size) }
            Paso.U_TEXTO -> planificar(t)
            else -> libre(t)
        }
    }

    // ---------------- Mensaje libre ----------------

    private var modo = ""
    private var tipo = ""
    private val contexto = StringBuilder()
    private val adjuntosAcumulados = linkedSetOf<String>()
    private var nuevaVisita = false
    /** Fotos adjuntadas a la minuta que se está redactando: se suben a la minuta al tocar «Redactar minuta». */
    private val fotosPorSubir = mutableListOf<Adjunto>()
    /** Última consulta de reporte: permite ajustarla («solo el mes anterior», «sin octubre», «de María») sin repetir todo. */
    private var consultaReporte: Map<String, Any?>? = null
    var minutaAbierta: Long? = null
        private set

    fun setNuevaVisita(v: Boolean) { nuevaVisita = v }

    /** Manda el mensaje al servidor, que decide la intención y responde con tarjetas (ver doc 01 §2.2). */
    private fun libre(t: String, conTipo: String? = null, textoOriginal: String? = null) = viewModelScope.launch {
        ocupado(true)
        val enNueva = modo == "new"
        if (enNueva && conTipo == null) {
            if (contexto.isNotEmpty()) { if (!contexto.trimEnd().endsWith('.') && !contexto.trimEnd().endsWith(';')) contexto.append('.'); contexto.append(' ') }
            contexto.append(t)
        }
        val texto = when {
            textoOriginal != null -> textoOriginal
            enNueva -> contexto.toString()
            else -> t
        }
        val body = buildMap<String, Any?> {
            put("texto", texto)
            if (modo.isNotEmpty() || conTipo != null) put("modo", if (conTipo != null) "new" else modo)
            (conTipo ?: tipo).takeIf { it.isNotEmpty() }?.let { put("tipo", it) }
            minutaId?.let { if (modo == "upd") put("minutaId", it) }
            if (adjuntosAcumulados.isNotEmpty()) put("adjuntos", adjuntosAcumulados.toList())
            consultaReporte?.let { put("reporte", it) }
        }
        val r = runCatching { repo.mensaje(body) }.getOrNull()
        ocupado(false)
        if (r == null) { bot("No pude conectarme con el Asistente IA. Revisa tu conexión e inténtalo de nuevo."); return@launch }
        if (r["casos"] != null) { mostrarAprendizaje(r); return@launch }
        @Suppress("UNCHECKED_CAST")
        val estado = r["estado"] as? Map<String, Any?>
        when (estado?.get("modo")) {
            "soporte" -> { paso = Paso.S_CLIENTE; pista("Cliente o local"); modo = "" }
            "upd" -> { modo = "upd"; minutaId = (estado["minutaId"] as? Number)?.toLong(); pista("Cuéntame qué cambió en la visita") }
            "new" -> { modo = "new"; tipo = estado["tipo"]?.toString() ?: "imp"
                if (contexto.isEmpty()) contexto.append(texto); _state.update { it.copy(subtitulo = "Redactando una minuta nueva") } }
        }
        @Suppress("UNCHECKED_CAST")
        val bloques = (r["bloques"] as? List<Map<String, Any?>>).orEmpty()
        if (bloques.isEmpty()) { bot(r["texto"]?.toString() ?: "Sin respuesta."); return@launch }
        // Si la tarjeta es «Tipo», guarda el texto para reenviarlo cuando elijan.
        bloques.firstOrNull { it["tipo"] == "actualizar_minuta" }?.let { b ->
            minutaId = (b["minutaId"] as? Number)?.toLong(); modo = "upd"; nuevaVisita = b["nuevaVisita"] == true
        }
        // Un reporte deja su consulta para ajustarla; cualquier otra tarjeta (que no sea texto) la cierra.
        val rep = bloques.firstOrNull { it["tipo"] == "reporte" }
        @Suppress("UNCHECKED_CAST")
        if (rep != null) consultaReporte = rep["consulta"] as? Map<String, Any?>
        else if (bloques.any { it["tipo"] !in setOf("texto", "texto_ia") }) consultaReporte = null
        _state.update { it.copy(mensajes = it.mensajes + Mensaje(false, "", bloques = bloques)) }
        bloques.firstOrNull { it["tipo"] == "accion" }?.let { b ->
            if (b["accion"] == "abrir_minuta") { (b["minutaId"] as? Number)?.toLong()?.let { id -> _eventos.trySend(AsistenteEvento.AbrirMinuta(id)) } }
        }
    }

    /** Un botón de una tarjeta: confirmar, agregar más, elegir tipo, abrir una minuta, descargar el reporte… */
    fun accion(bloque: Map<String, Any?>, a: Map<String, Any?>) {
        @Suppress("UNCHECKED_CAST")
        val datos = (a["datos"] as? Map<String, Any?>).orEmpty()
        when (a["id"]) {
            "tipo_imp", "tipo_sop" -> {
                yo(a["label"].toString())
                val orig = datos["texto"]?.toString().orEmpty()
                if (a["id"] == "tipo_sop") { modo = ""; libre("Soporte / incidencia", textoOriginal = "Redacta una minuta de soporte") }
                else { contexto.setLength(0); modo = "new"; contexto.append(orig); libre(orig, conTipo = "imp", textoOriginal = orig) }
            }
            "agregar" -> { pista("Agrega más contexto o adjunta más evidencias"); bot("Listo, cuéntame más o adjunta más evidencias con 📎. Cuando termines, toca «Redactar minuta» en el borrador.") }
            "redactar" -> redactar(bloque["datos"])
            "cancelar" -> { modo = ""; minutaId = null; bot("Listo, no se cambió la minuta.") }
            "aplicar" -> aplicarCambiosDeTarjeta(bloque)
            "abrir_minuta" -> (datos["minutaId"] as? Number)?.toLong()?.let { _eventos.trySend(AsistenteEvento.AbrirMinuta(it)) }
            "texto" -> datos["texto"]?.toString()?.let { _eventos.trySend(AsistenteEvento.PrefijarTexto(it)) }
            "descargar_reporte" -> @Suppress("UNCHECKED_CAST") descargar((datos["consulta"] as? Map<String, Any?>).orEmpty())
            "cancelar_programado" -> viewModelScope.launch {
                (datos["id"] as? Number)?.toLong()?.let { id ->
                    runCatching { repo.cancelarProgramado(id) }.onSuccess { bot("Listo, ya no te enviaré ese reporte.") }.onFailure { bot("No pude cancelar el envío. Inténtalo de nuevo.") }
                }
            }
            "ver_reportes" -> _eventos.trySend(AsistenteEvento.IrReportes)
        }
    }

    private fun reducir(uri: String): ByteArray {
        val bytes = appCtx.contentResolver.openInputStream(android.net.Uri.parse(uri))!!.use { it.readBytes() }
        val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
        val k = 1600f / maxOf(bmp.width, bmp.height).coerceAtLeast(1)
        val out = if (k < 1f) android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt(), (bmp.height * k).toInt(), true) else bmp
        return java.io.ByteArrayOutputStream().also { out.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, it) }.toByteArray()
    }

    suspend fun foto(minutaId: Long, archivo: String): ByteArray = repo.foto(minutaId, archivo)

    fun abrirMinuta(id: Long) { _eventos.trySend(AsistenteEvento.AbrirMinuta(id)) }

    suspend fun cargarMinuta(id: Long): Map<String, Any?> = repo.minuta(id)

    private fun redactar(datos: Any?) = viewModelScope.launch {
        _state.update { it.copy(mensajes = it.mensajes + Mensaje(false, "", progreso = true)) }
        ocupado(true)
        val r = runCatching { repo.redactar(datos) }.getOrNull()
        ocupado(false)
        if (r == null) { bot("No pude redactar la minuta. Revisa tu conexión e inténtalo de nuevo."); return@launch }
        @Suppress("UNCHECKED_CAST")
        val bloques = (r["bloques"] as? List<Map<String, Any?>>).orEmpty()
        modo = ""; tipo = ""; contexto.setLength(0); adjuntosAcumulados.clear()
        _state.update { it.copy(subtitulo = "En línea", mensajes = it.mensajes + Mensaje(false, "", bloques = bloques.filter { b -> b["tipo"] != "accion" })) }
        val id = bloques.firstOrNull { it["tipo"] == "accion" }?.let { b -> (b["minutaId"] as? Number)?.toLong() }
        if (id != null) {
            // Las fotos adjuntas pasan a ser evidencias de la minuta (se reducen a 1600 px para el celular).
            val fotos = fotosPorSubir.toList(); fotosPorSubir.clear()
            var fallaron = 0
            fotos.forEach { f -> runCatching { repo.subirFoto(id, f.tipo, reducir(f.uri)) }.onFailure { fallaron++ } }
            if (fallaron > 0) bot("No pude subir $fallaron foto${if (fallaron > 1) "s" else ""}. Quedan como «Evidencias por subir» en la minuta.")
            _eventos.trySend(AsistenteEvento.AbrirMinuta(id))
        }
    }

    private fun aplicarCambiosDeTarjeta(bloque: Map<String, Any?>) = viewModelScope.launch {
        val id = (bloque["minutaId"] as? Number)?.toLong() ?: return@launch
        @Suppress("UNCHECKED_CAST")
        val items = (bloque["items"] as? List<Any?>).orEmpty()
        yo("Aplicar y ver minuta")
        ocupado(true)
        val r = runCatching { repo.aplicar(id, items, nuevaVisita) }
        ocupado(false)
        modo = ""; minutaId = null
        r.onSuccess { m ->
            bot("Listo. La minuta quedó en la v${(m["version"] as? Number)?.toInt() ?: ""} y vuelve a validación del supervisor. Te llevo a Ver minuta.")
            _eventos.trySend(AsistenteEvento.AbrirMinuta(id))
        }.onFailure { bot("No pude aplicar los cambios. Inténtalo de nuevo.") }
    }

    private fun descargar(consulta: Map<String, Any?>) = viewModelScope.launch {
        ocupado(true)
        val bytes = runCatching { repo.reporteXlsx(consulta) }.getOrNull()
        ocupado(false)
        if (bytes == null) bot("No pude generar el Excel. Inténtalo de nuevo.")
        else _eventos.trySend(AsistenteEvento.GuardarXlsx(bytes, "reporte-minutas-${java.time.LocalDate.now()}.xlsx"))
    }

    // ---------------- Minuta de soporte ----------------

    private fun empezarSoporte() {
        sop = Soporte(); paso = Paso.S_CLIENTE; pista("Cliente o local")
        bot("¿De qué cliente es la visita de soporte? Escribe el nombre del local o la razón social.")
    }

    private fun cliente(t: String) = viewModelScope.launch {
        sop.cliente = t
        ocupado(true)
        sop.antecedentes = runCatching { repo.antecedentes(t) }.getOrDefault(emptyList())
        ocupado(false)
        val ant = if (sop.antecedentes.isEmpty()) "No hay visitas anteriores registradas de $t."
        else "Antecedentes de $t:\n" + sop.antecedentes.take(5).joinToString("\n") { "• ${it["fecha"] ?: ""} · ${if (it["tipo"] == "soporte") "Soporte" else "Implementación"}${(it["texto"] as? String)?.takeIf { t -> t.isNotBlank() }?.let { t -> " · $t" } ?: ""}" }
        paso = Paso.S_MOTIVO; pista("Motivo de la visita")
        bot("$ant\n\n¿Cuál es el motivo de la visita?")
    }

    private fun siguienteHecho(i: Int) {
        if (i < sop.puntos.size) {
            paso = Paso.S_HECHO; pista("Lo realizado en este punto")
            bot("${i + 1}. ${sop.puntos[i]}\n¿Qué se realizó? Incluye IP, conexión, respaldo y contraseñas si aplica; no pongas fechas.")
        } else evaluarReglas()
    }

    private fun evaluarReglas() = viewModelScope.launch {
        ocupado(true)
        val r = runCatching { repo.evaluarSoporte(sop.motivo, sop.hechos, sop.evidencias) }.getOrNull()
        ocupado(false)
        if (r == null) { bot("No pude revisar las reglas de soporte. Inténtalo de nuevo.", listOf(Opcion("s_reglas_ok", "Reintentar"))); return@launch }
        @Suppress("UNCHECKED_CAST")
        sop.reglas = ((r["reglas"] as? Map<String, Any?>) ?: emptyMap()).mapValues { it.value.toString() }.toMutableMap()
        @Suppress("UNCHECKED_CAST")
        sop.resultados = (r["resultados"] as? Map<String, Any?>) ?: emptyMap()
        sop.estadoSugerido = r["estadoSugerido"]?.toString() ?: "OP"
        sop.evidLeidas = (r["evidencias"] as? List<Any?>) ?: emptyList()
        sop.credenciales = (r["credenciales"] as? List<Any?>) ?: emptyList()
        paso = Paso.S_REGLAS
        mostrarReglas()
    }

    private val nombresReglas = linkedMapOf(
        "S1" to "Un solo sistema de impresiones activo", "S2" to "IP fijas y sin conflictos",
        "S3" to "Respaldo de cada impresora", "S4" to "Lugares de preparación con su impresora asignada"
    )

    private fun marca(v: String?) = when (v) { "ok" -> "✓ Cumple"; "bad" -> "✗ No cumple"; else -> "— No aplica" }

    private fun mostrarReglas() {
        val txt = nombresReglas.entries.joinToString("\n") { (k, n) -> "$k · $n: ${marca(sop.reglas[k])}" + (sop.resultados[k]?.let { "\n   $it" } ?: "") }
        bot("Revisé las reglas de soporte con lo que escribiste. Toca una regla para corregirla:\n\n$txt",
            nombresReglas.keys.map { Opcion("r:$it", "$it: ${marca(sop.reglas[it])}") } + Opcion("s_reglas_ok", "Continuar", true))
    }

    private fun alternarRegla(k: String) {
        sop.reglas[k] = when (sop.reglas[k]) { "ok" -> "bad"; "bad" -> "na"; else -> "ok" }
        if (sop.reglas.values.contains("bad")) sop.estadoSugerido = "OBS" else if (sop.estadoSugerido == "OBS") sop.estadoSugerido = "OP"
        mostrarReglas()
    }

    private fun pedirEstado() {
        if (paso != Paso.S_REGLAS) { evaluarReglas(); return }
        paso = Paso.S_ESTADO
        val sug = mapOf("OP" to "Operativo", "OPP" to "Operativo con pendiente", "OBS" to "Observado")
        bot("¿Cómo queda el local? Te sugiero «${sug[sop.estadoSugerido]}».", sug.map { (k, v) -> Opcion("e:$k", v, k == sop.estadoSugerido) })
    }

    private fun crearSoporte(estado: String) = viewModelScope.launch {
        ocupado(true)
        val body = mapOf(
            "cliente" to sop.cliente, "local" to sop.cliente, "antecedentes" to sop.antecedentes, "motivo" to sop.motivo,
            "puntos" to sop.puntos, "hechos" to sop.hechos, "evidencias" to sop.evidLeidas, "reglas" to sop.reglas,
            "estado" to estado, "credenciales" to sop.credenciales
        )
        val r = runCatching { repo.crearSoporte(body) }
        ocupado(false)
        r.onSuccess { m ->
            paso = Paso.LIBRE
            bot("Redacté la minuta de soporte de ${sop.cliente}. Quedó en Pendiente: revísala y solicita la validación del supervisor desde Minutas.",
                listOf(Opcion("menu", "Listo")), m["contentJson"] as? String)
        }.onFailure { bot("No pude guardar la minuta. Revisa tu conexión e inténtalo de nuevo.", listOf(Opcion("e:$estado", "Reintentar", true))) }
    }

    // ---------------- Actualizar una minuta ----------------

    private fun elegirMinuta() = viewModelScope.launch {
        ocupado(true)
        val d = runCatching { repo.reportes("mis", Filtros()) }.getOrNull()
        ocupado(false)
        val lista = d?.minutas.orEmpty().take(8)
        if (lista.isEmpty()) { bot("No encontré minutas tuyas para actualizar.", inicio(_state.value.admin)); return@launch }
        opcionesMinutas = lista.associate { "min:${it.id}" to it.id }
        paso = Paso.U_ELEGIR
        bot("¿Qué minuta quieres actualizar?", lista.map { Opcion("min:${it.id}", "${it.local ?: "Minuta ${it.id}"} · ${it.fecha ?: ""}") })
    }

    private fun planificar(t: String) = viewModelScope.launch {
        val id = minutaId ?: return@launch
        ocupado(true)
        val r = runCatching { repo.plan(id, t) }.getOrNull()
        ocupado(false)
        if (r == null) { bot("No pude leer el cambio. Inténtalo de nuevo."); return@launch }
        if (r["criterio"] == true) { bot(r["mensaje"]?.toString() ?: "Eso cambia el criterio de todas las minutas.", inicio(_state.value.admin)); paso = Paso.LIBRE; return@launch }
        plan = (r["items"] as? List<Any?>) ?: emptyList()
        if (plan.isEmpty()) { bot("No identifiqué datos para cambiar. Escríbelo de otra forma (por ejemplo, «la impresora de cocina quedó en el puerto 3»)."); return@launch }
        paso = Paso.U_CONFIRMAR
        val txt = plan.joinToString("\n") { "• " + ((it as? Map<*, *>)?.get("t") ?: "") }
        bot("Esto cambiaría en la minuta:\n$txt\n\nSe guarda como una versión nueva, queda en el historial y vuelve a validación del supervisor.",
            listOf(Opcion("u_aplicar", "Aplicar cambios", true), Opcion("u_cancelar", "Cancelar")))
    }

    private fun aplicarPlan() = viewModelScope.launch {
        val id = minutaId ?: return@launch
        ocupado(true)
        val r = runCatching { repo.aplicar(id, plan) }
        ocupado(false)
        paso = Paso.LIBRE
        r.onSuccess { m -> bot("Listo. La minuta quedó en la versión ${(m["version"] as? Number)?.toInt() ?: ""} y se envió a validación del supervisor.", inicio(_state.value.admin)) }
            .onFailure { bot("No pude aplicar los cambios. Inténtalo de nuevo.", listOf(Opcion("u_aplicar", "Reintentar", true))) }
    }

    // ---------------- ¿Qué aprendiste hoy? (administrador) ----------------

    private fun aprendizaje() = viewModelScope.launch {
        ocupado(true)
        val r = runCatching { repo.aprendizajeHoy() }.getOrNull()
        ocupado(false)
        if (r == null) bot("No pude revisar lo aprendido. Inténtalo de nuevo.") else mostrarAprendizaje(r)
    }

    private fun mostrarAprendizaje(r: Map<String, Any?>) {
        casos = (r["casos"] as? List<Any?>) ?: emptyList()
        val v = r["visitas"] as? Map<*, *>
        val n = { k: String -> (v?.get(k) as? Number)?.toInt() ?: 0 }
        val sb = StringBuilder("Hoy revisé ${n("evaluacion")} evaluaciones, ${n("soporte")} visitas de soporte y ${n("informacion")} de información.")
        casos.forEachIndexed { i, c ->
            val m = c as? Map<*, *> ?: return@forEachIndexed
            sb.append("\n\n${i + 1}. ${m["tipo"]} · ${m["local"] ?: ""}\nCaso: ${m["caso"] ?: ""}\nAprendizaje: ${m["aprendizaje"] ?: ""}")
            (m["regla"] as? Map<*, *>)?.let { sb.append("\nRegla sugerida: ${it["id"]} · ${it["t"] ?: ""}" + if (m["agregada"] == true) " (ya agregada)" else "") }
            (m["pregunta"] as? Map<*, *>)?.let { sb.append("\nPregunta sugerida: ${it["texto"] ?: ""}") }
            m["cubierta"]?.let { sb.append("\nYa lo cubre: $it") }
        }
        r["sugerencia"]?.let { sb.append("\n\n$it") }
        paso = Paso.L_CASOS
        val pend = casos.any { (it as? Map<*, *>)?.let { m -> m["regla"] != null && m["agregada"] != true } == true }
        bot(sb.toString(), buildList {
            if (pend) { add(Opcion("l_procede", "Procede", true)); add(Opcion("l_reglas", "Solo las reglas")) }
            add(Opcion("l_ficha", "Guardar y generar ficha (PDF)", !pend))
        })
    }

    private fun aplicarAprendizaje(solo: String?) = viewModelScope.launch {
        ocupado(true)
        val r = runCatching { repo.aprendizajeAplicar(casos, solo) }.getOrNull()
        ocupado(false)
        if (r == null) { bot("No pude agregarlas. Inténtalo de nuevo."); return@launch }
        val nr = (r["reglas"] as? List<*>)?.size ?: 0
        val np = (r["preguntas"] as? List<*>)?.size ?: 0
        bot("Listo: agregué $nr ${if (nr == 1) "regla" else "reglas"} (activas) y $np ${if (np == 1) "pregunta" else "preguntas"} en Preguntas y reglas, como aprobadas por el administrador. ¿Guardo el conocimiento y genero la ficha?",
            listOf(Opcion("l_ficha", "Guardar y generar ficha (PDF)", true), Opcion("menu", "Ahora no")))
    }

    private fun ficha() = viewModelScope.launch {
        ocupado(true)
        val r = runCatching { repo.fichas(casos) }.getOrNull()
        ocupado(false)
        val b64 = r?.get("pdfBase64") as? String
        if (b64 == null) { bot("No pude generar la ficha. Inténtalo de nuevo."); return@launch }
        val n = (r["fichas"] as? List<*>)?.size ?: 0
        bot("Guardé $n ${if (n == 1) "ficha" else "fichas"} en la base de conocimiento. El Asistente IA ya las usa para responder.", listOf(Opcion("menu", "Listo")))
        _eventos.trySend(AsistenteEvento.AbrirPdf(b64, "ficha-aprendizaje.pdf"))
    }
}
