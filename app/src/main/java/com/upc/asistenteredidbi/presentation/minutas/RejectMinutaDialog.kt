package com.upc.asistenteredidbi.presentation.minutas

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog

/** El supervisor rechaza una minuta completa: debe escribir qué corregir, y el técnico lo ve al abrirla. */
object RejectMinutaDialog {
    fun show(context: Context, clientName: String, onConfirm: (String) -> Unit) {
        val d = context.resources.displayMetrics.density
        val input = EditText(context).apply {
            hint = "Qué debe corregir el técnico"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 3
        }
        val box = FrameLayout(context).apply {
            setPadding((24 * d).toInt(), (8 * d).toInt(), (24 * d).toInt(), 0)
            addView(input)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle("Rechazar minuta")
            .setMessage("«$clientName» vuelve a Borrador. El técnico la corrige y la completa otra vez.")
            .setView(box)
            .setPositiveButton("Rechazar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val reason = input.text.toString().trim()
                if (reason.isEmpty()) {
                    input.error = "Escribe el motivo"
                } else {
                    dialog.dismiss()
                    onConfirm(reason)
                }
            }
        }
        dialog.show()
    }
}
