package com.upc.asistenteredidbi.presentation.gestion

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.upc.asistenteredidbi.data.remote.toFriendlyMessage
import com.upc.asistenteredidbi.data.repository.Asistente007Repository
import com.upc.asistenteredidbi.presentation.reportes.Ui007
import com.upc.asistenteredidbi.presentation.reportes.Ui007.dp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Preguntas y reglas (administrador, doc 0.0.7 §11 y §48). Pestañas Preguntas › Reglas › Mapa de nodos.
 * Preguntas: todas las del Chat IA por sección, completas y numeradas (el catálogo sale del motor, así nunca queda desfasado); las que
 * se agregaron al flujo se pueden subir o bajar con ↑ ↓ y Deshacer. Las preguntas base se arman por código en el motor del Chat IA:
 * se ven, pero su orden no se cambia desde la app. Reglas: activar, editar, eliminar y crear; SISIMP guarda el sistema de impresiones.
 * Mapa de nodos: Inicio → secciones con sus preguntas en orden → Fin; las fichas de sección llevan directo a esa sección.
 */
@AndroidEntryPoint
class PreguntasReglasFragment : Fragment() {

    @Inject lateinit var repo: Asistente007Repository
    private lateinit var body: LinearLayout
    private lateinit var progress: ProgressBar
    private var tab = "Preguntas"
    private var catalogo: List<Map<String, Any?>> = emptyList()
    private var scroll: ScrollView? = null
    private var abierta: String? = null
    private var deshacer: Triple<String, String, String>? = null  // id, dirección inversa, aviso
    private var reglas: List<Map<String, Any?>> = emptyList()
    private var preguntas: List<Map<String, Any?>> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val frame = FrameLayout(ctx).apply { setBackgroundColor(Ui007.BG) }
        val scroll = ScrollView(ctx).apply { isFillViewport = true }
        this.scroll = scroll
        body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; val p = dp(ctx, 16); setPadding(p, p + dp(ctx, 28), p, dp(ctx, 32)) }
        scroll.addView(body)
        progress = ProgressBar(ctx).apply { layoutParams = FrameLayout.LayoutParams(dp(ctx, 40), dp(ctx, 40), android.view.Gravity.CENTER) }
        frame.addView(scroll); frame.addView(progress)
        return frame
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { load() }

    private fun load() {
        progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val r = runCatching { repo.reglas() to repo.preguntasAgregadas() }
            catalogo = runCatching { @Suppress("UNCHECKED_CAST") (repo.catalogoFlujo()["secciones"] as? List<Map<String, Any?>>).orEmpty() }.getOrDefault(emptyList())
            progress.visibility = View.GONE
            if (context == null) return@launch
            r.onSuccess { reglas = it.first; preguntas = it.second; render() }.onFailure {
                body.removeAllViews()
                body.addView(Ui007.text(requireContext(), "No se pudo cargar Preguntas y reglas.", 15f, Ui007.GREY))
            }
        }
    }

    private fun id(r: Map<String, Any?>) = r["id"].toString()

    private fun render() {
        val ctx = requireContext()
        body.removeAllViews()
        body.addView(Ui007.text(ctx, "Preguntas y reglas", 24f, Ui007.INK, true))
        val tabs = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 12), 0, 0) }
        val tabsScroll = android.widget.HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; addView(tabs) }
        listOf("Preguntas", "Reglas", "Mapa de nodos").forEach { t -> tabs.addView(Ui007.chip(ctx, t, t == tab) { tab = t; render() }) }
        body.addView(tabsScroll)
        when (tab) { "Reglas" -> renderReglas(); "Mapa de nodos" -> renderMapa(); else -> renderPreguntas() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun opciones(q: Map<String, Any?>): List<String> = (q["opciones"] as? List<*>)?.map { it.toString() }.orEmpty()

    /** Preguntas agregadas de una sección, en su orden. */
    private fun agregadasDe(seccion: String) = preguntas.filter { (it["seccionId"] as? String) == seccion }

    private fun renderPreguntas() {
        val ctx = requireContext()
        body.addView(Ui007.text(ctx, "Toca una pregunta para ver su detalle. Las agregadas se suben o bajan con ↑ ↓.", 13f, Ui007.GREY).apply { setPadding(0, dp(ctx, 10), 0, 0) })
        deshacer?.let { (id, dir, aviso) ->
            val barra = Ui007.row(ctx).apply { background = Ui007.round(0xFFE3ECFB.toInt(), 12, ctx); setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8)); (layoutParams as? LinearLayout.LayoutParams) }
            barra.addView(Ui007.text(ctx, aviso, 13f, Ui007.INK), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            barra.addView(Ui007.chip(ctx, "↶ Deshacer", true) { deshacer = null; mover(id, dir, guardarDeshacer = false) })
            body.addView(barra, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 10) })
        }
        if (catalogo.isEmpty()) body.addView(Ui007.card(ctx).apply { addView(Ui007.text(ctx, "No se pudo cargar el catálogo de preguntas del Chat IA. Revisa tu conexión.", 14f, Ui007.GREY)) })
        val conocidas = catalogo.map { it["id"].toString() }.toSet()
        catalogo.forEach { sec ->
            val sid = sec["id"].toString()
            body.addView(Ui007.text(ctx, "$sid · ${sec["nombre"]}", 14f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 16), 0, dp(ctx, 2)) })
            (sec["preguntas"] as? List<Map<String, Any?>>).orEmpty().forEach { body.addView(fichaBase(it)) }
            agregadasDe(sid).forEachIndexed { i, q -> body.addView(fichaAgregada(q, i, agregadasDe(sid).size)) }
        }
        val sueltas = preguntas.filter { (it["seccionId"] as? String) !in conocidas }
        if (sueltas.isNotEmpty()) {
            body.addView(Ui007.text(ctx, "Agregadas", 14f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 16), 0, dp(ctx, 2)) })
            sueltas.forEachIndexed { i, q -> body.addView(fichaAgregada(q, i, sueltas.size)) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fichaBase(q: Map<String, Any?>): View {
        val ctx = requireContext()
        val id = q["id"].toString()
        val c = Ui007.card(ctx)
        c.addView(Ui007.text(ctx, "${(q["orden"] as? Number)?.toInt() ?: ""}. ${q["texto"]}", 14.5f, Ui007.INK, true))
        val ops = opciones(q)
        c.addView(Ui007.text(ctx, listOfNotNull(id, q["tipo"]?.toString(), if (ops.isNotEmpty()) "${ops.size} opciones" else null, if (q["opcional"] == true) "opcional" else null).joinToString(" · "), 12f, Ui007.GREY))
        if (abierta == id) {
            if (ops.isNotEmpty()) c.addView(Ui007.text(ctx, "Opciones: " + ops.joinToString(" · "), 12.5f, Ui007.INK).apply { setPadding(0, dp(ctx, 6), 0, 0) })
            if (q["evidencia"] == true) c.addView(Ui007.text(ctx, "Es una evidencia: el técnico sube la foto o captura.", 12.5f, Ui007.GREY).apply { setPadding(0, dp(ctx, 4), 0, 0) })
            c.addView(Ui007.text(ctx, "Esta pregunta base se arma en el motor del Chat IA según lo respondido antes; su texto y orden los cambia el equipo de desarrollo.", 11.5f, Ui007.GREY).apply { setPadding(0, dp(ctx, 6), 0, 0) })
        }
        c.setOnClickListener { abierta = if (abierta == id) null else id; render() }
        return c
    }

    private fun fichaAgregada(q: Map<String, Any?>, i: Int, n: Int): View {
        val ctx = requireContext()
        val id = id(q)
        val c = Ui007.card(ctx)
        val fila = Ui007.row(ctx)
        val textos = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        textos.addView(Ui007.text(ctx, q["texto"].toString(), 14.5f, Ui007.INK, true))
        textos.addView(Ui007.text(ctx, listOfNotNull(id, "agregada", q["tipo"]?.toString()).joinToString(" · "), 12f, Ui007.GREY))
        fila.addView(textos, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        fun flecha(t: String, desc: String, habilitada: Boolean, dir: String) = Ui007.text(ctx, t, 18f, Ui007.BLUE, true).apply {
            contentDescription = desc; alpha = if (habilitada) 1f else 0.25f; setPadding(dp(ctx, 12), dp(ctx, 6), dp(ctx, 12), dp(ctx, 6))
            if (habilitada) setOnClickListener { mover(id, dir) }
        }
        fila.addView(flecha("↑", "Subir $id", i > 0, "arriba"))
        fila.addView(flecha("↓", "Bajar $id", i < n - 1, "abajo"))
        c.addView(fila)
        (q["cuando"] as? String)?.takeIf { it.isNotBlank() }?.let { c.addView(Ui007.text(ctx, "Cuándo: $it", 12f, Ui007.GREY)) }
        opciones(q).let { if (it.isNotEmpty()) c.addView(Ui007.text(ctx, "Opciones: ${it.joinToString(" · ")}", 12f, Ui007.GREY)) }
        return c
    }

    /** Sube o baja una pregunta agregada; se guarda para Deshacer y queda en la bitácora de la empresa. */
    private fun mover(id: String, dir: String, guardarDeshacer: Boolean = true) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { repo.moverPregunta(id, dir) }
                .onSuccess { lista ->
                    preguntas = lista
                    val q = lista.firstOrNull { id(it) == id }
                    val pos = (q?.let { agregadasDe(it["seccionId"].toString()).indexOfFirst { x -> id(x) == id } } ?: 0) + 1
                    deshacer = if (guardarDeshacer) Triple(id, if (dir == "arriba") "abajo" else "arriba", "$id ahora va en ${q?.get("seccionId") ?: ""} · posición $pos") else null
                    render()
                }
                .onFailure { Toast.makeText(requireContext(), it.toFriendlyMessage(), Toast.LENGTH_LONG).show() }
        }
    }

    /** Mapa de nodos: Inicio → cada sección con sus preguntas en orden (ícono del tipo, código, tipo y texto) → Fin. */
    @Suppress("UNCHECKED_CAST")
    private fun renderMapa() {
        val ctx = requireContext()
        body.addView(Ui007.text(ctx, "Toca una sección para ir a ella y una pregunta para ver su detalle.", 13f, Ui007.GREY).apply { setPadding(0, dp(ctx, 10), 0, dp(ctx, 4)) })
        if (catalogo.isEmpty()) { body.addView(Ui007.card(ctx).apply { addView(Ui007.text(ctx, "No se pudo cargar el mapa. Revisa tu conexión.", 14f, Ui007.GREY)) }); return }
        val secciones = LinkedHashMap<String, View>()
        val fichas = Ui007.row(ctx)
        val barra = android.widget.HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; addView(fichas) }
        body.addView(Ui007.pill(ctx, "▶ Inicio", 0xFFDDF3E6.toInt(), 0xFF16663A.toInt()).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(ctx, 8) })
        catalogo.forEach { sec ->
            val sid = sec["id"].toString()
            val bloque = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            bloque.addView(Ui007.text(ctx, "$sid · ${sec["nombre"]}", 15f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 14), 0, dp(ctx, 2)) })
            (sec["preguntas"] as? List<Map<String, Any?>>).orEmpty().forEach { q ->
                val c = Ui007.card(ctx)
                val tipo = q["tipo"].toString()
                c.addView(Ui007.text(ctx, "${icono(tipo)}  ${q["id"]} · $tipo", 12f, Ui007.BLUE, true))
                c.addView(Ui007.text(ctx, q["texto"].toString(), 13.5f, Ui007.INK).apply { maxLines = 3; ellipsize = android.text.TextUtils.TruncateAt.END })
                c.setOnClickListener { detalleNodo(q) }
                bloque.addView(c)
            }
            agregadasDe(sid).forEach { q ->
                val c = Ui007.card(ctx).apply { background = Ui007.round(0xFFF3F7FE.toInt(), 14, ctx, 0xFFA9C2EC.toInt()) }
                c.addView(Ui007.text(ctx, "＋  ${id(q)} · agregada", 12f, Ui007.BLUE, true))
                c.addView(Ui007.text(ctx, q["texto"].toString(), 13.5f, Ui007.INK))
                bloque.addView(c)
            }
            body.addView(bloque)
            secciones[sid] = bloque
            fichas.addView(Ui007.chip(ctx, sid + " · " + sec["nombre"], false) { scroll?.post { scroll?.smoothScrollTo(0, (bloque.top - dp(ctx, 8)).coerceAtLeast(0)) } })
        }
        body.addView(Ui007.pill(ctx, "■ Fin", 0xFFEEF1F6.toInt(), 0xFF4A5A72.toInt()).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(ctx, 14) })
        // Las fichas de sección van arriba, antes de Inicio.
        body.addView(barra, 2, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 4) })
    }

    private fun icono(tipo: String) = when {
        tipo.startsWith("Evidencia") || tipo.startsWith("Fotos") || tipo.startsWith("Datos leídos") -> "📷"
        tipo.startsWith("Varias") -> "☑"
        tipo.startsWith("Opción") -> "◉"
        tipo.startsWith("Fecha") -> "📅"
        tipo.startsWith("Texto") -> "✎"
        tipo.startsWith("Enlaces") -> "🔗"
        else -> "•"
    }

    @Suppress("UNCHECKED_CAST")
    private fun detalleNodo(q: Map<String, Any?>) {
        val ctx = requireContext()
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 18), dp(ctx, 18), dp(ctx, 18), dp(ctx, 22)) }
        col.addView(Ui007.text(ctx, "${q["id"]} · ${q["tipo"]}", 13f, Ui007.BLUE, true))
        col.addView(Ui007.text(ctx, "Lo que verá el técnico", 12f, Ui007.GREY, true).apply { setPadding(0, dp(ctx, 10), 0, dp(ctx, 2)) })
        col.addView(Ui007.text(ctx, q["texto"].toString(), 15.5f, Ui007.INK))
        val ops = opciones(q)
        if (ops.isNotEmpty()) {
            col.addView(Ui007.text(ctx, "Opciones", 12f, Ui007.GREY, true).apply { setPadding(0, dp(ctx, 10), 0, dp(ctx, 2)) })
            ops.forEach { col.addView(Ui007.text(ctx, "• $it", 14f, Ui007.INK)) }
        }
        col.addView(Ui007.text(ctx, if (q["opcional"] == true) "Es opcional." else "El técnico la responde en el Chat IA cuando le corresponde según lo respondido antes.", 12f, Ui007.GREY).apply { setPadding(0, dp(ctx, 10), 0, 0) })
        sheet.setContentView(ScrollView(ctx).apply { addView(col) })
        sheet.show()
    }

    private fun renderReglas() {
        val ctx = requireContext()
        reglas.firstOrNull { id(it) == "SISIMP" }?.let { body.addView(sistemaImpresiones(it)) }
        body.addView(Ui007.button(ctx, "+ Nueva regla", true) { formulario(null) })
        reglas.filter { id(it) != "SISIMP" }.forEach { r ->
            val activa = r["activa"] == true
            val c = Ui007.card(ctx)
            val cab = Ui007.row(ctx)
            cab.addView(Ui007.text(ctx, "${id(r)} · ${r["nombre"]}", 15f, Ui007.INK, true).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
            cab.addView(SwitchCompat(ctx).apply {
                isChecked = activa
                setOnCheckedChangeListener { _, on -> guardar(id(r), mapOf("activa" to on)) }
            })
            c.addView(cab)
            val (bg, fg) = when (r["tipo"]) { "Recomendación" -> 0xFFE3ECFB.toInt() to 0xFF1A4FA8.toInt(); "Aviso" -> 0xFFFDECC8.toInt() to 0xFF8A5A00.toInt(); else -> 0xFFDDF3E6.toInt() to 0xFF16663A.toInt() }
            c.addView(Ui007.pill(ctx, r["tipo"].toString() + if (activa) "" else " · desactivada", bg, fg).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(ctx, 6) })
            (r["cuando"] as? String)?.takeIf { it.isNotBlank() }?.let { c.addView(Ui007.text(ctx, "Cuándo aplica: $it", 13f, Ui007.GREY).apply { setPadding(0, dp(ctx, 6), 0, 0) }) }
            (r["texto"] as? String)?.takeIf { it.isNotBlank() }?.let { c.addView(Ui007.text(ctx, "En la minuta: $it", 13f, Ui007.INK).apply { setPadding(0, dp(ctx, 4), 0, 0) }) }
            val acc = Ui007.row(ctx)
            acc.addView(Ui007.button(ctx, "Editar", false) { formulario(r) })
            acc.addView(Ui007.button(ctx, "Eliminar", false) { confirmarEliminar(r) })
            c.addView(acc)
            body.addView(c)
        }
    }

    private fun sistemaImpresiones(r: Map<String, Any?>): View {
        val ctx = requireContext()
        val c = Ui007.card(ctx)
        val cab = Ui007.row(ctx)
        cab.addView(Ui007.text(ctx, "SISIMP · Sistema de impresiones", 15f, Ui007.INK, true).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
        cab.addView(SwitchCompat(ctx).apply { isChecked = r["activa"] == true; setOnCheckedChangeListener { _, on -> guardar("SISIMP", mapOf("activa" to on)) } })
        c.addView(cab)
        c.addView(Ui007.text(ctx, "Se usa en las preguntas, las reglas y la minuta. Si está activa, antes de generar la minuta se pide el AnyDesk del equipo con este sistema.", 12f, Ui007.GREY))
        val et = EditText(ctx).apply {
            hint = "Wrapper"; setText((r["valor"] as? String).orEmpty()); setSingleLine(); textSize = 14f
            background = Ui007.round(Color.WHITE, 10, ctx, Ui007.LINE)
            setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 8) }
        }
        c.addView(et)
        c.addView(Ui007.button(ctx, "Guardar nombre", false) { guardar("SISIMP", mapOf("valor" to et.text.toString().trim())) })
        return c
    }

    private fun guardar(id: String, cambios: Map<String, Any?>) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { repo.reglaEditar(id, cambios) }
                .onSuccess { Toast.makeText(requireContext(), "Guardado.", Toast.LENGTH_SHORT).show(); load() }
                .onFailure { Toast.makeText(requireContext(), it.toFriendlyMessage(), Toast.LENGTH_LONG).show(); load() }
        }
    }

    private fun confirmarEliminar(r: Map<String, Any?>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar regla")
            .setMessage("«${r["nombre"]}» ya no saldrá en las minutas nuevas. ¿Eliminarla?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { repo.reglaEliminar(id(r)) }
                        .onSuccess { load() }
                        .onFailure { Toast.makeText(requireContext(), it.toFriendlyMessage(), Toast.LENGTH_LONG).show() }
                }
            }.setNegativeButton("Cancelar", null).show()
    }

    /** Crear (r = null) o editar una regla. */
    private fun formulario(r: Map<String, Any?>?) {
        val ctx = requireContext()
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; val p = dp(ctx, 20); setPadding(p, p, p, 0) }
        fun campo(hint: String, valor: String) = EditText(ctx).apply { this.hint = hint; setText(valor); textSize = 14f }.also { col.addView(it) }
        val nombre = campo("Nombre de la regla", r?.get("nombre") as? String ?: "")
        val cuando = campo("Cuándo aplica", r?.get("cuando") as? String ?: "")
        val texto = campo("Qué dice en la minuta", r?.get("texto") as? String ?: "")
        var tipo = r?.get("tipo") as? String ?: "Observación"
        val chips = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 10), 0, 0) }
        fun pintar() {
            chips.removeAllViews()
            listOf("Observación", "Recomendación", "Aviso").forEach { t -> chips.addView(Ui007.chip(ctx, t, t == tipo) { tipo = t; pintar() }) }
        }
        pintar()
        col.addView(android.widget.HorizontalScrollView(ctx).apply { addView(chips) })
        AlertDialog.Builder(ctx)
            .setTitle(if (r == null) "Nueva regla" else "Editar regla")
            .setView(col)
            .setPositiveButton("Guardar") { _, _ ->
                val cuerpo = mapOf("nombre" to nombre.text.toString(), "cuando" to cuando.text.toString(), "texto" to texto.text.toString(), "tipo" to tipo)
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { if (r == null) repo.reglaCrear(cuerpo) else repo.reglaEditar(id(r), cuerpo) }
                        .onSuccess { load() }
                        .onFailure { Toast.makeText(ctx, it.toFriendlyMessage(), Toast.LENGTH_LONG).show() }
                }
            }.setNegativeButton("Cancelar", null).show()
    }
}
