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
 * Preguntas y reglas (administrador, doc 0.0.7 §11). Pestañas Preguntas y Reglas. Reglas: activar o desactivar, editar,
 * eliminar y crear; la regla fija SISIMP guarda el nombre del sistema de impresiones de la empresa. Preguntas: las que se
 * agregaron al flujo; las base viven en el motor del Chat IA y todavía no se reordenan desde la app. Falta el Mapa de nodos.
 */
@AndroidEntryPoint
class PreguntasReglasFragment : Fragment() {

    @Inject lateinit var repo: Asistente007Repository
    private lateinit var body: LinearLayout
    private lateinit var progress: ProgressBar
    private var tab = "Reglas"
    private var reglas: List<Map<String, Any?>> = emptyList()
    private var preguntas: List<Map<String, Any?>> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val frame = FrameLayout(ctx).apply { setBackgroundColor(Ui007.BG) }
        val scroll = ScrollView(ctx).apply { isFillViewport = true }
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
        listOf("Preguntas", "Reglas").forEach { t -> tabs.addView(Ui007.chip(ctx, t, t == tab) { tab = t; render() }) }
        body.addView(tabs)
        if (tab == "Reglas") renderReglas() else renderPreguntas()
    }

    private fun renderPreguntas() {
        val ctx = requireContext()
        body.addView(Ui007.text(ctx, "Las preguntas base del Chat IA vienen del motor y no se editan aquí todavía. Estas son las que se agregaron al flujo.", 13f, Ui007.GREY).apply { setPadding(0, dp(ctx, 10), 0, 0) })
        if (preguntas.isEmpty()) body.addView(Ui007.card(ctx).apply { addView(Ui007.text(ctx, "Aún no se agregó ninguna pregunta.", 14f, Ui007.GREY)) })
        preguntas.forEach { q ->
            val c = Ui007.card(ctx)
            c.addView(Ui007.text(ctx, q["texto"].toString(), 15f, Ui007.INK, true))
            c.addView(Ui007.text(ctx, listOfNotNull(id(q), q["seccion"]?.toString(), q["tipo"]?.toString()).joinToString(" · "), 12f, Ui007.GREY))
            (q["cuando"] as? String)?.takeIf { it.isNotBlank() }?.let { c.addView(Ui007.text(ctx, "Cuándo: $it", 12f, Ui007.GREY)) }
            body.addView(c)
        }
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
