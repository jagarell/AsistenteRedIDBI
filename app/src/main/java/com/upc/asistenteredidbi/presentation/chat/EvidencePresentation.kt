package com.upc.asistenteredidbi.presentation.chat

import com.upc.asistenteredidbi.domain.model.ChatEvidenceResult
import com.upc.asistenteredidbi.domain.model.ChatNodePrompt

/** Una evidencia ya subida (o pendiente) — alimenta el resumen y la galería de evidencias. */
data class ChatEvidenceItem(
    val code: String,
    val scope: String,
    val title: String,
    val section: String,
    val summary: String,
    val paths: List<String>,
    /** "ok", "warn" (leída con observaciones) o "missing" (omitida por ahora). */
    val status: String,
    /** Id del mensaje del chat que originó la evidencia (para descartarla al volver atrás). */
    val messageId: Long = 0L
)

/** Un dato leído por la IA que el técnico puede corregir ("Corregir"). */
data class EditableField(val key: String, val label: String, val value: String, val numeric: Boolean)

/**
 * Cómo se presenta cada evidencia en el chat: la tarjeta que pide la foto, la
 * tarjeta "Esto leí…" con lo que extrajo la IA (diseño del prototipo) y el
 * resumen corto de la galería. Los códigos son los del flujo (E1..E9, EU-*).
 */
object EvidencePresentation {

    fun title(code: String): String = when (code) {
        "E1" -> "Speedtest"
        "E2" -> "Foto del router"
        "E3" -> "Etiqueta del router"
        "E4" -> "ipconfig de la caja"
        "E5" -> "Ticket de la impresora"
        "E6" -> "Puntos de red y tomas"
        "E7" -> "Extensión de energía"
        "E8" -> "Escáner de IP"
        "E9" -> "Fotos generales"
        "EU-R" -> "Punto de red cercano"
        "EU-RN" -> "Lugar del punto de red"
        "EU-E" -> "Toma cercana"
        "EU-EN" -> "Lugar de la toma"
        else -> code
    }

    fun section(code: String): String = when (code) {
        "E1" -> "Conectividad"
        "E2", "E3" -> "Equipamiento y POS"
        "E4" -> "Equipos de caja"
        "E5" -> "Impresoras"
        "E6", "E7" -> "Cableado y energía"
        "E8" -> "Red"
        "E9" -> "General"
        else -> "Instalaciones por hacer"
    }

    private fun hint(code: String): String? = when (code) {
        "E1" -> "Leeré la velocidad, el ping y el proveedor por ti."
        "E3" -> "Ocultaré usuario, contraseña y clave WiFi de la etiqueta."
        "E4" -> "Leeré la IP, la puerta de enlace y el tipo de adaptador."
        "E5" -> "Leeré el modelo, la IP, la MAC y el puerto."
        "E8" -> "Leeré cada dispositivo con su IP, MAC y fabricante."
        else -> null
    }

    /** Burbuja del bot que pide la evidencia: "Evidencia · Speedtest" + texto + pista. */
    fun promptCard(prompt: ChatNodePrompt): ChatCard {
        val code = prompt.evidenceCode.orEmpty()
        return ChatCard(
            kind = "EVIDENCE_PROMPT",
            title = "Evidencia · ${title(code)}",
            text = prompt.text,
            note = hint(code)
        )
    }

    /** Tarjeta con lo que leyó la IA. */
    fun resultCard(result: ChatEvidenceResult, crossChecks: List<String>): ChatCard {
        val x = result.extracted
        fun str(key: String): String? = (x[key] as? String)?.takeIf { it.isNotBlank() }
        fun num(key: String): String? = (x[key] as? Number)?.let { fmt(it.toDouble()) }
        val warnings = crossChecks.toMutableList()
        val description = str("descripcion")

        return when (result.code) {
            "E1" -> {
                val tiles = listOfNotNull(
                    num("bajadaMbps")?.let { ChatResultField("Bajada Mbps", it) },
                    num("subidaMbps")?.let { ChatResultField("Subida Mbps", it) },
                    num("pingMs")?.let { ChatResultField("Ping ms", it) }
                )
                val down = num("latenciaBajadaMs")
                val up = num("latenciaSubidaMs")
                if (down != null && up != null) {
                    warnings.add(0, "Con la red en uso el ping sube a $down ms (bajada) y $up ms (subida). Lo anoto como observación.")
                }
                ChatCard(
                    kind = "RESULT", title = "Esto leí en la captura", badge = "+ IA", tiles = tiles,
                    rows = listOfNotNull(
                        str("proveedor")?.let { ChatResultField("Proveedor", it) },
                        str("servidor")?.let { ChatResultField("Servidor", it) }
                    ),
                    text = description.takeIf { tiles.isEmpty() },
                    warnings = warnings
                )
            }

            "E3" -> ChatCard(
                kind = "RESULT", title = "Leí la etiqueta", badge = "+ IA",
                rows = listOfNotNull(
                    joined(str("marca"), str("modelo"))?.let { ChatResultField("Equipo", it) },
                    str("ipGestion")?.let { ChatResultField("IP de gestión", it) },
                    str("mac")?.let { ChatResultField("MAC", it) },
                    str("numeroSerie")?.let { ChatResultField("Serie", it) }
                ),
                text = description.takeIf { str("marca") == null && str("modelo") == null },
                note = "🔒 Oculté el usuario, la contraseña y la clave WiFi de la etiqueta. No se guardan en la minuta.",
                warnings = warnings
            )

            "E4" -> ChatCard(
                kind = "RESULT", title = "Leí el ipconfig", badge = "+ IA",
                rows = listOfNotNull(
                    str("adaptador")?.let { ChatResultField("Adaptador", it) },
                    str("ipv4")?.let { ChatResultField("IPv4", it) },
                    str("mascara")?.let { ChatResultField("Máscara", it) },
                    str("puertaEnlace")?.let { ChatResultField("Gateway", it) },
                    str("mac")?.let { ChatResultField("MAC", it) }
                ),
                text = description.takeIf { str("ipv4") == null },
                warnings = warnings
            )

            "E5" -> ChatCard(
                kind = "RESULT", title = "Leí el ticket de autoprueba", badge = "+ IA",
                rows = listOfNotNull(
                    joined(str("marca"), str("modelo"))?.let { ChatResultField("Modelo", it) },
                    str("ip")?.let {
                        ChatResultField("IP", it + if (x["dhcp"] == false) " · fija" else if (x["dhcp"] == true) " · DHCP" else "")
                    },
                    str("puertaEnlace")?.let { ChatResultField("Gateway", it) },
                    str("mac")?.let { ChatResultField("MAC", it) },
                    joined(str("puerto"), str("papel")?.let { "papel $it" }, " · ")?.let { ChatResultField("Puerto", it) },
                    str("numeroSerie")?.let { ChatResultField("Serie", it) }
                ),
                text = description.takeIf { str("modelo") == null && str("ip") == null },
                warnings = warnings
            )

            "E8" -> {
                val devices = (x["dispositivos"] as? List<*>).orEmpty().filterIsInstance<Map<*, *>>()
                val subnet = devices.mapNotNull { (it["ip"] as? String)?.split(".")?.take(3)?.joinToString(".") }
                    .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
                val shown = devices.take(7).map { d ->
                    val ip = d["ip"] as? String
                    val name = (d["nombre"] as? String)?.takeIf { it.isNotBlank() }
                        ?: (d["fabricante"] as? String)?.takeIf { it.isNotBlank() } ?: "Sin nombre"
                    ChatListRow(
                        tag = ip?.substringAfterLast('.')?.let { ".$it" }.orEmpty(),
                        label = name,
                        status = if (d["documentado"] == false) "warn" else "ok"
                    )
                }
                ChatCard(
                    kind = "RESULT",
                    title = if (devices.isEmpty()) "Escáner de IP" else "${devices.size} equipos" + (subnet?.let { " en $it.0/24" } ?: ""),
                    badge = "+ IA", list = shown,
                    text = description.takeIf { devices.isEmpty() },
                    footer = if (devices.size > shown.size) "+ ${devices.size - shown.size} más en la captura" else null,
                    warnings = warnings
                )
            }

            "E2" -> ChatCard(
                kind = "RESULT", title = "Leí la foto", badge = "+ IA",
                rows = listOfNotNull(
                    joined(str("marca"), str("modelo"))?.let { ChatResultField("Equipo", it) },
                    str("ubicacionVisual")?.let { ChatResultField("Ubicación", it) }
                ),
                text = description, warnings = warnings
            )

            "E6" -> ChatCard(
                kind = "RESULT", title = "Leí las fotos", badge = "+ IA",
                rows = listOfNotNull(
                    str("estadoCableado")?.let { ChatResultField("Cableado", it) },
                    (x["rotulado"] as? Boolean)?.let { ChatResultField("Rotulado", if (it) "Sí" else "No") },
                    str("observaciones")?.let { ChatResultField("Notas", it) }
                ),
                text = description, warnings = warnings
            )

            "E7" -> ChatCard(
                kind = "RESULT", title = "Leí la extensión", badge = "+ IA",
                rows = listOfNotNull(
                    (x["equiposConectados"] as? List<*>)?.joinToString(", ")?.takeIf { it.isNotBlank() }
                        ?.let { ChatResultField("Equipos", it) },
                    str("estado")?.let { ChatResultField("Estado", it) },
                    str("riesgo")?.let { ChatResultField("Riesgo", it) }
                ),
                text = description, warnings = warnings
            )

            else -> ChatCard(
                kind = "RESULT", title = "Esto veo en la foto", badge = "+ IA",
                rows = listOfNotNull(
                    str("estado")?.let { ChatResultField("Estado", it) },
                    str("tipoToma")?.let { ChatResultField("Tipo de toma", it) },
                    (x["rotulado"] as? Boolean)?.let { ChatResultField("Rotulado", if (it) "Sí" else "No") }
                ),
                text = description, warnings = warnings
            )
        }
    }

    /** Datos corregibles de una evidencia (los de lista, como el escáner de IP, no se editan). */
    fun editableFields(result: ChatEvidenceResult): List<EditableField> {
        val spec: List<Triple<String, String, Boolean>> = when (result.code) {
            "E1" -> listOf(
                Triple("bajadaMbps", "Bajada (Mbps)", true), Triple("subidaMbps", "Subida (Mbps)", true),
                Triple("pingMs", "Ping (ms)", true), Triple("proveedor", "Proveedor", false),
                Triple("servidor", "Servidor", false)
            )
            "E2" -> listOf(
                Triple("marca", "Marca", false), Triple("modelo", "Modelo", false),
                Triple("ubicacionVisual", "Ubicación", false)
            )
            "E3" -> listOf(
                Triple("marca", "Marca", false), Triple("modelo", "Modelo", false),
                Triple("ipGestion", "IP de gestión", false), Triple("mac", "MAC", false),
                Triple("numeroSerie", "Serie", false)
            )
            "E4" -> listOf(
                Triple("adaptador", "Adaptador (WiFi/Ethernet)", false), Triple("ipv4", "IPv4", false),
                Triple("mascara", "Máscara", false), Triple("puertaEnlace", "Gateway", false),
                Triple("mac", "MAC", false)
            )
            "E5" -> listOf(
                Triple("marca", "Marca", false), Triple("modelo", "Modelo", false), Triple("ip", "IP", false),
                Triple("puertaEnlace", "Gateway", false), Triple("mac", "MAC", false),
                Triple("puerto", "Puerto", false), Triple("papel", "Papel", false),
                Triple("numeroSerie", "Serie", false)
            )
            "E8" -> emptyList()
            else -> listOf(Triple("descripcion", "Descripción", false))
        }
        return spec.map { (key, label, numeric) ->
            val raw = result.extracted[key]
            val text = when (raw) {
                null -> ""
                is Number -> fmt(raw.toDouble())
                else -> raw.toString()
            }
            EditableField(key, label, text, numeric)
        }
    }

    /** Frase corta para la galería, ej. "✦ 91.5 ↓ · 92.5 ↑ Mbps · 8 ms". */
    fun summary(result: ChatEvidenceResult): String {
        val x = result.extracted
        fun str(key: String): String? = (x[key] as? String)?.takeIf { it.isNotBlank() }
        fun num(key: String): String? = (x[key] as? Number)?.let { fmt(it.toDouble()) }
        val text = when (result.code) {
            "E1" -> listOfNotNull(
                num("bajadaMbps")?.let { "$it ↓" }, num("subidaMbps")?.let { "$it ↑ Mbps" }, num("pingMs")?.let { "$it ms" }
            ).joinToString(" · ")
            "E3" -> listOfNotNull(joined(str("marca"), str("modelo")), str("ipGestion"), "credenciales ocultas")
                .joinToString(" · ")
            "E4" -> listOfNotNull(str("ipv4"), str("adaptador")).joinToString(" · ")
            "E5" -> listOfNotNull(joined(str("marca"), str("modelo")), str("ip")).joinToString(" · ")
            "E8" -> "${(x["dispositivos"] as? List<*>)?.size ?: 0} dispositivos"
            else -> ""
        }
        return text.ifBlank { str("descripcion").orEmpty() }
    }

    private fun joined(a: String?, b: String?, sep: String = " "): String? =
        listOfNotNull(a, b).joinToString(sep).takeIf { it.isNotBlank() }

    private fun fmt(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)
}
