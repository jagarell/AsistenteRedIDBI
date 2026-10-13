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
    val color: String? = null,
    /** "Ubic.: …": dónde está el equipo (lo dijo el técnico en el chat). */
    val location: String? = null,
    /** "IP: …"; null = "IP: sin dato" (solo en los tipos que llevan IP). */
    val ip: String? = null,
    /** Switch: cantidad de puertos ("Switch x8"). */
    val ports: Int? = null,
    /** ok | pending (por implementar: círculo blanco con borde punteado) | broken (con falla: en rojo). */
    val status: String? = null,
    /** ok (hay toma de energía: rayo azul) | missing (falta toma: rayo rojo) | null. */
    val power: String? = null,
    /** Impresora USB: "IP: no aplica (USB)". */
    val usb: Boolean = false,
    /** IP fuera del segmento del router: la línea "IP:" va en rojo. */
    val ipBad: Boolean = false,
    val tier: Int? = null
) {
    /** Por implementar (compatible con mapas guardados antes de `status`). */
    val isPending: Boolean get() = status == "pending" || (status == null && pending)
    val isBroken: Boolean get() = status == "broken"
}

data class MapLinkModel(
    val id: String,
    val source: String,
    val target: String,
    /** main (gruesa) | cable (fina) | camera (trazos cortos) | wifi (trazos medianos) | usb (flecha) — o solid / dashed / dotted de mapas anteriores. */
    val style: String = "cable",
    /** Se dibuja en rojo ("con falla / observado"). */
    val observed: Boolean = false,
    /** Color propio (#RRGGBB); null = azul existente, plomo por implementar o rojo observado. */
    val color: String? = null
) {
    /** Tipo de línea normalizado (los mapas viejos usaban solid / dashed / dotted). */
    val kind: String get() = when (style) { "solid" -> "cable"; "dashed" -> "wifi"; "dotted" -> "usb"; else -> style }
}

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
        "router" to "Router", "tplink" to "TP-Link con chip", "switch" to "Switch", "switch_managed" to "Switch administrable",
        "access_point" to "Access point", "repeater" to "Repetidor", "mesh" to "Mesh", "server" to "Servidor",
        "pc" to "PC", "laptop" to "Laptop", "raspberry" to "Raspberry", "printer" to "Impresora",
        "camera" to "Cámara de vigilancia", "tablet" to "Sunmi o tablet", "other" to "Otro"
    )

    fun label(type: String): String = when (type) {
        "internet" -> "Internet"
        "computer" -> "Computadora"
        "pos" -> "POS / caja"
        "ap" -> "Access point"
        else -> all.firstOrNull { it.first == type }?.second ?: "Equipo"
    }

    /** Tipos que llevan "IP: …" debajo del nombre. */
    fun showsIp(type: String) = type !in setOf("internet", "camera", "tablet", "pos")

    fun colorHex(type: String): String = when (type) {
        "internet", "server" -> "#37474F"
        "router", "tplink" -> "#1565C0"
        "switch", "switch_managed", "camera" -> "#00838F"
        "access_point", "ap", "repeater", "mesh" -> "#6A1B9A"
        "pc", "laptop", "computer", "tablet", "pos" -> "#2E7D32"
        "raspberry" -> "#AD1457"
        "printer" -> "#E65100"
        else -> "#546E7A"
    }
}
