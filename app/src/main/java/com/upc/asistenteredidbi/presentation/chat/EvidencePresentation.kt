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
        "E1b" -> "Speedtest con chip"
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

    /** Nombre corto para la miniatura del resumen ("Ticket cocina", "ipconfig caja"). */
    fun shortLabel(item: ChatEvidenceItem): String {
        val area = item.title.substringAfter(" · ", "").lowercase()
        fun withArea(base: String) = if (area.isNotBlank()) "$base $area" else base
        return when (item.code) {
            "E2" -> "Router"
            "E3" -> "Etiqueta router"
            "E4" -> withArea("ipconfig")
            "E5" -> withArea("Impresora")
            "E6" -> "Puntos de red"
            "E7" -> "Extensión"
            "E8" -> "Escáner IP"
            else -> item.title.substringBefore(" · ")
        }
    }

    fun section(code: String): String = when (code) {
        "E1", "E1b" -> "Conectividad"
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
        "E1b" -> "Leeré la velocidad que da el chip en el local."
        "E2" -> "Contaré los puertos LAN del router: cuántos hay y cuántos están libres."
        "E3" -> "Ocultaré usuario, contraseña y clave WiFi de la etiqueta."
        "E4" -> "Leeré la IP, la puerta de enlace y el tipo de adaptador."
        "E5" -> "Leeré el modelo, la IP, la MAC y el puerto."
        "E8" -> "Leeré cada dispositivo con su IP, MAC y fabricante."
        "E9" -> "Es obligatoria: sin la foto del local no se genera la minuta."
        else -> null
    }

    /** Burbuja del bot que pide la evidencia: "Evidencia · Speedtest" + texto + pista. */
    fun promptCard(prompt: ChatNodePrompt): ChatCard {
        val code = prompt.evidenceCode.orEmpty()
        return ChatCard(
            kind = "EVIDENCE_PROMPT",
            title = if (prompt.evidenceTotal > 0) "Evidencia ${prompt.evidenceNumber} de ${prompt.evidenceTotal} · ${title(code)}"
            else "Evidencia · ${title(code)}",
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
            "E1", "E1b" -> {
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
                fun nameOf(d: Map<*, *>): String? = (d["etiqueta"] as? String)?.takeIf { it.isNotBlank() }
                    ?: (d["nombre"] as? String)?.takeIf { it.isNotBlank() }
                    ?: (d["fabricante"] as? String)?.takeIf { it.isNotBlank() }
                // Primero lo documentado, luego lo que hay que aclarar; los que no
                // traen ni nombre ni fabricante se resumen en una línea.
                val named = devices.filter { nameOf(it) != null }
                    .sortedBy { if (it["documentado"] == false) 1 else 0 }
                val shown = named.take(7).map { d ->
                    ChatListRow(
                        tag = (d["ip"] as? String)?.substringAfterLast('.')?.let { ".$it" }.orEmpty(),
                        label = nameOf(d).orEmpty(),
                        status = if (d["documentado"] == false) "warn" else "ok"
                    )
                }
                val unnamed = devices.size - named.size
                ChatCard(
                    kind = "RESULT",
                    title = if (devices.isEmpty()) "Escáner de IP" else "${devices.size} equipos" + (subnet?.let { " en $it.0/24" } ?: ""),
                    badge = "+ IA", list = shown,
                    text = description.takeIf { devices.isEmpty() },
                    footer = when {
                        unnamed > 0 -> "+ $unnamed sin nombre (probablemente celulares)"
                        named.size > shown.size -> "+ ${named.size - shown.size} más en la captura"
                        else -> null
                    },
                    warnings = warnings
                )
            }

            "E2" -> ChatCard(
                kind = "RESULT", title = "Leí la foto", badge = "+ IA",
                rows = listOfNotNull(
                    joined(str("marca"), str("modelo"))?.let { ChatResultField("Equipo", it) },
                    str("ubicacionVisual")?.let { ChatResultField("Ubicación", it) },
                    num("puertosLanTotales")?.let { ChatResultField("Puertos LAN", it) },
                    num("puertosLanOcupados")?.let { ChatResultField("Ocupados", it) },
                    num("puertosLanLibres")?.let { ChatResultField("Libres", it) }
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
            "E1", "E1b" -> listOf(
                Triple("bajadaMbps", "Bajada (Mbps)", true), Triple("subidaMbps", "Subida (Mbps)", true),
                Triple("pingMs", "Ping (ms)", true), Triple("proveedor", "Proveedor", false),
                Triple("servidor", "Servidor", false)
            )
            "E2" -> listOf(
                Triple("marca", "Marca", false), Triple("modelo", "Modelo", false),
                Triple("ubicacionVisual", "Ubicación", false),
                Triple("puertosLanTotales", "Puertos LAN (total)", true),
                Triple("puertosLanOcupados", "Puertos LAN ocupados", true),
                Triple("puertosLanLibres", "Puertos LAN libres", true)
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
        // Lo que va entre * se pinta destacado en la galería.
        fun hi(v: String?) = v?.let { "*$it*" }
        val text = when (result.code) {
            "E1", "E1b" -> {
                val load = listOfNotNull(num("latenciaBajadaMs"), num("latenciaSubidaMs"))
                listOfNotNull(
                    listOfNotNull(num("bajadaMbps")?.let { "$it ↓" }, num("subidaMbps")?.let { "$it ↑ Mbps" }).joinToString(" · ")
                        .takeIf { it.isNotBlank() }?.let { hi(it) },
                    num("pingMs")?.let { hi("$it ms") },
                    if (load.size == 2) "⚠ ${load[0]}/${load[1]} ms con carga" else null
                ).joinToString(" · ")
            }
            "E3" -> listOfNotNull(
                hi(joined(str("marca"), str("modelo"))), hi(str("ipGestion")), "credenciales ocultas"
            ).joinToString(" · ")
            "E4" -> listOfNotNull(
                hi(listOfNotNull(str("ipv4")?.let { ".${it.substringAfterLast('.')}" },
                    str("adaptador")?.let { "por $it" }).joinToString(" ")),
                if (x["ethernetDesconectado"] == true) "Ethernet libre" else null
            ).filter { it.isNotBlank() && it != "**" }.joinToString(" · ")
            "E5" -> listOfNotNull(
                hi(joined(str("marca"), str("modelo"))),
                hi(str("ip")?.let { ".${it.substringAfterLast('.')}" + when (x["dhcp"]) { false -> " fija"; true -> " DHCP"; else -> "" } })
            ).joinToString(" · ")
            "E8" -> hi("${(x["dispositivos"] as? List<*>)?.size ?: 0} dispositivos").orEmpty()
            else -> ""
        }
        return text.ifBlank { shortSentence(str("descripcion").orEmpty()) }
    }

    /** Primera frase de lo que describió la IA, recortada para caber en la galería. */
    private fun shortSentence(text: String): String {
        val first = text.substringBefore(". ").trim().trimEnd('.')
        return if (first.length <= 70) first else first.take(67).trimEnd() + "…"
    }

    private fun joined(a: String?, b: String?, sep: String = " "): String? =
        listOfNotNull(a, b).joinToString(sep).takeIf { it.isNotBlank() }

    private fun fmt(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)
}
