package com.upc.asistenteredidbi.presentation.historial

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Hoja "Filtrar por fecha" del prototipo: Todas las fechas · Hoy · Últimos 7 días · Este mes, y un rango Desde/Hasta
 * con el botón "Aplicar rango".
 */
object DateFilterSheet {

    fun show(
        context: Context,
        current: HistorialUiState,
        onQuick: (DateQuick) -> Unit,
        onRange: (String?, String?) -> Unit
    ) {
        val d = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val shown = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        var from = current.dateFrom
        var to = current.dateTo
        val dialog = BottomSheetDialog(context)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(24))
        }
        root.addView(TextView(context).apply {
            text = "Filtrar por fecha"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#1F2937"))
        })
        root.addView(TextView(context).apply {
            text = "Fecha de la evaluación"; textSize = 14f; setTextColor(Color.parseColor("#6B7280"))
            setPadding(0, dp(4), 0, dp(12))
        })

        // Chips rápidos, de dos en dos para que quepan en pantallas angostas.
        val quicks = listOf(DateQuick.TODAS, DateQuick.HOY, DateQuick.SIETE, DateQuick.MES)
        quicks.chunked(2).forEach { pair ->
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { q ->
                val on = current.dateQuick == q
                row.addView(Button(context).apply {
                    text = q.label; isAllCaps = false; textSize = 14f
                    setTextColor(if (on) Color.WHITE else Color.parseColor("#374151"))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(21).toFloat()
                        setColor(if (on) Color.parseColor("#1565C0") else Color.WHITE)
                        setStroke(dp(1), if (on) Color.parseColor("#1565C0") else Color.parseColor("#D0D7E2"))
                    }
                    setOnClickListener { onQuick(q); dialog.dismiss() }
                }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) })
            }
            root.addView(row)
        }

        fun dateField(label: String, value: () -> String?, set: (String) -> Unit): LinearLayout {
            val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            box.addView(TextView(context).apply { text = label; textSize = 13f; setTextColor(Color.parseColor("#6B7280")) })
            val field = TextView(context).apply {
                textSize = 15f; setTextColor(Color.parseColor("#1F2937")); gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), 0, dp(12), 0)
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat(); setColor(Color.WHITE); setStroke(dp(1), Color.parseColor("#D0D7E2"))
                }
            }
            fun refresh() { field.text = value()?.let { shown.format(iso.parse(it)!!) } ?: "dd/mm/aaaa" }
            refresh()
            field.setOnClickListener {
                val cal = Calendar.getInstance().apply { value()?.let { time = iso.parse(it)!! } }
                DatePickerDialog(context, { _, y, m, day ->
                    val c = Calendar.getInstance().apply { set(y, m, day) }
                    set(iso.format(c.time)); refresh()
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            box.addView(field, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(4) })
            return box
        }

        val rangeRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(12), 0, dp(12)) }
        rangeRow.addView(dateField("Desde", { from }, { from = it }),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) })
        rangeRow.addView(dateField("Hasta", { to }, { to = it }),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6) })
        root.addView(rangeRow)

        root.addView(MaterialButton(context).apply {
            text = "Aplicar rango"; isAllCaps = false; textSize = 16f
            setBackgroundColor(Color.parseColor("#1565C0")); setTextColor(Color.WHITE)
            cornerRadius = dp(14)
            setOnClickListener {
                if (from != null && to != null && from!! > to!!) { val t = from; from = to; to = t }
                onRange(from, to); dialog.dismiss()
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)))

        dialog.setContentView(root as View)
        dialog.show()
    }
}
