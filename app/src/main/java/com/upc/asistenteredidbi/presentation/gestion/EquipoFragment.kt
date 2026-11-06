package com.upc.asistenteredidbi.presentation.gestion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
 * Equipo y accesos (administrador, doc 0.0.7 §15): lista agrupada en Supervisores, Técnicos y Administradores, con búsqueda
 * y filtros. Al tocar una persona se cambia su rol y, si es técnico, su supervisor.
 */
@AndroidEntryPoint
class EquipoFragment : Fragment() {

    @Inject lateinit var repo: Asistente007Repository
    private lateinit var body: LinearLayout
    private lateinit var progress: ProgressBar
    private var personas: List<Map<String, Any?>> = emptyList()
    private var filtro = "Todos"
    private var busqueda = ""

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
            val r = runCatching { repo.equipo() }
            progress.visibility = View.GONE
            if (context == null) return@launch
            r.onSuccess { personas = it; render() }.onFailure {
                body.removeAllViews()
                body.addView(Ui007.text(requireContext(), "No se pudo cargar el equipo.", 15f, Ui007.GREY))
            }
        }
    }

    private fun rol(p: Map<String, Any?>) = p["rol"].toString()
    private fun id(p: Map<String, Any?>) = (p["id"] as Number).toLong()

    private fun render() {
        val ctx = requireContext()
        body.removeAllViews()
        body.addView(Ui007.text(ctx, "Equipo y accesos", 24f, Ui007.INK, true))
        body.addView(Ui007.text(ctx, "Toca una persona para cambiar su rol o su supervisor. Las cuentas nuevas se crean desde la web.", 13f, Ui007.GREY))

        val et = android.widget.EditText(ctx).apply {
            hint = "Buscar por nombre o correo"; setText(busqueda); setSingleLine()
            background = Ui007.round(android.graphics.Color.WHITE, 12, ctx, Ui007.LINE)
            setPadding(dp(ctx, 14), dp(ctx, 10), dp(ctx, 14), dp(ctx, 10)); textSize = 14f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 12) }
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { v, _, _ -> busqueda = v.text.toString().trim(); render(); true }
        }
        body.addView(et)

        val chips = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 10), 0, 0) }
        val porAtender = personas.count { it["pidioCodigo"] == true }
        listOf("Todos", "Supervisores", "Técnicos", "Por atender ($porAtender)").forEach { f ->
            chips.addView(Ui007.chip(ctx, f, f == filtro) { filtro = f; render() })
        }
        body.addView(android.widget.HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; addView(chips) })

        val q = busqueda.lowercase()
        val visibles = personas.filter { p ->
            (q.isEmpty() || p["nombre"].toString().lowercase().contains(q) || p["email"].toString().lowercase().contains(q)) &&
                when {
                    filtro == "Supervisores" -> rol(p) == "SUPERVISOR"
                    filtro == "Técnicos" -> rol(p) == "TECNICO"
                    filtro.startsWith("Por atender") -> p["pidioCodigo"] == true
                    else -> true
                }
        }
        listOf("Supervisores" to "SUPERVISOR", "Técnicos" to "TECNICO", "Administradores" to "ADMINISTRADOR").forEach { (titulo, r) ->
            val grupo = visibles.filter { rol(it) == r }
            if (grupo.isEmpty()) return@forEach
            body.addView(Ui007.text(ctx, "$titulo (${grupo.size})", 13f, Ui007.GREY, true).apply { setPadding(0, dp(ctx, 16), 0, dp(ctx, 2)) })
            grupo.forEach { body.addView(fila(it)) }
        }
        if (visibles.isEmpty()) body.addView(Ui007.text(ctx, "Nadie coincide con la búsqueda.", 14f, Ui007.GREY).apply { setPadding(0, dp(ctx, 16), 0, 0) })
    }

    private fun fila(p: Map<String, Any?>): View {
        val ctx = requireContext()
        val c = Ui007.card(ctx).apply { setOnClickListener { ficha(p) } }
        val f = Ui007.row(ctx)
        val nombre = p["nombre"].toString()
        val ini = nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
        val color = when (rol(p)) { "SUPERVISOR" -> 0xFF6A3D9A.toInt(); "ADMINISTRADOR" -> 0xFF16663A.toInt(); else -> Ui007.BLUE }
        f.addView(Ui007.text(ctx, ini, 14f, android.graphics.Color.WHITE, true).apply {
            gravity = android.view.Gravity.CENTER; background = Ui007.round(color, 999, ctx)
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 40), dp(ctx, 40)).apply { marginEnd = dp(ctx, 12) }
        })
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        col.addView(Ui007.text(ctx, nombre, 15f, Ui007.INK, true).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END })
        val sub = when (rol(p)) {
            "SUPERVISOR" -> "${(p["tecnicosACargo"] as? Number)?.toInt() ?: 0} técnicos a cargo"
            "TECNICO" -> p["supervisorNombre"]?.let { "Supervisor: $it" } ?: "Sin supervisor asignado"
            else -> "Administrador"
        }
        col.addView(Ui007.text(ctx, sub, 12f, Ui007.GREY).apply { maxLines = 1 })
        f.addView(col)
        if (p["pidioCodigo"] == true) f.addView(Ui007.pill(ctx, "Pidió código", 0xFFFDECC8.toInt(), 0xFF8A5A00.toInt()))
        c.addView(f)
        return c
    }

    private fun ficha(p: Map<String, Any?>) {
        val ctx = requireContext()
        val roles = listOf("Técnico" to "TECNICO", "Supervisor" to "SUPERVISOR", "Administrador" to "ADMINISTRADOR")
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; val pad = dp(ctx, 20); setPadding(pad, pad, pad, pad) }
        col.addView(Ui007.text(ctx, p["nombre"].toString(), 20f, Ui007.INK, true))
        col.addView(Ui007.text(ctx, p["email"].toString(), 13f, Ui007.GREY))
        col.addView(Ui007.text(ctx, "Rol", 13f, Ui007.GREY, true).apply { setPadding(0, dp(ctx, 16), 0, dp(ctx, 6)) })
        val rolesRow = Ui007.row(ctx)
        roles.forEach { (label, value) ->
            rolesRow.addView(Ui007.chip(ctx, label, value == rol(p)) {
                if (value == rol(p)) return@chip
                sheet.dismiss()
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { repo.cambiarRol(id(p), value) }
                        .onSuccess { Toast.makeText(ctx, "${p["nombre"]} ahora es $label.", Toast.LENGTH_SHORT).show(); load() }
                        .onFailure { Toast.makeText(ctx, it.toFriendlyMessage(), Toast.LENGTH_LONG).show() }
                }
            })
        }
        col.addView(rolesRow)
        if (rol(p) == "TECNICO") {
            col.addView(Ui007.text(ctx, "Su supervisor", 13f, Ui007.GREY, true).apply { setPadding(0, dp(ctx, 16), 0, dp(ctx, 6)) })
            val supervisores = personas.filter { rol(it) == "SUPERVISOR" }
            if (supervisores.isEmpty()) col.addView(Ui007.text(ctx, "Aún no hay supervisores. Cambia el rol de alguien a Supervisor.", 13f, Ui007.GREY))
            val wrap = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            supervisores.forEach { s ->
                wrap.addView(Ui007.chip(ctx, s["nombre"].toString(), p["supervisorId"] != null && (p["supervisorId"] as Number).toLong() == id(s)) {
                    sheet.dismiss()
                    viewLifecycleOwner.lifecycleScope.launch {
                        runCatching { repo.asignarSupervisor(id(p), id(s)) }
                            .onSuccess { Toast.makeText(ctx, "Supervisor asignado.", Toast.LENGTH_SHORT).show(); load() }
                            .onFailure { Toast.makeText(ctx, it.toFriendlyMessage(), Toast.LENGTH_LONG).show() }
                    }
                }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(ctx, 8) } })
            }
            col.addView(wrap)
        }
        sheet.setContentView(col)
        sheet.show()
    }
}
