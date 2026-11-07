package com.upc.asistenteredidbi.presentation.reportes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.data.remote.FilaReporteDto
import com.upc.asistenteredidbi.data.remote.TecnicoReporteDto
import com.upc.asistenteredidbi.data.repository.Asistente007Repository
import com.upc.asistenteredidbi.data.repository.Asistente007Repository.Filtros
import com.upc.asistenteredidbi.presentation.reportes.Ui007.dp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * Reportes 0.0.7 (antes «Historial»). Mismas pestañas, filtros, indicadores y trazabilidad que la web, según el rol:
 * técnico → Mis minutas; supervisor → General de su equipo, Mis técnicos y Mis minutas; administrador → General y Por técnico.
 * La pantalla se arma en código para no tocar los layouts existentes; al cambiar de pestaña o filtro conserva la posición.
 */
@AndroidEntryPoint
class ReportesFragment : Fragment() {

    private val vm: ReportesViewModel by viewModels()
    @Inject lateinit var repo: Asistente007Repository

    private lateinit var scroll: ScrollView
    private lateinit var body: LinearLayout
    private lateinit var progress: ProgressBar
    private val scrollPorPestana = mutableMapOf<String, Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val frame = FrameLayout(ctx).apply { setBackgroundColor(Ui007.BG) }
        scroll = ScrollView(ctx).apply { isFillViewport = true }
        body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; val p = dp(ctx, 16); setPadding(p, p, p, dp(ctx, 32)) }
        scroll.addView(body)
        progress = ProgressBar(ctx).apply { layoutParams = FrameLayout.LayoutParams(dp(ctx, 40), dp(ctx, 40), android.view.Gravity.CENTER) }
        frame.addView(scroll); frame.addView(progress)
        return frame
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) vm.load(arguments?.getString("tab"), arguments?.getString("estado"), arguments?.getString("criticidad"))
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.state.collect { render(it) } }
        }
    }

    private fun render(s: ReportesUiState) {
        val ctx = context ?: return
        val y = scroll.scrollY
        Ui007.gone(progress, !s.loading)
        body.removeAllViews()

        body.addView(Ui007.text(ctx, "Reportes", 22f, Ui007.INK, true))
        body.addView(Ui007.text(ctx, "Minutas, plazos y desempeño según tu rol.", 13f, Ui007.GREY))
        s.error?.let { body.addView(Ui007.text(ctx, it, 13f, 0xFF9E2B23.toInt()).apply { setPadding(0, dp(ctx, 8), 0, 0) }) }
        if (s.tabs.isEmpty() && !s.loading) {
            body.addView(Ui007.text(ctx, "Tu rol no tiene acceso a Reportes. El administrador lo activa en Equipo › Roles.", 14f).apply { setPadding(0, dp(ctx, 16), 0, 0) })
            return
        }

        // Pestañas
        val tabs = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 12), 0, 0) }
        s.tabs.forEach { t -> tabs.addView(Ui007.chip(ctx, t.label, t.key == s.tab) { s.tab?.let { scrollPorPestana[it] = scroll.scrollY }; vm.selectTab(t.key) }) }
        body.addView(HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; addView(tabs) })

        if (s.tab != "tecnicos") filtros(s)
        indicadores(s)
        if (s.tab == "tecnicos") tecnicos(s.data?.tecnicos.orEmpty()) else minutas(s)
        if (s.data?.plazosEmpresa == true && s.tab == "general") {
            val c = Ui007.card(ctx)
            c.addView(Ui007.text(ctx, "Plazos de entrega", 15f, Ui007.INK, true))
            c.addView(Ui007.text(ctx, "Los plazos por esfuerzo (Básica, Media, Alta) los define el administrador en la web › Plazos. Aquí se aplican para «A tiempo», «Por vencer» y «En mora».", 13f, Ui007.GREY))
            body.addView(c)
        }

        val restore = s.tab?.let { scrollPorPestana.remove(it) } ?: y
        scroll.post { scroll.scrollTo(0, restore) }
    }

    // ---------- Filtros ----------

    private fun filtros(s: ReportesUiState) {
        val ctx = requireContext()
        val f = s.filtro
        val base = s.base?.minutas.orEmpty()
        val row = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 10), 0, 0) }
        val fecha = if (f.desde != null || f.hasta != null) "Fecha: ${f.desde ?: "…"} – ${f.hasta ?: "…"}" else "Fecha"
        row.addView(Ui007.chip(ctx, fecha, f.desde != null || f.hasta != null) { rango(f) })
        fun multi(label: String, sel: Set<String>, opts: List<String>, set: (Set<String>) -> Filtros) {
            if (opts.isEmpty()) return
            row.addView(Ui007.chip(ctx, if (sel.isEmpty()) label else "$label (${sel.size})", sel.isNotEmpty()) { elegir(label, opts, sel) { vm.setFiltro(set(it)) } })
        }
        fun opts(sel: (FilaReporteDto) -> String?) = base.mapNotNull(sel).filter { it.isNotBlank() }.distinct().sorted()
        if (s.tab != "mis") multi("Técnico", f.tecnico, opts { it.tecnico }) { f.copy(tecnico = it) }
        if (s.tab == "general") multi("Supervisor", f.supervisor, opts { it.supervisor }) { f.copy(supervisor = it) }
        multi("Rubro", f.rubro, opts { it.rubro }) { f.copy(rubro = it) }
        multi("Estado", f.estado, listOf("Pendiente", "Incompleta", "Completa", "En revisión", "Validada")) { f.copy(estado = it) }
        multi("Criticidad", f.criticidad, listOf("Viable", "Observado", "Riesgo")) { f.copy(criticidad = it) }
        multi("Esfuerzo", f.esfuerzo, listOf("Básica", "Media", "Alta")) { f.copy(esfuerzo = it) }
        multi("Plazo", f.plazo, listOf("A tiempo", "Por vencer", "En mora")) { f.copy(plazo = it) }
        if (f.count() > 0) row.addView(Ui007.chip(ctx, "Limpiar (${f.count()})", false) { vm.clearFiltros() })
        body.addView(HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; addView(row) })
    }

    private fun elegir(titulo: String, opts: List<String>, sel: Set<String>, done: (Set<String>) -> Unit) {
        val checked = BooleanArray(opts.size) { opts[it] in sel }
        AlertDialog.Builder(requireContext())
            .setTitle(titulo)
            .setMultiChoiceItems(opts.toTypedArray(), checked) { _, i, on -> checked[i] = on }
            .setPositiveButton("Aplicar") { _, _ -> done(opts.filterIndexed { i, _ -> checked[i] }.toSet()) }
            .setNeutralButton("Quitar") { _, _ -> done(emptySet()) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun rango(f: Filtros) {
        val picker = MaterialDatePicker.Builder.dateRangePicker().setTitleText("Rango de fechas").build()
        picker.addOnPositiveButtonClickListener { r ->
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            vm.setFiltro(f.copy(desde = r.first?.let { fmt.format(Date(it)) }, hasta = r.second?.let { fmt.format(Date(it)) }))
        }
        picker.show(childFragmentManager, "rango")
    }

    // ---------- Indicadores ----------

    private fun indicadores(s: ReportesUiState) {
        val ctx = requireContext()
        val i = s.data?.indicadores ?: return
        val kpis = listOf(
            "Completadas" to "${i.completadas} de ${i.total}",
            "A tiempo" to "${i.aTiempoPct}%",
            "Tiempo promedio" to Ui007.fmtMin(i.tiempoPromedioMin),
            "En riesgo" to "${i.enRiesgo}"
        )
        kpis.chunked(2).forEach { par ->
            val r = Ui007.row(ctx).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 10) } }
            par.forEachIndexed { idx, (k, v) ->
                val c = Ui007.card(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { if (idx == 0) marginEnd = dp(ctx, 10) }
                }
                c.addView(Ui007.text(ctx, k, 12f, Ui007.GREY))
                c.addView(Ui007.text(ctx, v, 20f, if (k == "En riesgo" && v != "0") 0xFF9E2B23.toInt() else Ui007.INK, true))
                r.addView(c)
            }
            body.addView(r)
        }
    }

    // ---------- Minutas + trazabilidad ----------

    private fun minutas(s: ReportesUiState) {
        val ctx = requireContext()
        val list = s.data?.minutas.orEmpty()
        body.addView(Ui007.text(ctx, "Minutas (${s.data?.total ?: list.size})", 15f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 16), 0, 0) })
        if (list.isEmpty() && !s.loading) {
            body.addView(Ui007.text(ctx, "No hay minutas con estos filtros.", 13f, Ui007.GREY)); return
        }
        list.forEach { m ->
            val c = Ui007.card(ctx)
            if (s.selected?.id == m.id) c.background = Ui007.round(0xFFF3F7FE.toInt(), 14, ctx, 0xFFA9C2EC.toInt())
            c.addView(Ui007.text(ctx, m.local ?: "—", 15f, Ui007.INK, true))
            c.addView(Ui007.text(ctx, listOfNotNull(m.fecha, m.tecnico, m.rubro, if (m.tipo == "SOPORTE") "Soporte" else null).joinToString(" · "), 12f, Ui007.GREY))
            val pills = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 6), 0, 0) }
            Ui007.estadoColors(m.estado).let { (bg, fg) -> pills.addView(Ui007.pill(ctx, m.estado ?: "—", bg, fg)) }
            m.criticidad?.let { cr -> Ui007.critColors(cr).let { (bg, fg) -> pills.addView(Ui007.pill(ctx, cr + (m.puntaje?.let { " · $it%" } ?: ""), bg, fg)) } }
            m.plazo?.let { pl -> pills.addView(Ui007.pill(ctx, pl, if (pl == "En mora") 0xFFFBE4E2.toInt() else 0xFFEEF1F6.toInt(), if (pl == "En mora") 0xFF9E2B23.toInt() else 0xFF4A5A72.toInt())) }
            c.addView(pills)
            c.setOnClickListener { vm.select(m) }
            if (s.selected?.id == m.id) detalle(m, s.versiones, c)
            body.addView(c)
        }
    }

    private fun detalle(m: FilaReporteDto, versiones: List<Map<String, Any?>>, into: LinearLayout) {
        val ctx = requireContext()
        into.addView(Ui007.text(ctx, "Trazabilidad", 13f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 10), 0, 0) })
        into.addView(Ui007.text(ctx, "Esfuerzo ${m.esfuerzo ?: "—"} · elaboración ${Ui007.fmtMin(m.minutos?.toLong())}" + (m.supervisor?.let { " · supervisor $it" } ?: ""), 12f, Ui007.GREY))
        if (versiones.isEmpty()) into.addView(Ui007.text(ctx, "Versión 1 · sin cambios posteriores.", 12f, Ui007.GREY))
        versiones.forEach { v ->
            val ver = (v["version"] as? Number)?.toInt()
            into.addView(Ui007.text(ctx, "v${ver ?: "?"} · ${v["authorName"] ?: ""} · ${fecha(v["createdAt"])}\n${v["resumen"] ?: ""}", 12f).apply { setPadding(0, dp(ctx, 4), 0, 0) })
        }
        into.addView(Ui007.button(ctx, "Ver minuta", true) { verMinuta(m) })
        recordatorios(m, into)
    }

    @javax.inject.Inject
    lateinit var notificaciones: com.upc.asistenteredidbi.data.remote.NotificationApiService

    /** Pedirle algo al técnico (doc 0.0.7 §29): terminar, corregir; solo el administrador recuerda al supervisor. */
    private fun recordatorios(m: FilaReporteDto, into: LinearLayout) {
        val ctx = requireContext()
        val tabs = vm.state.value.tabs
        val gestiona = tabs.any { it.key == "general" || it.key == "tecnicos" }
        if (!gestiona) return
        val admin = tabs.any { it.label == "Por técnico" }
        val opciones = buildList {
            when (m.estado) {
                "Pendiente" -> add("Pedir que la termine" to "terminar")
                "En revisión" -> add("Pedir que la corrija" to "corregir")
                "Completa" -> if (admin) add("Recordar al supervisor" to "supervisor")
            }
        }
        opciones.forEach { (label, accion) ->
            into.addView(Ui007.button(ctx, label, false) {
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { notificaciones.recordatorio(mapOf("minutaId" to m.id, "accion" to accion)) }
                        .onSuccess { r -> Toast.makeText(ctx, "✓ ${r["aviso"] ?: "Aviso"} avisado", Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(ctx, "No se pudo enviar el aviso.", Toast.LENGTH_SHORT).show() }
                }
            })
        }
    }

    private fun fecha(v: Any?): String = when (v) {
        is List<*> -> v.take(3).joinToString("-") { (it as? Number)?.toInt()?.toString()?.padStart(2, '0') ?: "" }
        null -> ""
        else -> v.toString().take(16).replace('T', ' ')
    }

    private fun verMinuta(m: FilaReporteDto) {
        if (m.tipo == "SOPORTE") {
            viewLifecycleOwner.lifecycleScope.launch {
                val r = runCatching { repo.minuta(m.id) }.getOrNull()
                val ctx = context ?: return@launch
                val cont = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; val p = dp(ctx, 18); setPadding(p, p, p, p) }
                DocBlocksView.render(ctx, r?.get("contentJson") as? String, cont)
                AlertDialog.Builder(ctx).setView(ScrollView(ctx).apply { addView(cont) }).setPositiveButton("Cerrar", null).show()
            }
            return
        }
        val ev = m.evaluacionId ?: return
        findNavController().navigate(R.id.action_historial_to_minuta, bundleOf("evaluationId" to ev.toString()))
    }

    // ---------- Por técnico ----------

    private fun tecnicos(list: List<TecnicoReporteDto>) {
        val ctx = requireContext()
        body.addView(Ui007.text(ctx, "Desempeño por técnico", 15f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 16), 0, 0) })
        if (list.isEmpty()) body.addView(Ui007.text(ctx, "Aún no hay técnicos con minutas.", 13f, Ui007.GREY))
        list.forEach { t ->
            val c = Ui007.card(ctx)
            c.addView(Ui007.text(ctx, t.tecnico, 15f, Ui007.INK, true))
            t.supervisor?.let { c.addView(Ui007.text(ctx, "Supervisor: $it", 12f, Ui007.GREY)) }
            c.addView(Ui007.text(ctx, "Meta ${t.completadas}/${t.meta} · A tiempo ${t.aTiempoPct}% · Promedio ${Ui007.fmtMin(t.tiempoPromedioMin)}", 13f).apply { setPadding(0, dp(ctx, 4), 0, 0) })
            val pills = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 6), 0, 0) }
            pills.addView(Ui007.pill(ctx, "En mora ${t.enMora}", if (t.enMora > 0) 0xFFFBE4E2.toInt() else 0xFFEEF1F6.toInt(), if (t.enMora > 0) 0xFF9E2B23.toInt() else 0xFF4A5A72.toInt()))
            pills.addView(Ui007.pill(ctx, "Devueltas ${t.devueltas}", 0xFFEEF1F6.toInt(), 0xFF4A5A72.toInt()))
            c.addView(pills)
            c.setOnClickListener { vm.selectTab(if (vm.state.value.tabs.any { it.key == "general" }) "general" else "mis"); vm.setFiltro(Filtros(tecnico = setOf(t.tecnico))) }
            body.addView(c)
        }
    }
}
