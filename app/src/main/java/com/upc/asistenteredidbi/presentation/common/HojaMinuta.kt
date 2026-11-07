package com.upc.asistenteredidbi.presentation.common

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.data.remote.Api007Service
import com.upc.asistenteredidbi.presentation.reportes.Ui007
import com.upc.asistenteredidbi.presentation.reportes.Ui007.dp
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * «Ver minuta» en cualquier pantalla (QA F1). Una minuta con evaluación abre la propuesta de siempre; las que se redactaron
 * desde el Asistente IA no tienen evaluación y se muestran en una hoja con sus datos, conexiones, recomendaciones y fotos.
 */
object HojaMinuta {

    fun abrir(f: Fragment, api: Api007Service, minutaId: Long) {
        f.viewLifecycleOwner.lifecycleScope.launch {
            val ctx = f.context ?: return@launch
            val m = runCatching { api.minuta(minutaId) }.getOrNull()
            if (m == null) { Toast.makeText(ctx, "No pude abrir la minuta.", Toast.LENGTH_SHORT).show(); return@launch }
            val ev = (m["evaluationId"] as? Number)?.toLong()
            if (ev != null) {
                f.findNavController().navigate(R.id.minutaProposalFragment, Bundle().apply {
                    putString("evaluationId", ev.toString()); putLong("minutaId", minutaId)
                })
            } else mostrar(f, api, m)
        }
    }

    fun mostrar(f: Fragment, api: Api007Service, m: Map<String, Any?>) {
        val ctx = f.context ?: return
        val sheet = BottomSheetDialog(ctx)
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 18), dp(ctx, 18), dp(ctx, 18), dp(ctx, 20)) }
        col.addView(Ui007.text(ctx, m["clientName"]?.toString() ?: "Minuta", 18f, Ui007.INK, true))
        col.addView(Ui007.text(ctx, "Minuta v${(m["version"] as? Number)?.toInt() ?: 1} · ${m["status"] ?: ""}", 13f, Ui007.GREY).apply { setPadding(0, dp(ctx, 2), 0, dp(ctx, 8)) })
        val c = runCatching { JSONObject(m["contentJson"]?.toString() ?: "{}") }.getOrDefault(JSONObject())
        fun fila(k: String, v: String?) { if (!v.isNullOrBlank()) col.addView(Ui007.text(ctx, "$k: $v", 14f, Ui007.INK).apply { setPadding(0, dp(ctx, 6), 0, 0) }) }
        c.optJSONObject("datos")?.let { d ->
            fila("Razón social", d.optString("razon")); fila("RUC", d.optString("ruc")); fila("Dirección", d.optString("direccion"))
            fila("Encargado", d.optString("encargado")); fila("Celular", d.optString("celular")); fila("Rubro", d.optString("rubro")); fila("Internet", d.optString("internet"))
        }
        c.optJSONArray("conexiones")?.let { a ->
            if (a.length() > 0) {
                col.addView(Ui007.text(ctx, "Conexiones de red", 14f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 12), 0, 0) })
                for (i in 0 until a.length()) a.getJSONObject(i).let { col.addView(Ui007.text(ctx, "• ${it.optString("desde")} → ${it.optString("hacia")} · ${it.optString("medio")}", 13f, Ui007.INK)) }
            }
        }
        fila("Observaciones", c.optString("obsTec"))
        c.optJSONArray("recs")?.let { a -> for (i in 0 until a.length()) fila("Recomendación", a.optJSONObject(i)?.optString("t") ?: a.optString(i)) }
        c.optJSONArray("pasos")?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.let { p -> fila("Paso (${p.optString("p")})", p.optString("t")) } }
        c.optJSONArray("fotos")?.let { a ->
            if (a.length() > 0) {
                col.addView(Ui007.text(ctx, "Evidencias (${a.length()})", 14f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 12), 0, dp(ctx, 4)) })
                val id = (m["id"] as? Number)?.toLong()
                for (i in 0 until a.length()) {
                    val foto = a.getJSONObject(i)
                    col.addView(Ui007.text(ctx, foto.optString("tipo"), 12f, Ui007.GREY))
                    val img = android.widget.ImageView(ctx).apply {
                        adjustViewBounds = true; scaleType = android.widget.ImageView.ScaleType.FIT_CENTER; setBackgroundColor(0xFFEFF2F7.toInt())
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 180)).apply { bottomMargin = dp(ctx, 8) }
                    }
                    col.addView(img)
                    if (id != null) f.viewLifecycleOwner.lifecycleScope.launch {
                        runCatching { api.foto(id, foto.optString("archivo")).use { it.bytes() } }
                            .onSuccess { b -> android.graphics.BitmapFactory.decodeByteArray(b, 0, b.size)?.let { img.setImageBitmap(it) } }
                    }
                    foto.optString("descripcion").takeIf { it.isNotBlank() }?.let { col.addView(Ui007.text(ctx, "Descripción: $it", 12f, Ui007.GREY)) }
                }
            }
        }
        c.optJSONArray("evidenciasFaltantes")?.let { a ->
            if (a.length() > 0) col.addView(Ui007.text(ctx, "Evidencias por subir: " + (0 until a.length()).joinToString(", ") { a.optString(it) }, 13f, 0xFFB45309.toInt()).apply { setPadding(0, dp(ctx, 12), 0, 0) })
        }
        col.addView(Ui007.button(ctx, "Cerrar", false) { sheet.dismiss() })
        sheet.setContentView(ScrollView(ctx).apply { addView(col) })
        sheet.show()
    }
}
