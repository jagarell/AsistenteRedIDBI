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
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.domain.model.ChatTopology
import kotlin.math.max

/**
 * Dibuja la topología estructurada construida a partir del chat: un ícono por
 * tipo de dispositivo (router, switch, POS, cámara...) sobre una insignia de
 * color, con su etiqueta debajo, ordenados por nivel (fila) y conectados por
 * enlaces coloreados/estilizados según el tipo de conexión y el estado (con
 * falla → rojo).
 *
 * Pensada para vivir dentro de un HorizontalScrollView: el ancho intrínseco es
 * el del contenido completo (no se re-escala), para que los diagramas anchos
 * se puedan desplazar en vez de amontonarse. Si el contenido es más angosto
 * que el espacio disponible, se centra en vez de quedar pegado a la izquierda.
 *
 * [zoomEnabled] habilita pellizcar-para-zoom y arrastrar — pensado para la
 * vista de pantalla completa (ver [TopologyZoomDialog]), no para la vista
 * chica embebida en la Propuesta Técnica (ahí competiría con el scroll de la
 * pantalla).
 */
class TopologyGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class NodePosition(val cx: Float, val cy: Float)

    private val density = resources.displayMetrics.density
    private val nodeWidth = 132 * density
    private val badgeDiameter = 56 * density
    private val nodeHeight = 96 * density
    private val hGap = 18 * density
    private val vGap = 46 * density
    private val padding = 16 * density
    private val iconInset = 14 * density

    private var topology: ChatTopology? = null
    private var positions: Map<String, NodePosition> = emptyMap()
    private var contentWidth = 0f
    private var contentHeight = 0f

    /** Cuánto centrar horizontalmente el contenido cuando es más angosto que
     * el ancho disponible (se recalcula en onMeasure). */
    private var centerOffsetX = 0f

    var zoomEnabled = false

    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(1f, 4f)
                invalidate()
                return true
            }
        }
    )

    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#263238")
        textAlign = Paint.Align.CENTER
        textSize = 11.5f * density
        isFakeBoldText = true
    }
    private val linkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }

    fun setTopology(newTopology: ChatTopology?) {
        topology = newTopology
        scaleFactor = 1f
        translateX = 0f
        translateY = 0f
        computeLayout()
        requestLayout()
        invalidate()
    }

    private fun computeLayout() {
        val t = topology
        if (t == null || t.nodes.isEmpty()) {
            positions = emptyMap()
            contentWidth = 0f
            contentHeight = 0f
            return
        }

        val byLevel = t.nodes.groupBy { it.level }.toSortedMap()
        val maxRowSize = byLevel.values.maxOf { it.size }.coerceAtLeast(1)

        contentWidth = padding * 2 + maxRowSize * nodeWidth + (maxRowSize - 1) * hGap
        contentHeight = padding * 2 + byLevel.size * nodeHeight + (byLevel.size - 1).coerceAtLeast(0) * vGap

        val map = mutableMapOf<String, NodePosition>()
        byLevel.values.forEachIndexed { rowIndex, nodesInRow ->
            val rowWidth = nodesInRow.size * nodeWidth + (nodesInRow.size - 1) * hGap
            val startX = (contentWidth - rowWidth) / 2f
            val cy = padding + rowIndex * (nodeHeight + vGap) + badgeDiameter / 2f
            nodesInRow.forEachIndexed { colIndex, node ->
                val cx = startX + colIndex * (nodeWidth + hGap) + nodeWidth / 2f
                map[node.id] = NodePosition(cx, cy)
            }
        }
        positions = map
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec)
        val w = if (contentWidth > 0f) max(contentWidth.toInt(), available) else available
        val h = if (contentHeight > 0f) contentHeight.toInt() else (nodeHeight + padding * 2).toInt()
        centerOffsetX = if (w > contentWidth) (w - contentWidth) / 2f else 0f
        setMeasuredDimension(w, h)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!zoomEnabled) return super.onTouchEvent(event)

        scaleDetector.onTouchEvent(event)

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = topology ?: return
        if (t.nodes.isEmpty()) return

        canvas.save()
        if (zoomEnabled) {
            canvas.translate(translateX, translateY)
            canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)
        }
        canvas.translate(centerOffsetX, 0f)

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

            val radius = badgeDiameter / 2f
            canvas.drawLine(from.cx, from.cy + radius, to.cx, to.cy - radius, linkPaint)
        }

        // Íconos encima.
        t.nodes.forEach { node ->
            val pos = positions[node.id] ?: return@forEach
            drawBadge(canvas, node.type, pos.cx, pos.cy)
            drawLabel(canvas, node.label, pos.cx, pos.cy + badgeDiameter / 2f + 8 * density)
        }

        canvas.restore()
    }

    private fun drawBadge(canvas: Canvas, type: String, cx: Float, cy: Float) {
        badgePaint.color = colorForType(type)
        val radius = badgeDiameter / 2f
        canvas.drawCircle(cx, cy, radius, badgePaint)

        val icon = iconFor(type) ?: return
        val half = radius - iconInset
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
        val maxWidth = nodeWidth - 8 * density
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
