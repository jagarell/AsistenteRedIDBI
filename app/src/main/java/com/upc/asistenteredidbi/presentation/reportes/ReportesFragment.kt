package com.upc.asistenteredidbi.presentation.reportes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.graphics.Color
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
        if (savedInstanceState == null) vm.load(arguments?.getString("tab"), arguments?.getString("estado"), arguments?.getString("criticidad"), arguments?.getString("tecnico"))
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

        accesoTemporal()

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

    // ---------- Acceso temporal a minutas de otros (doc 0.0.7 §46) ----------

    @javax.inject.Inject lateinit var apiAcceso: com.upc.asistenteredidbi.data.remote.Api007Service
    private var accesosVigentes: List<Map<String, Any?>> = emptyList()
    private var accesosCargados = false

    private fun accesoTemporal() {
        val ctx = requireContext()
        val gestiona = vm.state.value.tabs.any { it.label == "Por técnico" }
        if (gestiona) return // el administrador ya ve todas las minutas
        val c = Ui007.card(ctx).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 12) } }
        c.addView(Ui007.text(ctx, "Ver la minuta de otro", 14f, Ui007.INK, true))
        c.addView(Ui007.text(ctx, "Pide acceso a tu supervisor (o al administrador) con un motivo. Con el código ves la minuta 24 horas, solo lectura.", 12f, Ui007.GREY))
        val fila = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 8), 0, 0) }
        fila.addView(Ui007.chip(ctx, "Pedir acceso", true) { pedirAcceso() })
        fila.addView(Ui007.chip(ctx, "Ingresar código", false) { ingresarCodigo() }.apply { (layoutParams as LinearLayout.LayoutParams).marginStart = dp(ctx, 8) })
        c.addView(fila)
        accesosVigentes.forEach { a ->
            val f = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 8), 0, 0) }
            f.addView(Ui007.text(ctx, "${a["local"]} · hasta ${a["vence"]}", 12.5f, Ui007.INK), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            f.addView(Ui007.chip(ctx, "Ver", true) { verAcceso((a["accesoId"] as Number).toLong()) })
            c.addView(f)
        }
        body.addView(c)
        if (!accesosCargados) {
            accesosCargados = true
            viewLifecycleOwner.lifecycleScope.launch { accesosVigentes = runCatching { apiAcceso.accesosMios() }.getOrDefault(emptyList()); render(vm.state.value) }
        }
    }

    private fun pedirAcceso() {
        val ctx = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            val lista = runCatching { apiAcceso.accesosCandidatas() }.getOrDefault(emptyList())
            val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
            val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; val p = dp(ctx, 18); setPadding(p, p, p, p) }
            col.addView(Ui007.text(ctx, "Pedir acceso a una minuta", 18f, Ui007.INK, true))
            var elegida: Long? = null
            var motivo: String? = null
            val motivos = listOf("Visita de seguimiento al mismo local", "Reemplazo a otro técnico", "Mismo cliente en otro local")
            val enviar = Ui007.button(ctx, "Enviar solicitud", true) {}.apply { alpha = 0.4f }
            fun habilitar() { enviar.alpha = if (elegida != null && motivo != null) 1f else 0.4f }
            col.addView(Ui007.text(ctx, "1. Elige la minuta", 13f, Ui007.GREY, true).apply { setPadding(0, dp(ctx, 10), 0, dp(ctx, 4)) })
            if (lista.isEmpty()) col.addView(Ui007.text(ctx, "No hay minutas de otros para pedir.", 13f, Ui007.GREY))
            val filas = mutableListOf<Pair<Long, View>>()
            lista.take(12).forEach { m ->
                val id = (m["id"] as Number).toLong()
                val v = Ui007.text(ctx, "${m["local"]} · ${m["tecnico"]} · ${m["fecha"]}", 13.5f, Ui007.INK).apply {
                    background = Ui007.round(Color.WHITE, 12, ctx, Ui007.LINE); setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 6) }
                    setOnClickListener { elegida = id; filas.forEach { (i, w) -> w.background = Ui007.round(if (i == id) 0xFFE8F1FD.toInt() else Color.WHITE, 12, ctx, if (i == id) Ui007.BLUE else Ui007.LINE) }; habilitar() }
                }
                filas.add(id to v); col.addView(v)
            }
            col.addView(Ui007.text(ctx, "2. Motivo", 13f, Ui007.GREY, true).apply { setPadding(0, dp(ctx, 12), 0, dp(ctx, 4)) })
            val chips = Ui007.row(ctx)
            val cs = mutableListOf<Pair<String, TextView>>()
            motivos.forEach { mo ->
                val ch = Ui007.chip(ctx, mo, false) { motivo = mo; cs.forEach { (k, t) -> t.background = Ui007.round(if (k == mo) 0xFFE8F1FD.toInt() else Color.WHITE, 20, ctx, if (k == mo) Ui007.BLUE else Ui007.LINE) }; habilitar() }
                ch.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(ctx, 6) }
                cs.add(mo to ch); chips.addView(ch)
            }
            col.addView(HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; addView(chips) })
            enviar.setOnClickListener {
                val id = elegida; val mo = motivo
                if (id == null || mo == null) { Toast.makeText(ctx, "Elige la minuta y el motivo.", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { apiAcceso.accesoPedir(mapOf("minutaId" to id, "motivo" to mo)) }
                        .onSuccess { sheet.dismiss(); Toast.makeText(ctx, "Solicitud enviada. Te avisarán cuando la aprueben.", Toast.LENGTH_LONG).show() }
                        .onFailure { Toast.makeText(ctx, "No se pudo enviar la solicitud.", Toast.LENGTH_LONG).show() }
                }
            }
            col.addView(enviar)
            sheet.setContentView(ScrollView(ctx).apply { addView(col) })
            sheet.show()
        }
    }

    private fun ingresarCodigo() {
        val ctx = requireContext()
        val campo = android.widget.EditText(ctx).apply { hint = "Código de 6 dígitos"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; filters = arrayOf(android.text.InputFilter.LengthFilter(6)); textSize = 20f; gravity = android.view.Gravity.CENTER }
        androidx.appcompat.app.AlertDialog.Builder(ctx).setTitle("Ingresar código").setMessage("Escribe el código que te dio quien aprobó tu solicitud.").setView(campo)
            .setPositiveButton("Abrir") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { apiAcceso.accesoCanjear(mapOf("codigo" to campo.text.toString())) }
                        .onSuccess { r -> accesosVigentes = runCatching { apiAcceso.accesosMios() }.getOrDefault(emptyList()); verAcceso((r["accesoId"] as Number).toLong()) }
                        .onFailure { Toast.makeText(ctx, "El código no es válido o ya venció.", Toast.LENGTH_LONG).show() }
                }
            }.setNegativeButton("Cancelar", null).show()
    }

    /** Minuta en solo lectura: no se edita, descarga ni comparte; el acceso termina a las 24 horas. */
    private fun verAcceso(id: Long) {
        val ctx = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            val m = runCatching { apiAcceso.accesoMinuta(id) }.getOrNull()
            if (m == null) { Toast.makeText(ctx, "El acceso ya no está vigente.", Toast.LENGTH_SHORT).show(); return@launch }
            val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
            val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; val p = dp(ctx, 18); setPadding(p, p, p, p) }
            col.addView(Ui007.text(ctx, m["clientName"]?.toString().orEmpty(), 18f, Ui007.INK, true))
            col.addView(Ui007.text(ctx, "Solo lectura · v${m["version"]} · ${m["estado"]} · acceso hasta ${m["vence"]}", 12f, Ui007.GREY).apply { setPadding(0, 0, 0, dp(ctx, 8)) })
            fun fila(k: String, v: Any?) { val t = v?.toString().orEmpty(); if (t.isNotBlank()) col.addView(Ui007.text(ctx, "$k: $t", 14f, Ui007.INK).apply { setPadding(0, dp(ctx, 5), 0, 0) }) }
            fila("Técnico", m["technicianName"]); fila("Dirección", m["address"])
            val c = runCatching { org.json.JSONObject(m["contentJson"]?.toString() ?: "{}") }.getOrDefault(org.json.JSONObject())
            fila("Observaciones", c.optString("obsTec"))
            c.optJSONArray("recs")?.let { a -> for (i in 0 until a.length()) fila("Recomendación", a.optJSONObject(i)?.optString("t") ?: a.optString(i)) }
            col.addView(Ui007.button(ctx, "Cerrar", false) { sheet.dismiss() })
            sheet.setContentView(ScrollView(ctx).apply { addView(col) })
            sheet.show()
        }
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

    /**
     * «Lo que falta · por prioridad» (doc 0.0.7 §17): al filtrar por un técnico, primero lo que falta validar (menor % primero),
     * luego las rechazadas por corregir, las pendientes de solicitar la validación y las que siguen sin terminar.
     */
    private fun loQueFalta(s: ReportesUiState) {
        val ctx = requireContext()
        val tec = s.filtro.tecnico.singleOrNull() ?: return
        val lista = s.data?.minutas.orEmpty()
        val items = buildList {
            lista.filter { it.estado == "Completa" }.sortedBy { it.puntaje ?: 100 }.forEach { add("Falta validar" to it) }
            lista.filter { it.estado == "En revisión" }.forEach { add("Rechazada · por corregir" to it) }
            lista.filter { it.estado == "Pendiente" }.forEach { add("Pendiente de solicitar la validación" to it) }
        }
        val sinTerminar = s.data?.borradores.orEmpty().filter { it["tecnico"]?.toString().equals(tec, ignoreCase = true) }
        if (items.isEmpty() && sinTerminar.isEmpty()) return
        val c = Ui007.card(ctx)
        c.addView(Ui007.text(ctx, "Lo que falta · por prioridad", 15f, Ui007.INK, true))
        var n = 1
        items.forEach { (quien, m) ->
            val f = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 8), 0, 0); setOnClickListener { vm.select(m) } }
            f.addView(Ui007.text(ctx, "${n++}. ${m.local ?: "—"}\n$quien" + (m.puntaje?.let { " · $it %" } ?: ""), 13.5f, Ui007.INK), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            f.addView(Ui007.text(ctx, "›", 18f, Ui007.GREY))
            c.addView(f)
        }
        sinTerminar.forEach { b ->
            val f = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 8), 0, 0); setOnClickListener { fichaBorrador(b) } }
            f.addView(Ui007.text(ctx, "${n++}. ${b["local"]}\nBorrador · sin minuta · ${(b["avance"] as? Number)?.toInt() ?: 0} %", 13.5f, Ui007.INK), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            f.addView(Ui007.text(ctx, "›", 18f, Ui007.GREY))
            c.addView(f)
        }
        body.addView(c)
    }

    /** Evaluaciones que se quedaron en el levantamiento: Borrador, sin puntaje ni criticidad, con su avance. */
    private fun borradores(s: ReportesUiState) {
        val ctx = requireContext()
        val estados = s.filtro.estado
        if (estados.isNotEmpty() && "Borrador" !in estados && "Pendiente" !in estados) return
        val tec = s.filtro.tecnico
        val bs = s.data?.borradores.orEmpty().filter { tec.isEmpty() || tec.any { t -> t.equals(it["tecnico"]?.toString(), ignoreCase = true) } }
        if (bs.isEmpty()) return
        body.addView(Ui007.text(ctx, "Borradores · sin minuta todavía (${bs.size})", 15f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 16), 0, 0) })
        bs.forEach { b ->
            val avance = (b["avance"] as? Number)?.toInt() ?: 0
            val c = Ui007.card(ctx)
            c.addView(Ui007.text(ctx, b["local"]?.toString() ?: "—", 15f, Ui007.INK, true))
            c.addView(Ui007.text(ctx, listOfNotNull(b["fecha"]?.toString(), b["tecnico"]?.toString()).joinToString(" · "), 12f, Ui007.GREY))
            val pills = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 6), 0, 0) }
            Ui007.estadoColors("Borrador").let { (bg, fg) -> pills.addView(Ui007.pill(ctx, "Borrador", bg, fg)) }
            c.addView(pills)
            c.addView(android.widget.ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = avance; layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 8)).apply { topMargin = dp(ctx, 8) } })
            c.addView(Ui007.text(ctx, "$avance % · sin minuta todavía", 12f, Ui007.GREY).apply { setPadding(0, dp(ctx, 4), 0, 0) })
            c.setOnClickListener { fichaBorrador(b) }
            body.addView(c)
        }
    }

    /** Ficha de solo lectura «Borrador · sin terminar»: avance, lo levantado y, según el rol, pedir que la termine o continuar el Chat IA. */
    private fun fichaBorrador(b: Map<String, Any?>) {
        val ctx = requireContext()
        val ev = (b["evaluationId"] as? Number)?.toLong() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val r = runCatching { apiAcceso.levantamiento(ev) }.getOrNull().orEmpty()
            val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
            val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 18), dp(ctx, 18), dp(ctx, 18), dp(ctx, 24)) }
            col.addView(Ui007.text(ctx, "Borrador · sin terminar", 18f, Ui007.INK, true))
            col.addView(Ui007.text(ctx, "${(r["avance"] as? Number)?.toInt() ?: (b["avance"] as? Number)?.toInt() ?: 0} % del levantamiento", 14f, Ui007.BLUE, true).apply { setPadding(0, dp(ctx, 4), 0, 0) })
            col.addView(Ui007.text(ctx, "Todavía no es una minuta: no tiene puntaje, no cuenta en «Por validar» y no se puede compartir.", 12.5f, Ui007.GREY).apply { setPadding(0, dp(ctx, 6), 0, dp(ctx, 8)) })
            col.addView(Ui007.text(ctx, "LO QUE YA SE LEVANTÓ", 11f, Ui007.GREY, true))
            listOf("Local" to r["local"], "Técnico" to r["tecnico"], "Dirección" to r["direccion"], "Encargado" to r["contacto"], "Celular" to r["telefono"], "Rubro" to r["rubro"])
                .forEach { (k, v) -> if (v != null && v.toString().isNotBlank()) col.addView(Ui007.text(ctx, "$k: $v", 13.5f, Ui007.INK).apply { setPadding(0, dp(ctx, 3), 0, 0) }) }
            val gestiona = vm.state.value.tabs.any { it.key == "general" || it.key == "tecnicos" }
            if (gestiona) {
                col.addView(Ui007.button(ctx, "Pedir al técnico que la termine", true) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        runCatching { notificaciones.recordatorio(mapOf("evaluationId" to ev, "accion" to "terminar")) }
                            .onSuccess { Toast.makeText(ctx, "✓ Técnico avisado", Toast.LENGTH_SHORT).show(); sheet.dismiss() }
                            .onFailure { Toast.makeText(ctx, "No se pudo enviar el aviso.", Toast.LENGTH_SHORT).show() }
                    }
                })
            } else {
                col.addView(Ui007.button(ctx, "Continuar en el Chat IA", true) { sheet.dismiss(); findNavController().navigate(R.id.technicalChatFragment, bundleOf("evaluationId" to ev.toString())) })
            }
            sheet.setContentView(ScrollView(ctx).apply { addView(col) })
            sheet.show()
        }
    }

    private fun minutas(s: ReportesUiState) {
        val ctx = requireContext()
        loQueFalta(s)
        borradores(s)
        val list = s.data?.minutas.orEmpty()
        body.addView(Ui007.text(ctx, "Minutas (${s.data?.total ?: list.size})", 15f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 16), 0, 0) })
        if (list.isEmpty() && !s.loading) {
            body.addView(Ui007.text(ctx, "No hay minutas con estos filtros.", 13f, Ui007.GREY)); return
        }
        list.forEach { m ->
            val c = Ui007.card(ctx)
            if (s.selected?.id == m.id) c.background = Ui007.round(0xFFF3F7FE.toInt(), 14, ctx, 0xFFA9C2EC.toInt())
            c.addView(Ui007.text(ctx, m.local ?: "—", 15f, Ui007.INK, true))
            c.addView(Ui007.text(ctx, listOfNotNull(m.fecha?.let { fechaCorta(it) }, m.tecnico, m.rubro, if (m.tipo == "SOPORTE") "Soporte" else null).joinToString(" · "), 12f, Ui007.GREY))
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
        into.addView(Ui007.button(ctx, "Bitácora de la minuta", false) {
            com.upc.asistenteredidbi.presentation.minuta.BitacoraMinutaSheet.mostrar(ctx, viewLifecycleOwner.lifecycleScope, api007, m.id)
        })
        // Editar: siempre abre el Asistente IA con la minuta cargada (doc 0.0.7 §12).
        into.addView(Ui007.button(ctx, "Pedir un cambio al Asistente IA", false) {
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(com.upc.asistenteredidbi.R.id.asistenteFragment,
                androidx.core.os.bundleOf("minutaId" to m.id, "local" to (m.local ?: "la minuta")))
        })
        recordatorios(m, into)
    }

    @javax.inject.Inject
    lateinit var notificaciones: com.upc.asistenteredidbi.data.remote.NotificationApiService

    @javax.inject.Inject
    lateinit var api007: com.upc.asistenteredidbi.data.remote.Api007Service

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

    /** «2026-11-07T00:02:47.5» → «07/11/2026». */
    private fun fechaCorta(s: String): String = runCatching { java.time.LocalDate.parse(s.take(10)).let { "%02d/%02d/%d".format(it.dayOfMonth, it.monthValue, it.year) } }.getOrDefault(s)

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
