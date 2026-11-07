package com.upc.asistenteredidbi.presentation.gestion

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.data.remote.toFriendlyMessage
import com.upc.asistenteredidbi.data.repository.Asistente007Repository
import com.upc.asistenteredidbi.presentation.reportes.Ui007
import com.upc.asistenteredidbi.presentation.reportes.Ui007.dp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Panel de gestión v2 (administrador y supervisor, doc 0.0.7 §17, §43, §58). Un solo filtro general (Hoy, Ayer, 7 días, 14 días, Mes,
 * Todo y Rango de fechas) para indicadores, apartados (Por día · Estados · Tiempos · Alertas · Negocios), avance del equipo, por validar
 * primero y actividad reciente. Al tocar un apartado o el rango, el Panel se queda donde está.
 */
@AndroidEntryPoint
class PanelFragment : Fragment() {

    @Inject lateinit var repo: Asistente007Repository
    @Inject lateinit var api007: com.upc.asistenteredidbi.data.remote.Api007Service
    private lateinit var body: LinearLayout
    private lateinit var progress: ProgressBar
    private var periodo = "hoy"
    private var desde: LocalDate = LocalDate.now()
    private var hasta: LocalDate = LocalDate.now()
    private var apartado = "dia"
    private var datos: Map<String, Any?> = emptyMap()
    private var extras: Map<String, Any?> = emptyMap()
    private var scroll: ScrollView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val frame = FrameLayout(ctx).apply { setBackgroundColor(Ui007.BG) }
        val sv = ScrollView(ctx).apply { isFillViewport = true }
        scroll = sv
        body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; val p = dp(ctx, 16); setPadding(p, p + dp(ctx, 4), p, dp(ctx, 32)) }
        sv.addView(body)
        progress = ProgressBar(ctx).apply { layoutParams = FrameLayout.LayoutParams(dp(ctx, 40), dp(ctx, 40), Gravity.CENTER) }
        frame.addView(sv); frame.addView(progress)
        return frame
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { cargar() }

    private fun cargar() {
        progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val r = runCatching { repo.panelResumen(periodo, if (periodo == "rango") desde.toString() else null, if (periodo == "rango") hasta.toString() else null) }
            if (extras.isEmpty()) extras = runCatching { repo.panel(14) }.getOrDefault(emptyMap())
            progress.visibility = View.GONE
            if (context == null) return@launch
            r.onSuccess { datos = it; pintar() }.onFailure {
                body.removeAllViews()
                body.addView(Ui007.text(requireContext(), "No se pudo cargar el Panel. Revisa tu conexión.", 15f, Ui007.GREY))
            }
        }
    }

    /** Cambia solo el apartado: el Panel no vuelve al inicio del módulo. */
    private fun repintar() { val y = scroll?.scrollY ?: 0; pintar(); scroll?.post { scroll?.scrollTo(0, y) } }

    private fun num(m: Map<String, Any?>?, k: String): Long = (m?.get(k) as? Number)?.toLong() ?: 0L

    @Suppress("UNCHECKED_CAST")
    private fun pintar() {
        val ctx = requireContext()
        body.removeAllViews()
        body.addView(Ui007.text(ctx, "Panel", 24f, Ui007.INK, true))

        // Filtro general.
        val filtros = Ui007.row(ctx)
        listOf("hoy" to "Hoy", "ayer" to "Ayer", "7" to "7 días", "14" to "14 días", "mes" to "Mes", "todo" to "Todo", "rango" to "Rango de fechas").forEach { (k, l) ->
            filtros.addView(Ui007.chip(ctx, l, periodo == k) {
                if (k == "rango") elegirRango() else { periodo = k; cargar() }
            }.apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(ctx, 8) })
        }
        body.addView(HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; setPadding(0, dp(ctx, 10), 0, 0); addView(filtros) })
        val per = datos["periodo"] as? Map<String, Any?>
        body.addView(Ui007.text(ctx, "${per?.get("etiqueta") ?: ""} · ${fmt(per?.get("desde"))} al ${fmt(per?.get("hasta"))}", 12f, Ui007.GREY).apply { setPadding(0, dp(ctx, 6), 0, 0) })

        // Indicadores: salen de las mismas minutas que el avance, así que siempre cuadran.
        val ind = datos["indicadores"] as? Map<String, Any?>
        val total = num(ind, "total"); val ant = num(ind, "totalAnterior")
        val delta = if (ant == 0L) "sin periodo anterior" else (if (total >= ant) "+" else "") + (total - ant) + " vs periodo anterior"
        val conAnalisis = num(ind, "pendientesConAnalisis")
        val ahorro = (ind?.get("ahorroPct") as? Number)?.toInt()
        // título, valor, subtítulo, color del borde, color de fondo del subtítulo, color del texto del subtítulo
        val tarjetas = listOf(
            listOf("Total minutas", total.toString(), when { ant == 0L -> "Sin periodo anterior"; total == ant -> "Igual que el periodo anterior"; else -> (if (total > ant) "+" else "") + (total - ant) + " vs periodo anterior" }, 0xFF1565C0.toInt(), 0xFFE8F0FC.toInt(), 0xFF1565C0.toInt()),
            listOf("Por validar", num(ind, "porValidar").toString(), "${num(ind, "porValidarRiesgo")} con riesgo", 0xFFB45309.toInt(), 0xFFFFF3E0.toInt(), 0xFFB45309.toInt()),
            listOf("Pendientes de cerrar", num(ind, "pendientes").toString(), if (conAnalisis > 0) "$conAnalisis con análisis IA listo" else "Sin análisis IA listo", 0xFF6B7280.toInt(), 0xFFEEF1F6.toInt(), 0xFF4A5A72.toInt()),
            listOf("En revisión", num(ind, "enRevision").toString(), "Devueltas al técnico", 0xFFC62828.toInt(), 0xFFFBE4E2.toInt(), 0xFF9E2B23.toInt()),
            listOf("Validadas", num(ind, "validadas").toString(), "${num(ind, "validadasPct")} % del total", 0xFF2E7D32.toInt(), 0xFFE8F5E9.toInt(), 0xFF2E7D32.toInt()),
            listOf("Horas ahorradas", "%.1f".format((ind?.get("horasAhorradas") as? Number)?.toDouble() ?: 0.0), if (ahorro != null) "−$ahorro % por minuta" else "Aún sin tiempos registrados", 0xFF1565C0.toInt(), 0xFFE8F0FC.toInt(), 0xFF1565C0.toInt())
        )
        tarjetas.chunked(2).forEach { par ->
            val fila = Ui007.row(ctx).apply { gravity = Gravity.TOP }
            par.forEach { t ->
                // Borde de color arriba, título en mayúsculas y subtítulo en un chip de color (como el simulador).
                val borde = t[3] as Int
                val c = Ui007.card(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { topMargin = dp(ctx, 10); marginEnd = dp(ctx, 8) }
                    setPadding(dp(ctx, 12), 0, dp(ctx, 12), dp(ctx, 12))
                }
                c.addView(View(ctx).apply { setBackgroundColor(borde) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 4)))
                c.addView(Ui007.text(ctx, (t[0] as String).uppercase(), 11f, Ui007.GREY, true).apply { setPadding(0, dp(ctx, 10), 0, 0) })
                c.addView(Ui007.text(ctx, t[1] as String, 26f, Ui007.INK, true))
                c.addView(Ui007.pill(ctx, t[2] as String, t[4] as Int, t[5] as Int).apply { (layoutParams as? LinearLayout.LayoutParams)?.topMargin = dp(ctx, 4) })
                fila.addView(c)
            }
            body.addView(fila)
        }

        // Apartados (uno a la vez).
        val tabs = Ui007.row(ctx)
        listOf("dia" to "Por día", "estados" to "Estados", "tiempos" to "Tiempos", "alertas" to "Alertas", "negocios" to "Negocios").forEach { (k, l) ->
            tabs.addView(Ui007.chip(ctx, l, apartado == k) { apartado = k; repintar() }.apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(ctx, 8) })
        }
        body.addView(HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; setPadding(0, dp(ctx, 16), 0, dp(ctx, 4)); addView(tabs) })
        val c = Ui007.card(ctx)
        when (apartado) {
            "dia" -> apartadoDia(c)
            "estados" -> apartadoEstados(c)
            "tiempos" -> apartadoTiempos(c)
            "alertas" -> apartadoAlertas(c)
            else -> apartadoNegocios(c)
        }
        body.addView(c)

        avanceDelDia()
        equipo()
        porValidarPrimero()
        actividad()
        adminExtras()
    }

    @Suppress("UNCHECKED_CAST")
    private fun apartadoDia(c: LinearLayout) {
        val ctx = requireContext()
        val pd = datos["porDia"] as? Map<String, Any?> ?: return
        val unidad = when (pd["unidad"]) { "hora" -> "por hora"; "dia" -> "por día"; "semana" -> "por semana"; "mes" -> "por mes"; else -> "por año" }
        c.addView(Ui007.text(ctx, "Minutas terminadas $unidad", 15f, Ui007.INK, true))
        c.addView(Ui007.text(ctx, "Total ${num(pd, "total")} · promedio ${pd["promedio"]} por barra", 12f, Ui007.GREY).apply { setPadding(0, 0, 0, dp(ctx, 8)) })
        val barras = pd["barras"] as? List<Map<String, Any?>> ?: emptyList()
        val max = (barras.maxOfOrNull { num(it, "cantidad") } ?: 1L).coerceAtLeast(1L)
        barras.forEach { b ->
            val n = num(b, "cantidad")
            val f = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 3), 0, dp(ctx, 3)) }
            f.addView(Ui007.text(ctx, b["etiqueta"].toString(), 12f, Ui007.GREY), LinearLayout.LayoutParams(dp(ctx, 58), LinearLayout.LayoutParams.WRAP_CONTENT))
            f.addView(View(ctx).apply { background = Ui007.round(if (n > 0) Ui007.BLUE else 0xFFE3E8EF.toInt(), 4, ctx) }, LinearLayout.LayoutParams(0, dp(ctx, 12), (n.toFloat() / max).coerceAtLeast(0.02f)))
            f.addView(View(ctx), LinearLayout.LayoutParams(0, dp(ctx, 12), (1f - n.toFloat() / max).coerceAtLeast(0.001f)))
            f.addView(Ui007.text(ctx, n.toString(), 12f, Ui007.INK, true).apply { setPadding(dp(ctx, 8), 0, 0, 0) })
            c.addView(f)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun apartadoEstados(c: LinearLayout) {
        val ctx = requireContext()
        val total = num(datos["indicadores"] as? Map<String, Any?>, "total")
        c.addView(Ui007.text(ctx, "Minutas por estado · total $total", 15f, Ui007.INK, true))
        val colores = mapOf("Pendiente" to 0xFF0E5E63.toInt(), "Completa" to 0xFF1A4FA8.toInt(), "En revisión" to 0xFF6A3D9A.toInt(), "Validada" to 0xFF16663A.toInt())
        (datos["estados"] as? List<Map<String, Any?>>).orEmpty().forEach { e ->
            val f = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 8), 0, 0) }
            f.addView(View(ctx).apply { background = Ui007.round(colores[e["estado"]] ?: Ui007.BLUE, 6, ctx) }, LinearLayout.LayoutParams(dp(ctx, 12), dp(ctx, 12)).apply { marginEnd = dp(ctx, 10) })
            f.addView(Ui007.text(ctx, if (e["estado"] == "Completa") "Completa · por validar" else e["estado"].toString(), 14f), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            f.addView(Ui007.text(ctx, "${num(e, "cantidad")} · ${num(e, "pct")} %", 14f, Ui007.INK, true))
            c.addView(f)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun apartadoTiempos(c: LinearLayout) {
        val ctx = requireContext()
        val t = datos["tiempos"] as? Map<String, Any?> ?: return
        c.addView(Ui007.text(ctx, "Tiempo por minuta: mes anterior vs mes actual", 15f, Ui007.INK, true))
        val comp = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        c.addView(comp)
        comp.addView(Ui007.text(ctx, "Cargando…", 13f, Ui007.GREY))
        viewLifecycleOwner.lifecycleScope.launch {
            val r = runCatching { repo.tiempos() }.getOrNull()
            if (context == null) return@launch
            comp.removeAllViews()
            if (r == null) { comp.addView(Ui007.text(ctx, "No se pudo cargar la comparación.", 13f, Ui007.GREY)); return@launch }
            val manual = r["anteriorOrigen"] == "manual"
            comp.addView(Ui007.text(ctx, "${r["anteriorNombre"]} (${if (manual) "antes de la app, manual" else "con la app"}) → ${r["mesNombre"]} (con la app, en curso)", 12.5f, Ui007.GREY).apply { setPadding(0, dp(ctx, 2), 0, dp(ctx, 4)) })
            val filas = r["rubros"] as? List<Map<String, Any?>> ?: emptyList()
            if (filas.isEmpty()) comp.addView(Ui007.text(ctx, "Aún no hay minutas con tiempo este mes.", 13f, Ui007.GREY))
            filas.forEach { f ->
                val a = (f["anteriorMin"] as? Number)?.toLong(); val m = (f["actualMin"] as? Number)?.toLong(); val pct = (f["mejoraPct"] as? Number)?.toInt()
                val fila = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 6), 0, dp(ctx, 2)) }
                fila.addView(Ui007.text(ctx, "${f["rubro"]}\n${if (a == null) "—" else Ui007.fmtMin(a)} → ${if (m == null) "—" else Ui007.fmtMin(m)}", 13.5f, Ui007.INK), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                if (pct != null) fila.addView(Ui007.pill(ctx, (if (pct >= 0) "−$pct %" else "+${-pct} %"), if (pct >= 0) 0xFFE5F5EA.toInt() else 0xFFFBE4E2.toInt(), if (pct >= 0) 0xFF16663A.toInt() else 0xFF9E2B23.toInt()))
                comp.addView(fila)
            }
            (r["mejoraPromedioPct"] as? Number)?.let { comp.addView(Ui007.text(ctx, "Mejora promedio por minuta: $it %", 14f, 0xFF16663A.toInt(), true).apply { setPadding(0, dp(ctx, 10), 0, 0) }) }
            comp.addView(Ui007.text(ctx, "Ahorro estimado: ${"%.1f".format((r["horasAhorradas"] as? Number)?.toDouble() ?: 0.0)} h · S/ ${"%.0f".format((r["solesAhorro"] as? Number)?.toDouble() ?: 0.0)} (S/ ${"%.0f".format((r["costoHora"] as? Number)?.toDouble() ?: 25.0)} por hora)", 14f, 0xFF16663A.toInt(), true).apply { setPadding(0, dp(ctx, 4), 0, 0) })
            if (manual) comp.addView(Ui007.text(ctx, "El mes anterior usa los tiempos manuales de «Editar plazos»: así se trabajaba antes de la app.", 11.5f, Ui007.GREY).apply { setPadding(0, dp(ctx, 6), 0, 0) })
        }
        val rubros = t["rubros"] as? List<Map<String, Any?>> ?: emptyList()
        c.addView(Ui007.text(ctx, "En el periodo elegido", 13f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 14), 0, 0) })
        if (rubros.isEmpty()) c.addView(Ui007.text(ctx, "Aún no hay minutas en este periodo.", 13f, Ui007.GREY))
        rubros.forEach { r ->
            val min = (r["minutosPromedio"] as? Number)?.toLong()
            c.addView(Ui007.text(ctx, "${r["rubro"]} · ${num(r, "minutas")} minutas · ${if (min == null) "sin tiempo registrado" else Ui007.fmtMin(min)}", 13f, Ui007.INK).apply { setPadding(0, dp(ctx, 4), 0, 0) })
        }
        if (extras["porSupervisor"] != null) c.addView(Ui007.button(ctx, "Editar plazos", false) { editarPlazos() })
    }

    /** «Editar plazos» (solo administrador): minutos que tomaba cada tipo de local antes de la app y costo por hora del técnico. */
    @Suppress("UNCHECKED_CAST")
    private fun editarPlazos() {
        val ctx = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            val p = runCatching { repo.plazos() }.getOrNull() ?: return@launch Toast.makeText(ctx, "No se pudieron cargar los plazos.", Toast.LENGTH_LONG).show()
            val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
            val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 18), dp(ctx, 18), dp(ctx, 18), dp(ctx, 22)) }
            col.addView(Ui007.text(ctx, "Editar plazos", 18f, Ui007.INK, true))
            col.addView(Ui007.text(ctx, "Minutos que tomaba una minuta antes de la app, por tipo de local. Alimentan la comparación con el mes anterior.", 12.5f, Ui007.GREY).apply { setPadding(0, dp(ctx, 2), 0, dp(ctx, 8)) })
            val campos = linkedMapOf<String, android.widget.EditText>()
            fun campo(valor: String, hint: String) = android.widget.EditText(ctx).apply {
                setText(valor); this.hint = hint; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                background = Ui007.round(Ui007.BG, 12, ctx, Ui007.LINE); setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8)); textSize = 14f
            }
            (p["rubros"] as? Map<String, Any?>).orEmpty().forEach { (k, v) ->
                col.addView(Ui007.text(ctx, "$k (minutos)", 12.5f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 8), 0, dp(ctx, 2)) })
                val e = campo(((v as? Number)?.toInt() ?: 0).toString(), "minutos"); campos[k] = e; col.addView(e)
            }
            col.addView(Ui007.text(ctx, "Costo por hora del técnico (S/)", 12.5f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 10), 0, dp(ctx, 2)) })
            val costo = campo(((p["costoHora"] as? Number)?.toDouble() ?: 25.0).let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }, "S/ por hora")
            col.addView(costo)
            col.addView(Ui007.button(ctx, "Guardar plazos", true) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val rub = campos.mapValues { it.value.text.toString().trim().toDoubleOrNull()?.toInt() ?: 0 }
                    val r = runCatching { repo.plazosGuardar(mapOf("rubros" to rub, "costoHora" to (costo.text.toString().trim().toDoubleOrNull() ?: 0.0))) }
                    r.onSuccess { sheet.dismiss(); Toast.makeText(ctx, "Plazos guardados.", Toast.LENGTH_SHORT).show(); cargar() }
                        .onFailure { Toast.makeText(ctx, it.toFriendlyMessage(), Toast.LENGTH_LONG).show() }
                }
            })
            sheet.setContentView(ScrollView(ctx).apply { addView(col) })
            sheet.show()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun apartadoAlertas(c: LinearLayout) {
        val ctx = requireContext()
        c.addView(Ui007.text(ctx, "Indicadores que alertan", 15f, Ui007.INK, true))
        c.addView(Ui007.text(ctx, "Toca uno para ver el detalle con el Asistente IA.", 12f, Ui007.GREY))
        (datos["alertas"] as? List<Map<String, Any?>>).orEmpty().forEach { a ->
            val n = num(a, "cantidad")
            val f = Ui007.row(ctx).apply {
                setPadding(dp(ctx, 12), dp(ctx, 12), dp(ctx, 12), dp(ctx, 12)); background = Ui007.round(if (n > 0) 0xFFFFF3E8.toInt() else 0xFFF1F6EF.toInt(), 14, ctx)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 10) }
                setOnClickListener { findNavController().navigate(R.id.asistenteFragment, bundleOf("consulta" to a["consulta"].toString())) }
            }
            f.addView(Ui007.text(ctx, a["texto"].toString(), 14f, Ui007.INK), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            f.addView(Ui007.text(ctx, "$n  ›", 18f, if (n > 0) 0xFFB45309.toInt() else 0xFF16663A.toInt(), true))
            c.addView(f)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun apartadoNegocios(c: LinearLayout) {
        val ctx = requireContext()
        c.addView(Ui007.text(ctx, "Locales por tipo de negocio", 15f, Ui007.INK, true))
        val l = datos["negocios"] as? List<Map<String, Any?>> ?: emptyList()
        if (l.isEmpty()) c.addView(Ui007.text(ctx, "Aún no hay minutas en este periodo.", 13f, Ui007.GREY))
        l.forEach { n ->
            val f = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 6), 0, 0) }
            f.addView(Ui007.text(ctx, n["rubro"].toString(), 14f), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            f.addView(Ui007.text(ctx, num(n, "cantidad").toString(), 14f, Ui007.INK, true))
            c.addView(f)
        }
    }

    /** Avance y tiempos del equipo: por técnico, minutas por estado y horas promedio por minuta frente a la meta de 2 h. */
    /** «Avance · meta: N por técnico al día hábil»: barra y «Meta cumplida / Faltan» por técnico (QA G6). */
    @Suppress("UNCHECKED_CAST")
    private fun avanceDelDia() {
        val ctx = requireContext()
        val l = extras["avanceHoy"] as? List<Map<String, Any?>> ?: return
        val meta = num(extras, "metaDiaria").toInt().coerceAtLeast(1)
        body.addView(titulo("Avance · meta: $meta por técnico al día hábil"))
        val c = Ui007.card(ctx)
        if (l.isEmpty()) c.addView(Ui007.text(ctx, "No hay técnicos a tu cargo.", 14f, Ui007.GREY))
        l.forEach { t ->
            val hechas = num(t, "terminadas").toInt(); val curso = num(t, "enCurso").toInt()
            val cumplida = hechas >= meta
            val fila = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(ctx, 8), 0, dp(ctx, 6)) }
            val cab = Ui007.row(ctx)
            cab.addView(Ui007.text(ctx, t["tecnico"].toString(), 14f, Ui007.INK, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            cab.addView(Ui007.text(ctx, if (cumplida) "Meta cumplida $hechas/$meta" else "Faltan ${meta - hechas}" + if (curso > 0) " · $curso en curso" else "", 12f,
                if (cumplida) 0xFF2E7D32.toInt() else 0xFFB45309.toInt(), true))
            fila.addView(cab)
            fila.addView(ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = meta; progress = hechas.coerceAtMost(meta)
                progressTintList = android.content.res.ColorStateList.valueOf(if (cumplida) 0xFF2E7D32.toInt() else Ui007.BLUE)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 8)).apply { topMargin = dp(ctx, 6) })
            c.addView(fila)
        }
        body.addView(c)
    }

    private fun equipo() {
        val ctx = requireContext()
        body.addView(titulo("Avance y tiempos del equipo"))
        val c = Ui007.card(ctx)
        val l = datos["equipo"] as? List<Map<String, Any?>> ?: emptyList()
        val meta = (datos["metaHorasTecnico"] as? Number)?.toDouble() ?: 2.0
        if (l.isEmpty()) c.addView(Ui007.text(ctx, "No hay técnicos a tu cargo.", 14f, Ui007.GREY))
        l.forEach { t ->
            val h = (t["horasPromedio"] as? Number)?.toDouble()
            val fila = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL; setPadding(0, dp(ctx, 8), 0, dp(ctx, 8))
                setOnClickListener { findNavController().navigate(R.id.historialFragment, bundleOf("tab" to "general", "tecnico" to t["tecnico"].toString())) }
            }
            fila.addView(Ui007.text(ctx, "${t["tecnico"]}  ›", 14f, Ui007.INK, true))
            fila.addView(Ui007.text(ctx, "Pendientes ${num(t, "pendiente")} · Por validar ${num(t, "completa")} · En revisión ${num(t, "enRevision")} · Validadas ${num(t, "validada")}", 12f, Ui007.GREY))
            fila.addView(Ui007.text(ctx, if (h == null) "Sin tiempo registrado (meta $meta h)" else "$h h por minuta (meta $meta h)", 12f, if (h != null && h > meta) 0xFFB45309.toInt() else 0xFF16663A.toInt()))
            c.addView(fila)
        }
        c.addView(Ui007.button(ctx, "Reporte y mora", false) { findNavController().navigate(R.id.historialFragment, bundleOf("tab" to "tecnicos")) })
        body.addView(c)
    }

    @Suppress("UNCHECKED_CAST")
    private fun porValidarPrimero() {
        val ctx = requireContext()
        val l = datos["porValidarPrimero"] as? List<Map<String, Any?>> ?: emptyList()
        body.addView(titulo("Por validar primero"))
        val c = Ui007.card(ctx)
        if (l.isEmpty()) c.addView(Ui007.text(ctx, "No hay minutas por validar.", 14f, Ui007.GREY))
        c.addView(Ui007.text(ctx, "Menor puntaje = más riesgo.", 12f, Ui007.GREY))
        l.forEach { m ->
            val p = num(m, "puntaje")
            val f = Ui007.row(ctx).apply {
                setPadding(0, dp(ctx, 10), 0, 0)
                setOnClickListener {
                    val ev = num(m, "evaluationId")
                    if (ev > 0) findNavController().navigate(R.id.minutaProposalFragment, bundleOf("evaluationId" to ev.toString(), "minutaId" to num(m, "id")))
                    else com.upc.asistenteredidbi.presentation.common.HojaMinuta.abrir(this@PanelFragment, api007, num(m, "id").toLong())
                }
            }
            f.addView(Ui007.text(ctx, "$p %", 13f, if (p < 60) 0xFFC62828.toInt() else 0xFFB45309.toInt(), true), LinearLayout.LayoutParams(dp(ctx, 52), LinearLayout.LayoutParams.WRAP_CONTENT))
            f.addView(Ui007.text(ctx, "${m["local"]} · ${m["tecnico"]}", 14f, Ui007.INK), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            f.addView(Ui007.text(ctx, "›", 18f, Ui007.GREY))
            c.addView(f)
        }
        c.addView(Ui007.button(ctx, "Ver todas", false) { findNavController().navigate(R.id.historialFragment, bundleOf("tab" to "general")) })
        body.addView(c)
    }

    @Suppress("UNCHECKED_CAST")
    private fun actividad() {
        val ctx = requireContext()
        val l = datos["actividad"] as? List<Map<String, Any?>> ?: emptyList()
        body.addView(titulo("Actividad reciente"))
        val c = Ui007.card(ctx)
        if (l.isEmpty()) c.addView(Ui007.text(ctx, "Todavía no hay movimientos.", 14f, Ui007.GREY))
        l.forEach { a -> c.addView(Ui007.text(ctx, "${a["ts"]} · ${a["usuario"]}\n${a["accion"]}", 12.5f, Ui007.INK).apply { setPadding(0, dp(ctx, 6), 0, dp(ctx, 2)) }) }
        c.addView(Ui007.button(ctx, "Ver toda la actividad", false) { toda() })
        body.addView(c)
    }

    /** «Ver toda la actividad»: la bitácora completa de la empresa. */
    private fun toda() {
        val ctx = requireContext()
        viewLifecycleOwner.lifecycleScope.launch {
            val l = runCatching { api007.bitacora() }.getOrNull()
            if (l == null) { android.widget.Toast.makeText(ctx, "No se pudo abrir la actividad.", android.widget.Toast.LENGTH_SHORT).show(); return@launch }
            val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
            val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 18), dp(ctx, 18), dp(ctx, 18), dp(ctx, 24)) }
            col.addView(Ui007.text(ctx, "Actividad de la empresa", 18f, Ui007.INK, true))
            if (l.isEmpty()) col.addView(Ui007.text(ctx, "Todavía no hay movimientos.", 14f, Ui007.GREY))
            l.take(200).forEach { a -> col.addView(Ui007.text(ctx, "${a["ts"] ?: a["fecha"] ?: ""} · ${a["usuario"] ?: a["usuarioNombre"] ?: ""}\n${a["accion"] ?: a["detalle"] ?: ""}", 12.5f, Ui007.INK).apply { setPadding(0, dp(ctx, 8), 0, 0) }) }
            sheet.setContentView(ScrollView(ctx).apply { addView(col) })
            sheet.show()
        }
    }

    /** Solo administrador (vienen del resumen de la 0.0.7): por supervisor y plan y usuarios. */
    @Suppress("UNCHECKED_CAST")
    private fun adminExtras() {
        val ctx = requireContext()
        val sup = extras["porSupervisor"] as? List<Map<String, Any?>> ?: return
        body.addView(titulo("Supervisores · horas promedio en validar (meta 24 h)"))
        val c = Ui007.card(ctx)
        if (sup.isEmpty()) c.addView(Ui007.text(ctx, "Aún no hay supervisores.", 14f, Ui007.GREY))
        sup.forEach { s ->
            val f = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 6), 0, dp(ctx, 6)) }
            val h = (s["horasEspera"] as? Number)?.toDouble()
            f.addView(Ui007.text(ctx, s["supervisor"].toString(), 14f, Ui007.INK, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            f.addView(Ui007.text(ctx, "${num(s, "porValidar")} por validar · " + (h?.let { "$it h" } ?: "—"), 13f, if (h != null && h > 24) 0xFFB45309.toInt() else Ui007.GREY))
            c.addView(f)
        }
        body.addView(c)
    }

    private fun elegirRango() {
        val ctx = requireContext()
        DatePickerDialog(ctx, { _, y, m, d ->
            desde = LocalDate.of(y, m + 1, d)
            DatePickerDialog(ctx, { _, y2, m2, d2 -> hasta = LocalDate.of(y2, m2 + 1, d2); periodo = "rango"; cargar() }, y, m, d).apply { setTitle("Hasta"); show() }
        }, desde.year, desde.monthValue - 1, desde.dayOfMonth).apply { setTitle("Desde"); show() }
    }

    private fun fmt(v: Any?): String = runCatching { LocalDate.parse(v.toString()).let { "%02d/%02d/%d".format(it.dayOfMonth, it.monthValue, it.year) } }.getOrDefault("")

    private fun titulo(t: String) = Ui007.text(requireContext(), t, 16f, Ui007.INK, true).apply { setPadding(0, dp(requireContext(), 18), 0, dp(requireContext(), 4)) }
}
