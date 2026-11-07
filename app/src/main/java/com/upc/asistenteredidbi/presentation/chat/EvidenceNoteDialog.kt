package com.upc.asistenteredidbi.presentation.chat

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 0.0.15 · «¿Qué muestra esta foto?»: descripción opcional de una evidencia.
 * Sale debajo de la foto en la minuta y la IA la usa como dato del técnico
 * (por ejemplo, «el speedtest lo hice desde mi laptop» o «la PC ya está en caja y es del local»).
 * Se usa desde la lista de evidencias, desde la foto en el chat y antes de subir la foto.
 */
object EvidenceNoteDialog {

    fun show(context: Context, title: String, current: String, onSave: (String) -> Unit) {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        box.addView(TextView(context).apply {
            text = "Escribe qué muestra la foto. Sale debajo de la foto en la minuta y la IA la usa para sus observaciones y recomendaciones."
            textSize = 13f
            setPadding(0, 0, 0, dp(8))
        })
        val input = EditText(context).apply {
            setText(current)
            setSelection(text.length)
            hint = "Ej.: La PC ya está en caja y es del local"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 2
            maxLines = 6
        }
        box.addView(input)
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setView(box)
            .setPositiveButton("Guardar") { _, _ -> onSave(input.text.toString().trim()) }
            .setNegativeButton("Cancelar", null)
        if (current.isNotBlank()) builder.setNeutralButton("Eliminar") { _, _ -> onSave("") }
        builder.show()
    }
}
