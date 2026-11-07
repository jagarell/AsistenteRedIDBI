package com.upc.asistenteredidbi.presentation.reportes

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Piezas de interfaz compartidas por Reportes y el Asistente IA 0.0.7 (vistas armadas en código, sin layouts nuevos). */
object Ui007 {
    const val BLUE = 0xFF1565C0.toInt()
    const val INK = 0xFF1F2937.toInt()
    const val GREY = 0xFF6B7280.toInt()
    const val LINE = 0xFFE3E8EF.toInt()
    const val BG = 0xFFF5F7FB.toInt()

    /** Ícono de línea del diseño (res/drawable/ic_ln_*), teñido con [color]. */
    fun icon(ctx: Context, res: Int, color: Int, sizeDp: Int = 22): android.widget.ImageView = android.widget.ImageView(ctx).apply {
        setImageResource(res); setColorFilter(color)
        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        layoutParams = LinearLayout.LayoutParams(dp(ctx, sizeDp), dp(ctx, sizeDp))
    }

    fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    fun round(color: Int, radiusDp: Int, ctx: Context, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(ctx, radiusDp).toFloat(); if (stroke != null) setStroke(dp(ctx, 1), stroke)
    }

    fun text(ctx: Context, s: CharSequence, sizeSp: Float = 14f, color: Int = INK, bold: Boolean = false): TextView = TextView(ctx).apply {
        text = s; textSize = sizeSp; setTextColor(color); if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    fun card(ctx: Context): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = round(Color.WHITE, 14, ctx, LINE)
        val p = dp(ctx, 14); setPadding(p, p, p, p)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 10) }
    }

    fun pill(ctx: Context, s: String, bg: Int, fg: Int): TextView = text(ctx, s, 12f, fg, true).apply {
        background = round(bg, 999, ctx); setPadding(dp(ctx, 9), dp(ctx, 3), dp(ctx, 9), dp(ctx, 3))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(ctx, 6) }
    }

    fun chip(ctx: Context, s: String, on: Boolean, onClick: () -> Unit): TextView = text(ctx, s, 13f, if (on) BLUE else 0xFF33445E.toInt(), true).apply {
        background = round(if (on) 0xFFE3ECFB.toInt() else Color.WHITE, 999, ctx, if (on) 0xFFA9C2EC.toInt() else 0xFFC9D3E1.toInt())
        setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8)); gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(ctx, 8) }
        setOnClickListener { onClick() }
    }

    fun button(ctx: Context, s: String, filled: Boolean, onClick: () -> Unit): TextView = text(ctx, s, 14f, if (filled) Color.WHITE else BLUE, true).apply {
        background = round(if (filled) BLUE else Color.WHITE, 12, ctx, if (filled) null else BLUE)
        setPadding(dp(ctx, 14), dp(ctx, 10), dp(ctx, 14), dp(ctx, 10)); gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 8); marginEnd = dp(ctx, 8) }
        setOnClickListener { onClick() }
    }

    /** Colores por estado, criticidad y plazo (los mismos de la web). */
    fun estadoColors(s: String?): Pair<Int, Int> = when (s) {
        "Validada" -> 0xFFDDF3E6.toInt() to 0xFF16663A.toInt()
        "Completa" -> 0xFFE3ECFB.toInt() to 0xFF1A4FA8.toInt()
        "En revisión" -> 0xFFF0E6FA.toInt() to 0xFF6A3D9A.toInt()
        "Pendiente" -> 0xFFDDF1F2.toInt() to 0xFF0E5E63.toInt()
        else -> 0xFFFDF0D8.toInt() to 0xFF7A4A00.toInt()
    }

    fun critColors(s: String?): Pair<Int, Int> = when (s) {
        "Riesgo" -> 0xFFFBE4E2.toInt() to 0xFF9E2B23.toInt()
        "Observado" -> 0xFFFDF0D8.toInt() to 0xFF7A4A00.toInt()
        "Viable" -> 0xFFDDF3E6.toInt() to 0xFF16663A.toInt()
        else -> 0xFFEEF1F6.toInt() to 0xFF4A5A72.toInt()
    }

    fun row(ctx: Context): LinearLayout = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

    fun fmtMin(m: Long?): String = when {
        m == null -> "—"
        m >= 60 -> "${m / 60} h" + if (m % 60 > 0) " ${m % 60} min" else ""
        else -> "$m min"
    }

    fun gone(v: View, hide: Boolean) { v.visibility = if (hide) View.GONE else View.VISIBLE }
}
