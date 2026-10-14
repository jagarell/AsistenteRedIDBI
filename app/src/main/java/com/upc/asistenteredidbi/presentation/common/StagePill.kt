package com.upc.asistenteredidbi.presentation.common

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import com.upc.asistenteredidbi.domain.model.EvaluationStage

/** Etiqueta de estado con los colores del prototipo (En análisis / Borrador / Completa / Validada). */
object StagePill {
    private fun colors(stage: EvaluationStage): Pair<String, String> = when (stage) {
        EvaluationStage.EN_CURSO -> "#F3E5F5" to "#7B1FA2"
        EvaluationStage.BORRADOR -> "#FFF3E0" to "#EF6C00"
        EvaluationStage.COMPLETA -> "#E8F5E9" to "#2E7D32"
        EvaluationStage.VALIDADA -> "#E3F2FD" to "#1565C0"
    }

    /** `rejected`: borrador que el supervisor devolvió al técnico; se ve en rojo como "Rechazada". */
    fun apply(view: TextView, stage: EvaluationStage, rejected: Boolean = false) {
        val (bg, fg) = if (rejected) "#FDECEA" to "#C62828" else colors(stage)
        view.text = if (rejected) "Rechazada" else stage.label
        view.setTextColor(Color.parseColor(fg))
        view.background = GradientDrawable().apply {
            setColor(Color.parseColor(bg))
            cornerRadius = 16f * view.resources.displayMetrics.density
        }
    }
}
