package com.upc.asistenteredidbi.presentation.reportes

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.LinearLayout
import com.upc.asistenteredidbi.presentation.reportes.Ui007.dp
import org.json.JSONArray
import org.json.JSONObject

/**
 * Muestra en pantalla el documento de una minuta de soporte (bloques h2, h3, kv, para, ul, table, rules, exec) tal como
 * lo arma la API. Se usa en «Ver minuta» de Reportes y en la vista previa del Asistente IA.
 */
object DocBlocksView {

    fun render(ctx: Context, docJson: String?, into: LinearLayout) {
        into.removeAllViews()
        val doc = runCatching { JSONObject(docJson ?: "{}") }.getOrDefault(JSONObject())
        into.addView(Ui007.text(ctx, doc.optString("title", "Minuta de soporte"), 16f, Ui007.BLUE, true))
        if (doc.has("header")) into.addView(Ui007.text(ctx, doc.optString("header"), 12f, Ui007.GREY))
        val blocks = doc.optJSONArray("blocks") ?: JSONArray()
        for (i in 0 until blocks.length()) {
            val b = blocks.optJSONObject(i) ?: continue
            when (b.optString("t")) {
                "h2" -> into.addView(Ui007.text(ctx, "${b.optString("n")} ${b.optString("text")}".trim(), 15f, Ui007.BLUE, true).apply { setPadding(0, dp(ctx, 12), 0, dp(ctx, 4)) })
                "h3" -> into.addView(Ui007.text(ctx, b.optString("text"), 14f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 8), 0, dp(ctx, 2)) })
                "para" -> into.addView(Ui007.text(ctx, bold(b.optString("b"), b.optString("text")), 14f).apply { setPadding(0, dp(ctx, 3), 0, dp(ctx, 3)) })
                "ul" -> b.optJSONArray("items")?.let { a -> for (j in 0 until a.length()) a.optJSONObject(j)?.let { into.addView(Ui007.text(ctx, SpannableStringBuilder("• ").append(bold(it.optString("b"), it.optString("text"))), 14f)) } }
                "kv" -> b.optJSONArray("rows")?.let { a -> for (j in 0 until a.length()) a.optJSONObject(j)?.let { into.addView(Ui007.text(ctx, bold(it.optString("label") + ":", it.optString("value")), 13f)) } }
                "table" -> table(ctx, b, into)
                "rules" -> b.optJSONArray("rows")?.let { a ->
                    for (j in 0 until a.length()) a.optJSONObject(j)?.let {
                        val ok = it.optBoolean("ok", true)
                        into.addView(Ui007.text(ctx, (if (ok) "✓ " else "✗ ") + it.optString("rule") + " — " + it.optString("result"), 13f, if (ok) 0xFF16663A.toInt() else 0xFF9E2B23.toInt()))
                    }
                }
                "exec" -> {
                    val ok = b.optBoolean("ok")
                    val c = Ui007.card(ctx).apply { background = Ui007.round(if (ok) 0xFFDDF3E6.toInt() else 0xFFFDF0D8.toInt(), 12, ctx) }
                    c.addView(Ui007.text(ctx, b.optString("status"), 14f, if (ok) 0xFF16663A.toInt() else 0xFF7A4A00.toInt(), true))
                    c.addView(Ui007.text(ctx, b.optString("text"), 13f))
                    into.addView(c)
                }
            }
        }
    }

    private fun table(ctx: Context, b: JSONObject, into: LinearLayout) {
        val head = b.optJSONArray("head") ?: JSONArray()
        val rows = b.optJSONArray("rows") ?: JSONArray()
        for (r in 0 until rows.length()) {
            val row = rows.optJSONArray(r) ?: continue
            val sb = SpannableStringBuilder()
            for (c in 0 until row.length()) {
                if (c > 0) sb.append("\n")
                sb.append(bold(head.optString(c, "") + ":", row.optString(c, "")))
            }
            into.addView(Ui007.text(ctx, sb, 13f).apply {
                background = Ui007.round(Ui007.BG, 10, ctx, Ui007.LINE); setPadding(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 6) }
            })
        }
    }

    private fun bold(b: String, rest: String): CharSequence {
        if (b.isBlank()) return rest
        val sb = SpannableStringBuilder(b).append(" ").append(rest)
        sb.setSpan(StyleSpan(Typeface.BOLD), 0, b.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }
}
