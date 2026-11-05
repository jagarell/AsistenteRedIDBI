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
    val doc: String? = null
)

data class AsistenteUiState(
    val mensajes: List<Mensaje> = emptyList(),
    val cargando: Boolean = false,
    val admin: Boolean = false,
    val pista: String = "Escribe al Asistente IA…"
)

sealed interface AsistenteEvento {
    /** «Implementación»: vuelve al inicio y abre una evaluación nueva con el flujo de siempre. */
    data object NuevaImplementacion : AsistenteEvento
    data class AbrirPdf(val base64: String, val nombre: String) : AsistenteEvento
}

/**
 * Asistente IA 0.0.7: «Nueva minuta» (Implementación o Soporte / incidencia), flujo de la minuta de soporte, actualizar una
 * minuta con el chat (nueva versión + historial + validación del supervisor), «¿Qué aprendiste hoy?» del administrador y
 * preguntas libres (ChatGPT en la API, con las reglas de la empresa como contexto).
 */
@HiltViewModel
class AsistenteViewModel @Inject constructor(
    private val repo: Asistente007Repository,
    private val session: SessionManager
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
            val admin = session.getRole() == Role.ADMINISTRADOR
            _state.update { it.copy(admin = admin) }
            bot("Hola, soy el Asistente IA. ¿Qué necesitas?", inicio(admin))
        }
    }

    private fun inicio(admin: Boolean) = buildList {
        add(Opcion("nueva", "Nueva minuta", true))
        add(Opcion("actualizar", "Actualizar una minuta"))
        if (admin) add(Opcion("aprendiste", "¿Qué aprendiste hoy?"))
    }

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
        if (t.isEmpty()) return
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

    private fun libre(t: String) = viewModelScope.launch {
        ocupado(true)
        val r = runCatching { repo.mensaje(t) }.getOrNull()
        ocupado(false)
        when {
            r == null -> bot("No pude conectarme con el Asistente IA. Revisa tu conexión e inténtalo de nuevo.")
            r["casos"] != null -> mostrarAprendizaje(r)
            r["tipo"] == "tipo_minuta" -> bot(r["texto"].toString(), listOf(Opcion("imp", "Implementación", true), Opcion("sop", "Soporte / incidencia")))
            r["tipo"] == "soporte" -> { paso = Paso.S_CLIENTE; pista("Cliente o local"); bot(r["texto"].toString()) }
            else -> bot(r["texto"]?.toString() ?: "Sin respuesta.")
        }
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
