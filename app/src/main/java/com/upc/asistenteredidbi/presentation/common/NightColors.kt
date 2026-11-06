package com.upc.asistenteredidbi.presentation.common

import android.content.res.Configuration
import android.graphics.Color

/**
 * Colores de la UI armada por código. En modo noche cambia los neutros y los
 * fondos claros a su equivalente oscuro; el resto (azules, rojos, verdes) queda igual.
 */
object NightColors {
    @Volatile
    var night: Boolean = false

    fun sync(conf: Configuration) {
        night = (conf.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private val dark = mapOf(
        "#1F2937" to "#E8EAED", "#111827" to "#E8EAED", "#263238" to "#E8EAED",
        "#374151" to "#CBD2DC", "#4B5563" to "#CBD2DC", "#455A64" to "#B0BEC5",
        "#6B7280" to "#A0A8B5", "#546E7A" to "#90A4AE",
        "#F5F7FB" to "#0F141A", "#F7F8FB" to "#0F141A", "#FFFFFF" to "#1A2029",
        "#E8F1FD" to "#1E3350", "#E3EEFC" to "#1E3350", "#E3F2FD" to "#1E3350",
        "#FFF7EC" to "#3A2D1A", "#FFF7E6" to "#3A2D1A", "#FFF4E5" to "#3A2D1A",
        "#FFF3E0" to "#3A2D1A", "#FFF4CC" to "#3A2D1A", "#FDECEA" to "#3D2124",
        "#E8F5E9" to "#1D3325", "#D0D7E2" to "#33506F", "#C9CFDA" to "#33506F",
        "#1565C0" to "#4C8DDB", "#1F5FBF" to "#4C8DDB", "#1F4E9E" to "#4C8DDB",
        "#B45309" to "#E0A15A", "#C62828" to "#EF7B7B", "#D32F2F" to "#EF7B7B",
        "#2E7D32" to "#6FBF73"
    )

    fun parse(hex: String): Int {
        val key = hex.uppercase()
        return Color.parseColor(if (night) dark[key] ?: hex else hex)
    }
}
