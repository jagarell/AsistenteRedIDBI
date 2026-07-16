package com.upc.asistenteredidbi.presentation.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import com.upc.asistenteredidbi.domain.model.ChatTopology

/**
 * Dibuja la topología estructurada construida a partir del chat: un nodo por
 * dispositivo, ordenados por nivel (fila), con enlaces coloreados/estilizados
 * según el tipo de conexión y el estado (con falla → rojo).
 *
 * Pensada para vivir dentro de un HorizontalScrollView: el ancho intrínseco es
 * el del contenido completo (no se re-escala), para que los diagramas anchos
 * se puedan desplazar en vez de amontonarse.
 */
class TopologyGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class NodePosition(val cx: Float, val cy: Float)

    private val density = resources.displayMetrics.density
    private val nodeWidth = 132 * density
    private val nodeHeight = 52 * density
    private val hGap = 18 * density
    private val vGap = 50 * density
    private val padding = 16 * density
    private val cornerRadius = 10 * density

    private var topology: ChatTopology? = null
    private var positions: Map<String, NodePosition> = emptyMap()
    private var contentWidth = 0f
    private var contentHeight = 0f

    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
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
            val cy = padding + rowIndex * (nodeHeight + vGap) + nodeHeight / 2f
            nodesInRow.forEachIndexed { colIndex, node ->
                val cx = startX + colIndex * (nodeWidth + hGap) + nodeWidth / 2f
                map[node.id] = NodePosition(cx, cy)
            }
        }
        positions = map
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = if (contentWidth > 0f) contentWidth.toInt() else MeasureSpec.getSize(widthMeasureSpec)
        val h = if (contentHeight > 0f) contentHeight.toInt() else (nodeHeight + padding * 2).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = topology ?: return
        if (t.nodes.isEmpty()) return

        // Enlaces primero (quedan debajo de los nodos).
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

            canvas.drawLine(from.cx, from.cy + nodeHeight / 2f, to.cx, to.cy - nodeHeight / 2f, linkPaint)
        }

        // Nodos encima.
        t.nodes.forEach { node ->
            val pos = positions[node.id] ?: return@forEach
            nodePaint.color = colorForType(node.type)

            val rect = RectF(
                pos.cx - nodeWidth / 2f, pos.cy - nodeHeight / 2f,
                pos.cx + nodeWidth / 2f, pos.cy + nodeHeight / 2f
            )
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, nodePaint)
            drawLabel(canvas, node.label, pos.cx, pos.cy)
        }
    }

    private fun drawLabel(canvas: Canvas, text: String, cx: Float, cy: Float) {
        val maxWidth = nodeWidth - 14 * density
        val lines = wrapToTwoLines(text, maxWidth)
        val lineHeight = textPaint.textSize * 1.15f
        var y = cy - (lineHeight * (lines.size - 1)) / 2f + textPaint.textSize * 0.35f
        lines.forEach { line ->
            canvas.drawText(line, cx, y, textPaint)
            y += lineHeight
        }
    }

    /** Ajusta la etiqueta a máximo 2 líneas, con puntos suspensivos si no cabe. */
    private fun wrapToTwoLines(text: String, maxWidth: Float): List<String> {
        if (textPaint.measureText(text) <= maxWidth) {
            return listOf(text)
        }
        val words = text.split(" ")
        val firstLine = StringBuilder()
        var index = 0
        while (index < words.size) {
            val candidate = if (firstLine.isEmpty()) words[index] else "$firstLine ${words[index]}"
            if (textPaint.measureText(candidate) > maxWidth && firstLine.isNotEmpty()) break
            firstLine.append(if (firstLine.isEmpty()) words[index] else " ${words[index]}")
            index++
        }
        val remaining = words.drop(index).joinToString(" ")
        if (remaining.isBlank()) {
            return listOf(firstLine.toString())
        }
        val secondLine = TextUtils.ellipsize(remaining, textPaint, maxWidth, TextUtils.TruncateAt.END).toString()
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
