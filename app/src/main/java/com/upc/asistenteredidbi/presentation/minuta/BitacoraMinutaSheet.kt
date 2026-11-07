package com.upc.asistenteredidbi.presentation.minuta

import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.upc.asistenteredidbi.data.remote.Api007Service
import com.upc.asistenteredidbi.presentation.reportes.Ui007
import com.upc.asistenteredidbi.presentation.reportes.Ui007.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * «Bitácora de la minuta» (doc 0.0.7 §24): resumen (creada por, con qué, estado, rechazada por y quién la corrigió, validada por),
 * quiénes la editaron en las últimas 48 horas e historial con quién, rol, fecha, hora y qué parte se cambió.
 */
object BitacoraMinutaSheet {

    fun mostrar(ctx: Context, scope: CoroutineScope, api: Api007Service, minutaId: Long) {
        scope.launch {
            val r = runCatching { api.bitacoraMinuta(minutaId) }.getOrNull()
            if (r == null) {
                Toast.makeText(ctx, "No se pudo abrir la bitácora.", Toast.LENGTH_LONG).show()
                return@launch
            }
            val sheet = BottomSheetDialog(ctx)
            val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 18), dp(ctx, 18), dp(ctx, 18), dp(ctx, 24)) }
            col.addView(Ui007.text(ctx, "Bitácora de la minuta", 18f, Ui007.INK, true))
            col.addView(Ui007.text(ctx, "Nada se borra: cada acción queda con quién, cuándo y qué parte cambió.", 12f, Ui007.GREY).apply { setPadding(0, dp(ctx, 2), 0, dp(ctx, 6)) })

            col.addView(Ui007.text(ctx, "RESUMEN", 11f, Ui007.GREY, true).apply { setPadding(0, dp(ctx, 10), 0, dp(ctx, 4)) })
            val resumen = Ui007.card(ctx)
            fun fila(t: String, v: Any?) { if (v != null && v.toString().isNotBlank()) resumen.addView(Ui007.text(ctx, "$t: $v", 13f, Ui007.INK).apply { setPadding(0, dp(ctx, 2), 0, dp(ctx, 2)) }) }
            fila("Creada por", listOfNotNull(r["creadaPor"], r["creadaEn"]).joinToString(" · "))
            fila("Con", r["herramienta"])
            fila("Estado actual", "${r["estado"] ?: "—"} · v${(r["version"] as? Number)?.toInt() ?: 1}")
            if (r["rechazadaPor"] != null) {
                fila("Rechazada por", listOfNotNull(r["rechazadaPor"], r["rechazadaEn"]).joinToString(" · "))
                fila("Motivo", r["motivo"])
                fila("Corregida por", if (r["corregidaPor"] != null) listOfNotNull(r["corregidaPor"], r["corregidaEn"]).joinToString(" · ") else "todavía no")
            }
            if (r["validadaPor"] != null) fila("Validada por", listOfNotNull(r["validadaPor"], r["validadaEn"]).joinToString(" · "))
            col.addView(resumen)

            col.addView(Ui007.text(ctx, "EDITARON EN LAS ÚLTIMAS 48 HORAS", 11f, Ui007.GREY, true).apply { setPadding(0, dp(ctx, 12), 0, dp(ctx, 4)) })
            val editores = (r["editores48h"] as? Map<*, *>).orEmpty()
            val ed = Ui007.card(ctx)
            if (editores.isEmpty()) ed.addView(Ui007.text(ctx, "Nadie la editó en las últimas 48 horas.", 13f, Ui007.GREY))
            editores.forEach { (u, n) ->
                val c = (n as? Number)?.toInt() ?: 1
                ed.addView(Ui007.text(ctx, "$u · $c ${if (c == 1) "cambio" else "cambios"}", 13f, Ui007.INK))
            }
            col.addView(ed)

            col.addView(Ui007.text(ctx, "HISTORIAL", 11f, Ui007.GREY, true).apply { setPadding(0, dp(ctx, 12), 0, dp(ctx, 4)) })
            val eventos = (r["eventos"] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()
            val hist = Ui007.card(ctx)
            if (eventos.isEmpty()) hist.addView(Ui007.text(ctx, "Todavía no hay movimientos.", 13f, Ui007.GREY))
            eventos.forEach { e ->
                hist.addView(Ui007.text(ctx, "${e["ts"]} · ${e["usuario"]} · ${e["rol"].toString().lowercase().replaceFirstChar { it.uppercase() }}\n${e["accion"]}", 12.5f, Ui007.INK).apply { setPadding(0, dp(ctx, 6), 0, dp(ctx, 4)) })
            }
            col.addView(hist)
            sheet.setContentView(ScrollView(ctx).apply { addView(col) })
            sheet.show()
        }
    }
}
