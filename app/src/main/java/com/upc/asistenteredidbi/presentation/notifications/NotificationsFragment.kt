package com.upc.asistenteredidbi.presentation.notifications

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.data.remote.MinutaApiService
import com.upc.asistenteredidbi.data.remote.NotificationApiService
import com.upc.asistenteredidbi.data.remote.RejectMinutaRequest
import com.upc.asistenteredidbi.presentation.reportes.Ui007
import com.upc.asistenteredidbi.presentation.reportes.Ui007.dp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Notificaciones por rol (doc 0.0.7 §44). Supervisor y administrador ven quién envió cada minuta, el local, el puntaje y la hora, con
 * ver minuta, ver topología, ✓ validar y ✕ rechazar. Filtros con cantidad (Todas · Completas · En revisión · Validadas) y orden por
 * criticidad o por fecha. El técnico ve el estado de sus minutas. Los demás avisos (recordatorios, por vencer, códigos) van en «Otros avisos».
 */
@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    @Inject lateinit var api: NotificationApiService
    @Inject lateinit var minutaApi: MinutaApiService

    private lateinit var cuerpo: LinearLayout
    private var datos: Map<String, Any?> = emptyMap()
    private var filtro = "ALL"
    private var orden = "crit"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui007.BG) }
        root.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Ui007.BLUE); setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 16))
            addView(TextView(ctx).apply { text = "←"; textSize = 26f; setTextColor(Color.WHITE); setPadding(0, 0, dp(ctx, 16), 0); setOnClickListener { findNavController().navigateUp() } })
            addView(TextView(ctx).apply { text = "Notificaciones"; textSize = 22f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        })
        cuerpo = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 16), dp(ctx, 12), dp(ctx, 16), dp(ctx, 24)) }
        root.addView(ScrollView(ctx).apply { addView(cuerpo) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = cargar(marcarLeidas = true)

    private fun cargar(marcarLeidas: Boolean = false) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { api.bandeja() }
                .onSuccess { datos = it; pintar(); if (marcarLeidas) runCatching { api.markAllRead() } }
                .onFailure { Toast.makeText(requireContext(), "No se pudieron cargar las notificaciones", Toast.LENGTH_SHORT).show() }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun pintar() {
        val ctx = context ?: return
        cuerpo.removeAllViews()
        val rol = datos["rol"]?.toString().orEmpty()
        val tecnico = rol == "TECNICO"
        cuerpo.addView(Ui007.text(ctx, datos["descripcion"]?.toString().orEmpty(), 13f, Ui007.GREY))

        // Filtros con cantidad.
        val cnt = datos["conteos"] as? Map<String, Any?> ?: emptyMap()
        val n = { k: String -> (cnt[k] as? Number)?.toInt() ?: 0 }
        val filtros = listOf("ALL" to "Todas (${n("ALL")})", "Completa" to "Completas · por validar (${n("Completa")})",
            "En revisión" to "En revisión (${n("En revisión")})", "Validada" to "Validadas (${n("Validada")})")
        val fila = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 10), 0, 0) }
        filtros.forEach { (k, l) -> fila.addView(Ui007.chip(ctx, l, filtro == k) { filtro = k; pintar() }.apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(ctx, 8) }) }
        cuerpo.addView(HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; addView(fila) })

        val todas = (datos["minutas"] as? List<Map<String, Any?>>).orEmpty()
        val lista = todas.filter { filtro == "ALL" || it["estado"] == filtro }.let { l ->
            if (orden == "crit") l.sortedWith(compareBy<Map<String, Any?>> { (it["puntaje"] as? Number)?.toInt() ?: 0 }.thenByDescending { "${it["fecha"]}${it["hora"]}" })
            else l.sortedByDescending { f -> f["fecha"].toString().split("/").reversed().joinToString("") + f["hora"] }
        }

        val ord = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 10), 0, 0) }
        ord.addView(Ui007.text(ctx, "${lista.size} ${if (lista.size == 1) "minuta" else "minutas"}", 12.5f, Ui007.GREY), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        listOf("crit" to "Por criticidad", "fecha" to "Por fecha").forEach { (k, l) -> ord.addView(Ui007.chip(ctx, l, orden == k) { orden = k; pintar() }.apply { (layoutParams as LinearLayout.LayoutParams).marginStart = dp(ctx, 6) }) }
        cuerpo.addView(ord)
        cuerpo.addView(Ui007.text(ctx, if (orden == "crit") "Primero las de menor puntaje (más riesgo)." else "Primero las más recientes.", 12f, Ui007.GREY))

        if (lista.isEmpty()) {
            cuerpo.addView(Ui007.text(ctx, "No hay minutas ${when (filtro) { "ALL" -> ""; "Completa" -> "por validar"; "En revisión" -> "en revisión"; else -> "validadas" }}.", 14f, Ui007.GREY).apply { setPadding(0, dp(ctx, 12), 0, 0) })
        }
        lista.forEach { cuerpo.addView(filaMinuta(it, tecnico)) }

        // Otros avisos (recordatorios, por vencer, códigos de contraseña…).
        val otros = (datos["otros"] as? List<Map<String, Any?>>).orEmpty()
        if (otros.isNotEmpty()) {
            cuerpo.addView(Ui007.text(ctx, "Otros avisos", 15f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 18), 0, dp(ctx, 2)) })
            otros.forEach { o ->
                val c = Ui007.row(ctx).apply {
                    background = Ui007.round(if (o["leida"] == true) Color.WHITE else 0xFFEAF2FD.toInt(), 16, ctx, 0xFFD6E4FA.toInt())
                    setPadding(dp(ctx, 12), dp(ctx, 12), dp(ctx, 12), dp(ctx, 12))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 8) }
                }
                c.addView(Ui007.text(ctx, "🔔", 18f).apply { gravity = Gravity.CENTER; background = Ui007.round(0xFFE3F2FD.toInt(), 22, ctx); layoutParams = LinearLayout.LayoutParams(dp(ctx, 40), dp(ctx, 40)) })
                val tx = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 12), 0, 0, 0) }
                tx.addView(Ui007.text(ctx, o["texto"]?.toString().orEmpty(), 14f, Ui007.INK))
                (o["fecha"] as? String)?.take(16)?.replace('T', ' ')?.takeIf { it.isNotBlank() }?.let { tx.addView(Ui007.text(ctx, it, 11.5f, Ui007.GREY)) }
                c.addView(tx, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                (o["evaluationId"] as? Number)?.toLong()?.let { ev ->
                    c.setOnClickListener {
                        // El recordatorio de «termina la minuta» retoma el Chat IA donde se quedó; los demás abren la propuesta.
                        val esBorrador = o["tipo"] == "RECORDATORIO"
                        findNavController().navigate(if (esBorrador) R.id.technicalChatFragment else R.id.minutaProposalFragment,
                            bundleOf("evaluationId" to ev.toString(), "minutaId" to ((o["minutaId"] as? Number)?.toLong() ?: -1L)))
                    }
                }
                cuerpo.addView(c)
            }
        }
    }

    private fun filaMinuta(m: Map<String, Any?>, tecnico: Boolean): View {
        val ctx = requireContext()
        val p = (m["puntaje"] as? Number)?.toInt() ?: 0
        val (bg, fg) = when { p < 60 -> 0xFFFDECEC.toInt() to 0xFFC62828.toInt(); p < 80 -> 0xFFFFF3E0.toInt() to 0xFFB45309.toInt(); else -> 0xFFE8F5E9.toInt() to 0xFF2E7D32.toInt() }
        val estado = m["estado"]?.toString().orEmpty()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui007.round(if (estado == "Completa") 0xFFF1F6FE.toInt() else Color.WHITE, 16, ctx, Ui007.LINE)
            setPadding(dp(ctx, 12), dp(ctx, 12), dp(ctx, 12), dp(ctx, 12))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 10) }
        }
        val top = Ui007.row(ctx)
        top.addView(Ui007.text(ctx, "$p%", 13f, fg, true).apply { gravity = Gravity.CENTER; background = Ui007.round(bg, 22, ctx); layoutParams = LinearLayout.LayoutParams(dp(ctx, 46), dp(ctx, 46)) })
        val tx = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 12), 0, 0, 0) }
        val titulo = if (tecnico) "Tu minuta de ${m["local"]}" else "${m["tecnico"]} envió la minuta de ${m["local"]}"
        tx.addView(Ui007.text(ctx, titulo, 14f, Ui007.INK, true))
        tx.addView(Ui007.text(ctx, "Puntaje $p % · ${m["fecha"].toString().take(5)} ${m["hora"]}", 12f, Ui007.GREY))
        top.addView(tx, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(top)

        val acc = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 10), 0, 0) }
        val id = (m["id"] as Number).toLong()
        val ev = (m["evaluationId"] as? Number)?.toLong()
        fun icono(glifo: String, desc: String, fondo: Int, color: Int, habilitado: Boolean = true, onClick: () -> Unit) {
            acc.addView(Ui007.text(ctx, glifo, 17f, color, true).apply {
                gravity = Gravity.CENTER; contentDescription = desc; alpha = if (habilitado) 1f else 0.35f
                background = Ui007.round(fondo, 12, ctx, if (fondo == Color.WHITE) Ui007.LINE else null)
                layoutParams = LinearLayout.LayoutParams(dp(ctx, 44), dp(ctx, 40)).apply { marginEnd = dp(ctx, 8) }
                setOnClickListener { if (habilitado) onClick() }
            })
        }
        val abrir = { if (ev != null) findNavController().navigate(R.id.minutaProposalFragment, bundleOf("evaluationId" to ev.toString(), "minutaId" to id))
            else Toast.makeText(ctx, "Esta minuta se redactó con el Asistente IA; ábrela desde el Asistente IA.", Toast.LENGTH_SHORT).show() }
        icono("👁", "Ver minuta", Color.WHITE, Ui007.BLUE) { abrir() }
        icono("⛓", "Ver topología", Color.WHITE, Ui007.BLUE, m["topologia"] == true) { abrir() }
        if (estado == "Completa" && !tecnico) {
            icono("✓", "Validar", 0xFF2E7D32.toInt(), Color.WHITE) { validar(id) }
            icono("✕", "Rechazar", Color.WHITE, 0xFFC62828.toInt()) { rechazar(id, m["local"].toString()) }
        } else {
            val (pbg, pfg) = Ui007.estadoColors(estado)
            acc.addView(Ui007.pill(ctx, if (tecnico && estado == "Completa") "Esperando validación" else estado, pbg, pfg))
        }
        card.addView(acc)
        return card
    }

    private fun validar(id: Long) = viewLifecycleOwner.lifecycleScope.launch {
        runCatching { minutaApi.validateMinuta(id) }
            .onSuccess { Toast.makeText(requireContext(), "Minuta validada. Se avisó al técnico.", Toast.LENGTH_SHORT).show(); cargar() }
            .onFailure { Toast.makeText(requireContext(), "No se pudo validar la minuta.", Toast.LENGTH_SHORT).show() }
    }

    private fun rechazar(id: Long, local: String) {
        com.upc.asistenteredidbi.presentation.minutas.RejectMinutaDialog.show(requireContext(), local) { motivo ->
            viewLifecycleOwner.lifecycleScope.launch {
                runCatching { minutaApi.rejectMinuta(id, RejectMinutaRequest(motivo)) }
                    .onSuccess { Toast.makeText(requireContext(), "Minuta devuelta al técnico para corregir.", Toast.LENGTH_SHORT).show(); cargar() }
                    .onFailure { Toast.makeText(requireContext(), "No se pudo rechazar la minuta.", Toast.LENGTH_SHORT).show() }
            }
        }
    }
}
