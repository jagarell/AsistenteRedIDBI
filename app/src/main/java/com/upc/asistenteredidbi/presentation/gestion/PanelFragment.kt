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
        body = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; val p = dp(ctx, 16); setPadding(p, p + dp(ctx, 28), p, dp(ctx, 32)) }
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
        val tarjetas = listOf(
            Triple("Total minutas", total.toString(), delta),
            Triple("Por validar", num(ind, "porValidar").toString(), "${num(ind, "porValidarRiesgo")} con riesgo"),
            Triple("Pendientes de cerrar", num(ind, "pendientes").toString(), "sin terminar o sin solicitar validación"),
            Triple("En revisión", num(ind, "enRevision").toString(), "devueltas al técnico"),
            Triple("Validadas", num(ind, "validadas").toString(), "${num(ind, "validadasPct")} % del total"),
            Triple("Horas ahorradas", "%.1f".format((ind?.get("horasAhorradas") as? Number)?.toDouble() ?: 0.0), "1,5 h menos por minuta")
        )
        tarjetas.chunked(2).forEach { par ->
            val fila = Ui007.row(ctx).apply { gravity = Gravity.TOP }
            par.forEach { (t, v, s) ->
                val c = Ui007.card(ctx).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { topMargin = dp(ctx, 10); marginEnd = dp(ctx, 8) } }
                c.addView(Ui007.text(ctx, t, 12f, Ui007.GREY)); c.addView(Ui007.text(ctx, v, 26f, Ui007.BLUE, true)); c.addView(Ui007.text(ctx, s, 11f, Ui007.GREY))
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
        c.addView(Ui007.text(ctx, "Tiempo por minuta por tipo de negocio", 15f, Ui007.INK, true))
        val rubros = t["rubros"] as? List<Map<String, Any?>> ?: emptyList()
        if (rubros.isEmpty()) c.addView(Ui007.text(ctx, "Aún no hay minutas en este periodo.", 13f, Ui007.GREY))
        rubros.forEach { r ->
            val min = (r["minutosPromedio"] as? Number)?.toLong()
            c.addView(Ui007.text(ctx, "${r["rubro"]} · ${num(r, "minutas")} minutas · ${if (min == null) "sin tiempo registrado" else Ui007.fmtMin(min)}", 13.5f, Ui007.INK).apply { setPadding(0, dp(ctx, 6), 0, 0) })
        }
        c.addView(Ui007.text(ctx, "Ahorro estimado: ${"%.1f".format((t["horasAhorradas"] as? Number)?.toDouble() ?: 0.0)} h · S/ ${"%.0f".format((t["solesAhorro"] as? Number)?.toDouble() ?: 0.0)} (S/ ${"%.0f".format((t["solesPorHora"] as? Number)?.toDouble() ?: 25.0)} por hora)", 14f, 0xFF16663A.toInt(), true).apply { setPadding(0, dp(ctx, 12), 0, 0) })
        c.addView(Ui007.text(ctx, "La comparación con el mes anterior usa los tiempos de «Editar plazos» (web), que aún no están en la app.", 11.5f, Ui007.GREY).apply { setPadding(0, dp(ctx, 6), 0, 0) })
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
    @Suppress("UNCHECKED_CAST")
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
                    else Toast.makeText(ctx, "Se abre desde Notificaciones o el Asistente IA.", Toast.LENGTH_SHORT).show()
                }
            }
            f.addView(Ui007.text(ctx, "$p %", 13f, if (p < 60) 0xFFC62828.toInt() else 0xFFB45309.toInt(), true), LinearLayout.LayoutParams(dp(ctx, 52), LinearLayout.LayoutParams.WRAP_CONTENT))
            f.addView(Ui007.text(ctx, "${m["local"]} · ${m["tecnico"]}", 14f, Ui007.INK), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            f.addView(Ui007.text(ctx, "›", 18f, Ui007.GREY))
            c.addView(f)
        }
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
        body.addView(c)
    }

    /** Solo administrador (vienen del resumen de la 0.0.7): por supervisor y plan y usuarios. */
    @Suppress("UNCHECKED_CAST")
    private fun adminExtras() {
        val ctx = requireContext()
        val sup = extras["porSupervisor"] as? List<Map<String, Any?>> ?: return
        body.addView(titulo("Por supervisor"))
        val c = Ui007.card(ctx)
        if (sup.isEmpty()) c.addView(Ui007.text(ctx, "Aún no hay supervisores.", 14f, Ui007.GREY))
        sup.forEach { s ->
            val f = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 6), 0, dp(ctx, 6)) }
            f.addView(Ui007.text(ctx, s["supervisor"].toString(), 14f, Ui007.INK, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            f.addView(Ui007.text(ctx, "${num(s, "tecnicos")} técnicos · ${num(s, "porValidar")} por validar", 13f, Ui007.GREY))
            c.addView(f)
        }
        body.addView(c)
        body.addView(titulo("Equipo y accesos"))
        val cu = Ui007.card(ctx)
        cu.addView(Ui007.text(ctx, "${num(extras, "usuarios")} usuarios en la empresa", 14f, Ui007.INK, true))
        cu.addView(Ui007.button(ctx, "Equipo y accesos", false) { findNavController().navigate(R.id.equipoFragment) })
        body.addView(cu)
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
