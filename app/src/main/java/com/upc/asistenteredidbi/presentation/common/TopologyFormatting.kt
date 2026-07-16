package com.upc.asistenteredidbi.presentation.common

import com.upc.asistenteredidbi.domain.model.ChatTopology

/**
 * Formatea la topología estructurada (construida por el motor a partir de las
 * respuestas del chat) como texto legible: un enlace por línea, con el tipo de
 * conexión y, si corresponde, el estado del enlace. Se usa tanto en el chat
 * técnico (al completarse la evaluación) como en la propuesta técnica.
 */
fun ChatTopology.toHierarchicalText(): String {
    if (nodes.isEmpty() || links.isEmpty()) {
        return "Topología no disponible."
    }

    val labelById = nodes.associateBy({ it.id }, { it.label })

    return links.joinToString(separator = "\n") { link ->
        val source = labelById[link.source] ?: link.source
        val target = labelById[link.target] ?: link.target
        val connectionLabel = connectionTypeLabel(link.connectionType)
        val statusSuffix = if (link.status.equals("CON_FALLA", ignoreCase = true)) {
            " ⚠ con falla"
        } else {
            ""
        }
        "$source → $target  [$connectionLabel]$statusSuffix"
    }
}

private fun connectionTypeLabel(connectionType: String): String = when (connectionType.uppercase()) {
    "CABLE_RED" -> "Cable de red"
    "WIFI" -> "WiFi"
    "USB_BLUETOOTH" -> "USB / Bluetooth"
    else -> connectionType
}
