package com.upc.asistenteredidbi.presentation.gestion

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.upc.asistenteredidbi.data.repository.Asistente007Repository
import com.upc.asistenteredidbi.presentation.reportes.Ui007
import com.upc.asistenteredidbi.presentation.reportes.Ui007.dp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Panel de gestión (administrador y supervisor, doc 0.0.7 §14): rango, indicadores, avance de hoy por técnico,
 * minutas por estado y ahorro estimado. «Por supervisor» y «Plan y usuarios» solo salen para el administrador.
 */
@AndroidEntryPoint
class PanelFragment : Fragment() {

    @Inject lateinit var repo: Asistente007Repository
    private lateinit var body: LinearLayout
    private lateinit var progress: ProgressBar
    private var dias = 14

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
            val r = runCatching { repo.panel(dias) }
            progress.visibility = View.GONE
            if (context == null) return@launch
            r.onSuccess { render(it) }.onFailure {
                body.removeAllViews()
                body.addView(Ui007.text(requireContext(), "No se pudo cargar el Panel. Revisa tu conexión.", 15f, Ui007.GREY))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun render(d: Map<String, Any?>) {
        val ctx = requireContext()
        body.removeAllViews()
        body.addView(Ui007.text(ctx, "Panel", 24f, Ui007.INK, true))
        body.addView(Ui007.text(ctx, "Información para decidir: cuánto avanza tu equipo y qué alerta.", 13f, Ui007.GREY))

        val rango = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 12), 0, 0) }
        listOf(7, 14, 30).forEach { n -> rango.addView(Ui007.chip(ctx, "$n días", n == dias) { dias = n; load() }) }
        body.addView(rango)

        val kpi = d["kpi"] as? Map<String, Any?> ?: emptyMap()
        fun n(k: String) = (kpi[k] as? Number)?.toLong() ?: 0L
        val grid = listOf(
            "Minutas" to n("minutas").toString(), "Por validar" to n("porValidar").toString(),
            "Pendientes" to n("pendientes").toString(), "Rechazadas" to n("rechazadas").toString(),
            "Validadas" to n("validadas").toString(), "Horas ahorradas" to "%.1f".format((kpi["horasAhorradas"] as? Number)?.toDouble() ?: 0.0)
        )
        grid.chunked(2).forEach { par ->
            val fila = Ui007.row(ctx)
            par.forEach { (t, v) ->
                val c = Ui007.card(ctx).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { topMargin = dp(ctx, 10); marginEnd = dp(ctx, 8) } }
                c.addView(Ui007.text(ctx, v, 26f, Ui007.BLUE, true)); c.addView(Ui007.text(ctx, t, 12f, Ui007.GREY))
                fila.addView(c)
            }
            body.addView(fila)
        }

        // Avance de hoy por técnico frente a la meta diaria.
        val meta = (d["metaDiaria"] as? Number)?.toInt() ?: 3
        body.addView(titulo("Avance de hoy"))
        val avance = d["avanceHoy"] as? List<Map<String, Any?>> ?: emptyList()
        val cAv = Ui007.card(ctx)
        if (avance.isEmpty()) cAv.addView(Ui007.text(ctx, "No hay técnicos a tu cargo.", 14f, Ui007.GREY))
        avance.forEach { a ->
            val hechas = (a["terminadas"] as? Number)?.toInt() ?: 0
            val curso = (a["enCurso"] as? Number)?.toInt() ?: 0
            val fila = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 6), 0, dp(ctx, 6)) }
            fila.addView(Ui007.text(ctx, a["tecnico"].toString(), 14f, Ui007.INK, true).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
            fila.addView(Ui007.text(ctx, "$hechas de $meta" + if (curso > 0) " · $curso en curso" else "", 13f, if (hechas >= meta) 0xFF16663A.toInt() else Ui007.GREY))
            cAv.addView(fila)
            cAv.addView(ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply { max = meta; progress = hechas.coerceAtMost(meta) })
        }
        body.addView(cAv)

        body.addView(titulo("Minutas por estado"))
        val cEs = Ui007.card(ctx)
        (d["porEstado"] as? Map<String, Any?> ?: emptyMap()).forEach { (k, v) ->
            val f = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 4), 0, dp(ctx, 4)) }
            f.addView(Ui007.text(ctx, k, 14f).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
            f.addView(Ui007.text(ctx, v.toString().substringBefore('.'), 14f, Ui007.INK, true))
            cEs.addView(f)
        }
        body.addView(cEs)

        val ahorro = d["ahorro"] as? Map<String, Any?>
        if (ahorro != null) {
            body.addView(titulo("Ahorro estimado"))
            val c = Ui007.card(ctx)
            val soles = (ahorro["soles"] as? Number)?.toDouble() ?: 0.0
            c.addView(Ui007.text(ctx, "S/ %.0f".format(soles), 26f, 0xFF16663A.toInt(), true))
            c.addView(Ui007.text(ctx, "Horas ahorradas × S/ %.0f por hora de técnico.".format((ahorro["solesPorHora"] as? Number)?.toDouble() ?: 25.0), 12f, Ui007.GREY))
            body.addView(c)
        }

        // Solo administrador.
        val sup = d["porSupervisor"] as? List<Map<String, Any?>>
        if (sup != null) {
            body.addView(titulo("Por supervisor"))
            val c = Ui007.card(ctx)
            if (sup.isEmpty()) c.addView(Ui007.text(ctx, "Aún no hay supervisores.", 14f, Ui007.GREY))
            sup.forEach { s ->
                val f = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 6), 0, dp(ctx, 6)) }
                f.addView(Ui007.text(ctx, s["supervisor"].toString(), 14f, Ui007.INK, true).apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
                f.addView(Ui007.text(ctx, "${s["tecnicos"].toString().substringBefore('.')} técnicos · ${s["porValidar"].toString().substringBefore('.')} por validar", 13f, Ui007.GREY))
                c.addView(f)
            }
            body.addView(c)
            body.addView(titulo("Plan y usuarios"))
            val cu = Ui007.card(ctx)
            cu.addView(Ui007.text(ctx, "${d["usuarios"].toString().substringBefore('.')} usuarios en la empresa", 14f, Ui007.INK, true))
            cu.addView(Ui007.button(ctx, "Equipo y accesos", false) {
                androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(com.upc.asistenteredidbi.R.id.equipoFragment)
            })
            body.addView(cu)
        }
    }

    private fun titulo(t: String) = Ui007.text(requireContext(), t, 16f, Ui007.INK, true).apply { setPadding(0, dp(requireContext(), 18), 0, 0) }
}
