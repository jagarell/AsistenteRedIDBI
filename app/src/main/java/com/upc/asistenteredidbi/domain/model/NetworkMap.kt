package com.upc.asistenteredidbi.domain.model

/**
 * Mapa de red editable (nodos, enlaces, cajas de texto y fotos de evidencia) con
 * posiciones normalizadas 0..1. Es el mismo documento que define FastAPI
 * (`app/chat/map_model.py`): el gateway lo guarda tal cual y la minuta lo dibuja.
 */
data class MapNodeModel(
    val id: String,
    val label: String,
    /** internet | router | switch | access_point | repeater | computer | printer | camera | pos | other */
    val type: String = "other",
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    /** Equipo por adquirir/instalar (borde punteado). */
    val pending: Boolean = false,
    val detail: String? = null,
    /** Color propio (#RRGGBB); null = el del tipo. */
    val color: String? = null
)

data class MapLinkModel(
    val id: String,
    val source: String,
    val target: String,
    /** solid (cable) | dashed (WiFi) | dotted (USB) */
    val style: String = "solid",
    /** Se dibuja en rojo ("debería ir por cable"). */
    val observed: Boolean = false
)

data class MapTextModel(
    val id: String,
    val text: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    /** S | M | L */
    val size: String = "M",
    val color: String = "#FFF4CC"
)

data class MapImageModel(
    val id: String,
    val evidenceCode: String,
    val scope: String = "",
    val label: String = "",
    val x: Float = 0.8f,
    val y: Float = 0.2f,
    /** S | M | L */
    val size: String = "M"
)

data class NetworkMap(
    val nodes: List<MapNodeModel> = emptyList(),
    val links: List<MapLinkModel> = emptyList(),
    val texts: List<MapTextModel> = emptyList(),
    val images: List<MapImageModel> = emptyList()
) {
    /** "7 equipos · 6 enlaces · 1 observado" */
    fun summary(): String {
        val observed = links.count { it.observed }
        return "${nodes.size} equipos · ${links.size} enlaces" +
            if (observed > 0) " · $observed observado" + if (observed > 1) "s" else "" else ""
    }
}

object MapNodeTypes {
    /** (tipo, nombre para mostrar) en el orden del menú "Elemento de red". */
    val all = listOf(
        "router" to "Router", "switch" to "Switch", "access_point" to "Access point",
        "repeater" to "Repetidor", "computer" to "PC / laptop", "printer" to "Impresora",
        "camera" to "Cámara", "pos" to "POS / caja", "other" to "Otro"
    )

    fun label(type: String): String = when (type) {
        "internet" -> "Internet"
        "computer" -> "Computadora"
        else -> all.firstOrNull { it.first == type }?.second ?: "Equipo"
    }

    fun colorHex(type: String): String = when (type) {
        "internet" -> "#37474F"
        "router" -> "#1565C0"
        "switch" -> "#2E7D32"
        "access_point", "repeater" -> "#6A1B9A"
        "computer" -> "#00838F"
        "printer" -> "#555555"
        "camera" -> "#C62828"
        "pos" -> "#EF6C00"
        else -> "#607D8B"
    }
}
