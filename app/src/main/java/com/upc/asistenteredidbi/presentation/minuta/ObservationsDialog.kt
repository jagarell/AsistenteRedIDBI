package com.upc.asistenteredidbi.presentation.minuta

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Observaciones de la minuta: texto del técnico, serie de facturación, si el cliente acepta, método de pago y otro. */
object ObservationsDialog {

    fun show(ctx: Context, current: Map<String, Any?>, onSave: (Map<String, Any?>) -> Unit) {
        val dp = ctx.resources.displayMetrics.density
        val form = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding((24 * dp).toInt(), (8 * dp).toInt(), (24 * dp).toInt(), 0) }
        fun label(t: String) = TextView(ctx).apply { text = t; textSize = 12f; setTextColor(Color.parseColor("#6B7280")); setPadding(0, (12 * dp).toInt(), 0, 0) }
        fun input(hint: String, value: Any?, multi: Boolean = false) = EditText(ctx).apply {
            this.hint = hint; setText(value?.toString().orEmpty())
            inputType = InputType.TYPE_CLASS_TEXT or (if (multi) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0) or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        form.addView(label("Observaciones del técnico")); val tec = input("Escribe tus observaciones", current["tec"], true); form.addView(tec)
        form.addView(label("Serie de facturación")); val serie = input("Ej. F001", current["serie"]); form.addView(serie)
        form.addView(label("¿El cliente acepta la recomendación?"))
        val viabOptions = listOf("", "Sí", "No", "Otro")
        val viab = android.widget.Spinner(ctx).apply {
            adapter = android.widget.ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, listOf("Sin responder", "Sí", "No", "Otro (escribir)"))
            setSelection(viabOptions.indexOf((current["viab"] as? String).orEmpty()).coerceAtLeast(0))
        }
        form.addView(viab)
        val viabOtro = input("Escribe cuál", current["viabOtro"]); form.addView(viabOtro)
        form.addView(label("Método de pago"))
        val payOptions = listOf("Niubiz", "Izipay", "Culqi", "Otro")
        @Suppress("UNCHECKED_CAST")
        val currentPay = (current["pago"] as? List<String>).orEmpty()
        val pay = payOptions.map { opt -> android.widget.CheckBox(ctx).apply { text = opt; isChecked = opt in currentPay } }
        pay.forEach { form.addView(it) }
        val payOtro = input("Escribe cuál", current["pagoOtro"]); form.addView(payOtro)
        form.addView(label("Otro: nombre")); val otroT = input("Ej. Horario de atención", current["otroT"]); form.addView(otroT)
        form.addView(label("Otro: contenido")); val otroC = input("Escribe el contenido", current["otroC"], true); form.addView(otroC)
        AlertDialog.Builder(ctx)
            .setTitle("Observaciones")
            .setView(android.widget.ScrollView(ctx).apply { addView(form) })
            .setPositiveButton("Guardar") { _, _ ->
                onSave(mapOf(
                    "tec" to tec.text.toString().trim(), "serie" to serie.text.toString().trim(),
                    "viab" to viabOptions[viab.selectedItemPosition], "viabOtro" to viabOtro.text.toString().trim(),
                    "pago" to pay.filter { it.isChecked }.map { it.text.toString() }, "pagoOtro" to payOtro.text.toString().trim(),
                    "otroT" to otroT.text.toString().trim(), "otroC" to otroC.text.toString().trim()
                ))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
