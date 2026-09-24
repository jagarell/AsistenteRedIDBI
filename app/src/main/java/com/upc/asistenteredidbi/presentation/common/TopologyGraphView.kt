package com.upc.asistenteredidbi.presentation.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.domain.model.ChatTopology
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Dibuja la topología estructurada del chat como un diagrama tipo "estrella":
 * el troncal (Internet → Router) apilado arriba, el dispositivo con más
 * conexiones (switch/hub) al centro, y todos sus equipos alrededor en
 * círculo — igual que un diagrama de topología en estrella clásico, en vez
 * de una fila horizontal que crece sin límite con cada equipo nuevo (eso
 * obligaba a hacer scroll/zoom infinito para entender la red completa).
 *
 * Siempre se autoescala para caber completo en el espacio disponible: la
 * vista chica embebida en la Propuesta Técnica lo hace de forma fija (solo
 * de un vistazo, sin gestos, para no competir con el scroll de la
 * pantalla); la vista de pantalla completa ([zoomEnabled]=true) calcula esa
 * misma escala "ver todo" como punto de partida y permite pellizcar para
 * acercarse más si hace falta, o doble tap para volver a "ver todo".
 */
class TopologyGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class NodePosition(val cx: Float, val cy: Float)

    private val density = resources.displayMetrics.density

    // Tamaños "compactos" (vista chica embebida) vs "completos" (pantalla de
    // zoom) — la vista chica usa nodos más pequeños para que quepan más
    // equipos sin tener que reducir tanto la escala general.
    private val nodeWidth get() = (if (zoomEnabled) 118 else 82) * density
    private val badgeDiameter get() = (if (zoomEnabled) 52 else 36) * density
    private val vPitch get() = (if (zoomEnabled) 92 else 66) * density
    private val labelTextSize get() = (if (zoomEnabled) 11.5f else 9f) * density
    private val leafRadiusExtra get() = (if (zoomEnabled) 78 else 54) * density
    private val padding = 20 * density
    private val iconInsetRatio = 0.28f

    private var topology: ChatTopology? = null
    private var positions: Map<String, NodePosition> = emptyMap()
    private var contentWidth = 0f
    private var contentHeight = 0f

    /** Escala "ver todo" (fit-to-view), recalculada en onMeasure. */
    private var fitScale = 1f

    /** true = pantalla completa (pellizcar/arrastrar habilitado); false =
     *  vista chica embebida, siempre a escala fija sin gestos. */
    var zoomEnabled = false

    /** Zoom relativo del usuario POR ENCIMA de [fitScale] (1f = "ver todo"). */
    private var userScale = 1f
    private var translateX = 0f
    private var translateY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                userScale = (userScale * detector.scaleFactor).coerceIn(0.6f, 3.5f)
                invalidate()
                return true
            }
        }
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                userScale = 1f
                translateX = 0f
                translateY = 0f
                invalidate()
                return true
            }
        }
    )

    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val badgeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
    }
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#263238")
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val linkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    fun setTopology(newTopology: ChatTopology?) {
        topology = newTopology
        userScale = 1f
        translateX = 0f
        translateY = 0f
        computeLayout()
        requestLayout()
        invalidate()
    }

    // ---------------------------------------------------------------------
    // Layout: troncal arriba + hub central + equipos en estrella alrededor.
    // ---------------------------------------------------------------------

    private fun computeLayout() {
        val t = topology
        if (t == null || t.nodes.isEmpty()) {
            positions = emptyMap()
            contentWidth = 0f
            contentHeight = 0f
            return
        }

        val nodesById = t.nodes.associateBy { it.id }
        val childrenOf: Map<String, List<String>> = t.links.groupBy({ it.source }, { it.target })
        val parentsOf: Map<String, List<String>> = t.links.groupBy({ it.target }, { it.source })

        // Hub = el nodo con más conexiones salientes (normalmente el switch;
        // si no hay switch, cae en el router). Con un solo nodo, es ese nodo.
        val hub = t.nodes.maxByOrNull { childrenOf[it.id]?.size ?: 0 } ?: t.nodes.first()
        val hubChildren = childrenOf[hub.id].orEmpty()
            .mapNotNull { nodesById[it] }
            .distinctBy { it.id }
        val hubChildIds = hubChildren.map { it.id }.toSet()

        // Troncal: ancestros del hub (Internet → Router), se asume una
        // cadena simple sin ramificarse antes de llegar al hub.
        val backbone = t.nodes
            .filter { it.level < hub.level && it.id != hub.id }
            .sortedBy { it.level }

        // Segundo grado: equipos colgados de un equipo del hub (ej. los
        // "Dispositivos WiFi" de cada Access Point) — no son hijos directos
        // del hub, pero tampoco son parte del troncal.
        val handledIds = (backbone.map { it.id } + hub.id + hubChildIds).toMutableSet()
        val secondDegree = t.nodes.filterNot { handledIds.contains(it.id) }

        val map = mutableMapOf<String, NodePosition>()
        val spokeAngleDeg = mutableMapOf<String, Float>()

        // --- troncal, apilado verticalmente arriba del hub ---
        val hubCx = 0f
        var cy = -(backbone.size) * vPitch
        backbone.forEach { node ->
            map[node.id] = NodePosition(hubCx, cy)
            cy += vPitch
        }
        val hubCy = cy
        map[hub.id] = NodePosition(hubCx, hubCy)

        // --- equipos del hub, repartidos en círculo (dejando un hueco
        // arriba por donde entra la línea del troncal) ---
        val n = hubChildren.size
        val gapDeg = 46f
        val spokeRadius = spokeRadiusFor(n)
        if (n == 1) {
            val deg = 180f
            spokeAngleDeg[hubChildren[0].id] = deg
            val (dx, dy) = offsetForAngle(deg, spokeRadius)
            map[hubChildren[0].id] = NodePosition(hubCx + dx, hubCy + dy)
        } else if (n > 1) {
            val sweep = 360f - gapDeg
            val step = sweep / n
            hubChildren.forEachIndexed { index, node ->
                val deg = gapDeg / 2f + step * (index + 0.5f)
                spokeAngleDeg[node.id] = deg
                val (dx, dy) = offsetForAngle(deg, spokeRadius)
                map[node.id] = NodePosition(hubCx + dx, hubCy + dy)
            }
        }

        // --- segundo grado: mismo ángulo (promedio) que sus padres, un
        // poco más lejos del centro ---
        secondDegree.forEach { node ->
            val parentIds = parentsOf[node.id].orEmpty().filter { spokeAngleDeg.containsKey(it) }
            val angle = if (parentIds.isEmpty()) {
                180f
            } else {
                circularMeanDeg(parentIds.mapNotNull { spokeAngleDeg[it] })
            }
            val radius = spokeRadius + leafRadiusExtra
            val (dx, dy) = offsetForAngle(angle, radius)
            map[node.id] = NodePosition(hubCx + dx, hubCy + dy)
        }

        // Nodos huérfanos (grafo inesperado): los acomodamos igual, en un
        // círculo aparte más externo, para que nunca desaparezcan del mapa.
        val stillMissing = t.nodes.filter { !map.containsKey(it.id) }
        if (stillMissing.isNotEmpty()) {
            val extraRadius = spokeRadius + leafRadiusExtra * 2f
            val step = 360f / stillMissing.size
            stillMissing.forEachIndexed { index, node ->
                val (dx, dy) = offsetForAngle(step * index, extraRadius)
                map[node.id] = NodePosition(hubCx + dx, hubCy + dy)
            }
        }

        positions = map

        // --- bounding box real de todo lo dibujado (nodo + medio ancho de
        // etiqueta + alto de badge/etiqueta) para poder normalizar a (0,0) ---
        val halfLabelW = nodeWidth / 2f
        val topExtent = badgeDiameter / 2f
        val bottomExtent = badgeDiameter / 2f + labelTextSize * 2.6f

        val minX = map.values.minOf { it.cx - halfLabelW }
        val maxX = map.values.maxOf { it.cx + halfLabelW }
        val minY = map.values.minOf { it.cy - topExtent }
        val maxY = map.values.maxOf { it.cy + bottomExtent }

        contentWidth = (maxX - minX) + padding * 2f
        contentHeight = (maxY - minY) + padding * 2f

        val shiftX = padding - minX
        val shiftY = padding - minY
        positions = map.mapValues { (_, pos) -> NodePosition(pos.cx + shiftX, pos.cy + shiftY) }
    }

    /** Radio del círculo de equipos: crece con la cantidad de equipos para
     *  que los íconos no se encimen, con un piso mínimo prolijo. */
    private fun spokeRadiusFor(count: Int): Float {
        val minRadius = (if (zoomEnabled) 118 else 84) * density
        if (count <= 1) return minRadius
        val stepRad = Math.toRadians((320f / count).toDouble())
        val neededForSpacing = (nodeWidth * 0.62f) / max(0.15f, sin(stepRad / 2.0).toFloat())
        return max(minRadius, neededForSpacing)
    }

    /** deg=0 → arriba, crece en sentido horario (0=arriba,90=derecha,180=abajo,270=izquierda). */
    private fun offsetForAngle(deg: Float, radius: Float): Pair<Float, Float> {
        val rad = Math.toRadians(deg.toDouble())
        val dx = radius * sin(rad)
        val dy = -radius * cos(rad)
        return dx.toFloat() to dy.toFloat()
    }

    private fun circularMeanDeg(degs: List<Float>): Float {
        if (degs.isEmpty()) return 180f
        var sx = 0.0
        var sy = 0.0
        degs.forEach { deg ->
            val (dx, dy) = offsetForAngle(deg, 1f)
            sx += dx
            sy += dy
        }
        val meanDeg = Math.toDegrees(atan2(sx, -sy))
        return ((meanDeg + 360.0) % 360.0).toFloat()
    }

    // ---------------------------------------------------------------------
    // Medida: siempre autoescala para que el diagrama entero quepa.
    // ---------------------------------------------------------------------

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableW = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val availableH = MeasureSpec.getSize(heightMeasureSpec).toFloat()

        if (contentWidth <= 0f || contentHeight <= 0f) {
            fitScale = 1f
            val h = (badgeDiameter + padding * 2f).toInt()
            setMeasuredDimension(availableW.toInt(), h)
            return
        }

        if (zoomEnabled) {
            // La pantalla de zoom mide EXACTO (match_parent dentro de un
            // FrameLayout) — usamos ese tamaño real para calcular "ver todo".
            fitScale = min(availableW / contentWidth, availableH / contentHeight)
                .coerceIn(0.25f, 1.6f)
            setMeasuredDimension(availableW.toInt(), availableH.toInt())
        } else {
            // Vista chica: ancho fijo por el padre, alto se ajusta al
            // contenido ya escalado (nunca agranda más allá del tamaño
            // natural, solo achica si hace falta).
            fitScale = (availableW / contentWidth).coerceIn(0.3f, 1f)
            val h = (contentHeight * fitScale).toInt()
            setMeasuredDimension(availableW.toInt(), h)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!zoomEnabled) return super.onTouchEvent(event)

        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && !scaleDetector.isInProgress) {
                    translateX += event.x - lastTouchX
                    translateY += event.y - lastTouchY
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    // ---------------------------------------------------------------------
    // Dibujo.
    // ---------------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = topology ?: return
        if (t.nodes.isEmpty()) return

        val effectiveScale = fitScale * userScale
        val centerOffsetX = (width - contentWidth * effectiveScale) / 2f
        val centerOffsetY = (height - contentHeight * effectiveScale) / 2f

        canvas.save()
        if (zoomEnabled) {
            canvas.translate(translateX, translateY)
        }
        canvas.translate(centerOffsetX, centerOffsetY)
        canvas.scale(effectiveScale, effectiveScale)

        linkPaint.strokeWidth = 2.5f * density

        // Enlaces primero (quedan debajo de los íconos).
        t.links.forEach { link ->
            val from = positions[link.source] ?: return@forEach
            val to = positions[link.target] ?: return@forEach

            linkPaint.pathEffect = when (link.connectionType.uppercase()) {
                "WIFI" -> DashPathEffect(floatArrayOf(12f, 8f), 0f)
                "USB_BLUETOOTH" -> DashPathEffect(floatArrayOf(3f, 6f), 0f)
                else -> null
            }
            linkPaint.color = if (link.status.equals("CON_FALLA", ignoreCase = true)) {
                Color.parseColor("#D32F2F")
            } else {
                Color.parseColor("#90A4AE")
            }

            drawLinkBetweenBadges(canvas, from, to)
        }

        // Íconos encima.
        t.nodes.forEach { node ->
            val pos = positions[node.id] ?: return@forEach
            drawBadge(canvas, node.type, pos.cx, pos.cy)
            drawLabel(canvas, node.label, pos.cx, pos.cy + badgeDiameter / 2f + 6 * density)
        }

        canvas.restore()
    }

    /** Traza la línea entre los bordes de las dos insignias (no de centro a
     *  centro), para que no quede tapada por los íconos. */
    private fun drawLinkBetweenBadges(canvas: Canvas, from: NodePosition, to: NodePosition) {
        val dx = to.cx - from.cx
        val dy = to.cy - from.cy
        val dist = kotlin.math.hypot(dx, dy)
        if (dist < 1f) return
        val radius = badgeDiameter / 2f
        val ux = dx / dist
        val uy = dy / dist
        canvas.drawLine(
            from.cx + ux * radius, from.cy + uy * radius,
            to.cx - ux * radius, to.cy - uy * radius,
            linkPaint
        )
    }

    private fun drawBadge(canvas: Canvas, type: String, cx: Float, cy: Float) {
        badgePaint.color = colorForType(type)
        val radius = badgeDiameter / 2f
        canvas.drawCircle(cx, cy, radius, badgePaint)
        badgeStrokePaint.strokeWidth = 2f * density
        canvas.drawCircle(cx, cy, radius, badgeStrokePaint)

        val icon = iconFor(type) ?: return
        val half = radius * (1f - iconInsetRatio)
        icon.setBounds(
            (cx - half).toInt(), (cy - half).toInt(),
            (cx + half).toInt(), (cy + half).toInt()
        )
        icon.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        icon.draw(canvas)
    }

    private fun iconFor(type: String): Drawable? {
        val res = when (type.lowercase()) {
            "internet" -> R.drawable.ic_network
            "router" -> R.drawable.ic_router
            "switch" -> R.drawable.ic_server
            "access_point" -> R.drawable.ic_wifi
            "pos" -> R.drawable.ic_monitor
            "printer" -> R.drawable.ic_document
            "camera" -> R.drawable.ic_camera
            "computer" -> R.drawable.ic_monitor
            else -> R.drawable.ic_network
        }
        return ContextCompat.getDrawable(context, res)?.mutate()
    }

    private fun drawLabel(canvas: Canvas, text: String, cx: Float, top: Float) {
        labelPaint.textSize = labelTextSize
        val maxWidth = nodeWidth - 6 * density
        val lines = wrapToTwoLines(text, maxWidth)
        val lineHeight = labelPaint.textSize * 1.2f
        var y = top + labelPaint.textSize
        lines.forEach { line ->
            canvas.drawText(line, cx, y, labelPaint)
            y += lineHeight
        }
    }

    /** Ajusta la etiqueta a máximo 2 líneas, con puntos suspensivos si no cabe. */
    private fun wrapToTwoLines(text: String, maxWidth: Float): List<String> {
        if (labelPaint.measureText(text) <= maxWidth) {
            return listOf(text)
        }
        val words = text.split(" ")
        val firstLine = StringBuilder()
        var index = 0
        while (index < words.size) {
            val candidate = if (firstLine.isEmpty()) words[index] else "$firstLine ${words[index]}"
            if (labelPaint.measureText(candidate) > maxWidth && firstLine.isNotEmpty()) break
            firstLine.append(if (firstLine.isEmpty()) words[index] else " ${words[index]}")
            index++
        }
        val remaining = words.drop(index).joinToString(" ")
        if (remaining.isBlank()) {
            return listOf(firstLine.toString())
        }
        val secondLine = TextUtils.ellipsize(remaining, labelPaint, maxWidth, TextUtils.TruncateAt.END).toString()
        return listOf(firstLine.toString(), secondLine)
    }

    private fun colorForType(type: String): Int = when (type.lowercase()) {
        "internet" -> Color.parseColor("#37474F")
        "router" -> Color.parseColor("#1565C0")
        "switch" -> Color.parseColor("#2E7D32")
        "access_point" -> Color.parseColor("#7B1FA2")
        "pos" -> Color.parseColor("#EF6C00")
        "printer" -> Color.parseColor("#616161")
        "camera" -> Color.parseColor("#C62828")
        "computer" -> Color.parseColor("#00838F")
        else -> Color.parseColor("#455A64")
    }
}
