package com.upc.asistenteredidbi.presentation.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.domain.model.MapImageModel
import com.upc.asistenteredidbi.domain.model.MapLinkModel
import com.upc.asistenteredidbi.domain.model.MapNodeModel
import com.upc.asistenteredidbi.domain.model.MapNodeTypes
import com.upc.asistenteredidbi.domain.model.MapTextModel
import com.upc.asistenteredidbi.domain.model.NetworkMap
import kotlin.math.hypot
import kotlin.math.max

/**
 * Lienzo del mapa de red. Dibuja nodos, enlaces (cable / WiFi / USB / observado),
 * cajas de texto y fotos; en modo edición permite arrastrar elementos, tocar para
 * seleccionar, mantener presionado un equipo y conectar dos elementos. Siempre
 * permite pellizcar para acercar/alejar y arrastrar el fondo para mover el mapa.
 *
 * Las posiciones del modelo son normalizadas (0..1) sobre el área útil de la vista.
 */
class MapCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Mode { VIEW, EDIT, CONNECT }

    /** Elemento seleccionado: kind = node | link | text | image. */
    data class Selection(val kind: String, val id: String)

    interface Listener {
        /** Se va a modificar el mapa (guardar el estado actual para "Deshacer"). */
        fun onBeforeChange() {}
        fun onMapChanged(map: NetworkMap) {}
        fun onSelectionChanged(selection: Selection?) {}
        fun onNodeLongPress(node: MapNodeModel) {}
        /** En modo CONNECT: el técnico tocó origen y destino. */
        fun onConnect(sourceId: String, targetId: String) {}
        fun onConnectSourcePicked(sourceId: String) {}
    }

    var listener: Listener? = null

    var map: NetworkMap = NetworkMap()
        set(value) {
            field = value
            if (selection != null && !exists(selection!!)) selection = null
            invalidate()
        }

    var mode: Mode = Mode.VIEW
        set(value) {
            field = value
            if (value != Mode.CONNECT) connectSourceId = null
            invalidate()
        }

    /** Sin interacción de edición (miniatura dentro del chat): solo se dibuja. */
    var staticPreview: Boolean = false

    var selection: Selection? = null
        private set

    var connectSourceId: String? = null
        private set

    /** Devuelve la foto (ya reducida) de una imagen del mapa. */
    var imageProvider: ((MapImageModel) -> Bitmap?)? = null

    // ---- transformación (zoom / pan) ------------------------------------------------------
    private var scale = 1f
    private var tx = 0f
    private var ty = 0f
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newScale = (scale * detector.scaleFactor).coerceIn(0.5f, 4f)
            val factor = newScale / scale
            tx = detector.focusX - (detector.focusX - tx) * factor
            ty = detector.focusY - (detector.focusY - ty) * factor
            scale = newScale
            invalidate()
            return true
        }
    })

    // ---- pinceles ---------------------------------------------------------------------------
    private val density = resources.displayMetrics.density

    /** Unidad de dibujo: la miniatura del chat usa elementos más chicos para que quepan sin encimarse. */
    private val d get() = density * if (staticPreview) 0.55f else 1f
    private val nodeRadius get() = 24f * d
    private val linkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f * density }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f * d }
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F2937"); textSize = 11f * density; typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val detailPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B7280"); textSize = 9f * density; textAlign = Paint.Align.CENTER
    }
    private val noteTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5A3C0A"); typeface = Typeface.DEFAULT_BOLD
    }
    private val observedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C62828"); textSize = 9f * density; textAlign = Paint.Align.CENTER
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val iconCache = HashMap<String, android.graphics.drawable.Drawable?>()

    private val padX get() = 56f * d
    private val padTop get() = 34f * d
    private val padBottom get() = 46f * d

    // ---- coordenadas -------------------------------------------------------------------------
    private fun baseX(nx: Float) = padX + nx * (width - 2 * padX)
    private fun baseY(ny: Float) = padTop + ny * (height - padTop - padBottom)
    private fun normX(bx: Float) = ((bx - padX) / (width - 2 * padX)).coerceIn(0.02f, 0.98f)
    private fun normY(by: Float) = ((by - padTop) / (height - padTop - padBottom)).coerceIn(0.02f, 0.98f)
    private fun toBaseX(sx: Float) = (sx - tx) / scale
    private fun toBaseY(sy: Float) = (sy - ty) / scale

    private fun textWidthPx(size: String) = when (size) { "S" -> 90f; "L" -> 160f; else -> 120f } * d
    private fun imageWidthPx(size: String) = when (size) { "S" -> 70f; "L" -> 140f; else -> 100f } * d
    private fun textSizePx(size: String) = when (size) { "S" -> 10f; "L" -> 15f; else -> 12f } * d

    private fun layoutFor(t: MapTextModel): StaticLayout {
        noteTextPaint.textSize = textSizePx(t.size)
        return StaticLayout.Builder.obtain(t.text.ifBlank { " " }, 0, t.text.ifBlank { " " }.length, noteTextPaint, textWidthPx(t.size).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
    }

    private fun textRect(t: MapTextModel): RectF {
        val l = layoutFor(t)
        val w = textWidthPx(t.size) + 16f * d
        val h = l.height + 12f * d
        val cx = baseX(t.x)
        val cy = baseY(t.y)
        return RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
    }

    private fun imageRect(i: MapImageModel): RectF {
        val bmp = imageProvider?.invoke(i)
        val w = imageWidthPx(i.size)
        val h = if (bmp != null) w * bmp.height / bmp.width else w * 0.7f
        val cx = baseX(i.x)
        val cy = baseY(i.y)
        return RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
    }

    private fun nodePos(n: MapNodeModel) = baseX(n.x) to baseY(n.y)

    // ---- dibujo ---------------------------------------------------------------------------------
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        labelPaint.textSize = 11f * d
        detailPaint.textSize = 9f * d
        observedPaint.textSize = 9f * d
        canvas.save()
        canvas.translate(tx, ty)
        canvas.scale(scale, scale)

        val byId = map.nodes.associateBy { it.id }

        for (l in map.links) {
            val a = byId[l.source]?.let { nodePos(it) } ?: continue
            val b = byId[l.target]?.let { nodePos(it) } ?: continue
            drawLink(canvas, l, a, b)
        }

        for (img in map.images) drawImage(canvas, img)
        for (n in map.nodes) drawNode(canvas, n)
        for (t in map.texts) drawText(canvas, t)

        canvas.restore()
    }

    private fun drawLink(canvas: Canvas, l: MapLinkModel, a: Pair<Float, Float>, b: Pair<Float, Float>) {
        val selected = selection?.kind == "link" && selection?.id == l.id
        linkPaint.color = when {
            selected -> Color.parseColor("#1565C0")
            l.observed -> Color.parseColor("#C62828")
            else -> Color.parseColor("#78909C")
        }
        linkPaint.strokeWidth = (if (selected) 4f else 2.5f) * d
        linkPaint.pathEffect = when {
            l.observed || l.style == "dashed" -> DashPathEffect(floatArrayOf(10f * d, 6f * d), 0f)
            l.style == "dotted" -> DashPathEffect(floatArrayOf(2f * d, 6f * d), 0f)
            else -> null
        }
        canvas.drawLine(a.first, a.second, b.first, b.second, linkPaint)
        linkPaint.pathEffect = null
        if (l.observed) {
            canvas.drawText("WiFi: debería ir por cable", (a.first + b.first) / 2 - 24f * d, (a.second + b.second) / 2 - 6f * d, observedPaint)
        }
    }

    private fun drawNode(canvas: Canvas, n: MapNodeModel) {
        val (cx, cy) = nodePos(n)
        val color = Color.parseColor(n.color ?: MapNodeTypes.colorHex(n.type))
        if (n.pending) {
            fillPaint.color = Color.WHITE
            canvas.drawCircle(cx, cy, nodeRadius, fillPaint)
            ringPaint.color = color
            ringPaint.pathEffect = DashPathEffect(floatArrayOf(6f * d, 4f * d), 0f)
            canvas.drawCircle(cx, cy, nodeRadius, ringPaint)
            ringPaint.pathEffect = null
        } else {
            fillPaint.color = color
            canvas.drawCircle(cx, cy, nodeRadius, fillPaint)
        }
        icon(n.type)?.let {
            val s = (nodeRadius * 0.9f).toInt()
            it.setBounds((cx - s / 2).toInt(), (cy - s / 2).toInt(), (cx + s / 2).toInt(), (cy + s / 2).toInt())
            it.setTint(if (n.pending) color else Color.WHITE)
            it.draw(canvas)
        }
        val selected = selection?.kind == "node" && selection?.id == n.id
        if (selected) {
            ringPaint.color = Color.parseColor("#1565C0")
            canvas.drawCircle(cx, cy, nodeRadius + 4f * d, ringPaint)
        }
        if (connectSourceId == n.id) {
            ringPaint.color = Color.parseColor("#00838F")
            ringPaint.pathEffect = DashPathEffect(floatArrayOf(5f * d, 4f * d), 0f)
            canvas.drawCircle(cx, cy, nodeRadius + 5f * d, ringPaint)
            ringPaint.pathEffect = null
        }
        canvas.drawText(n.label.take(if (staticPreview) 18 else 26), cx, cy + nodeRadius + 14f * d, labelPaint)
        n.detail?.takeIf { it.isNotBlank() }?.let { canvas.drawText(it.take(30), cx, cy + nodeRadius + 26f * d, detailPaint) }
    }

    private fun drawText(canvas: Canvas, t: MapTextModel) {
        val r = textRect(t)
        fillPaint.color = Color.parseColor(t.color.takeIf { it.startsWith("#") } ?: "#FFF4CC")
        canvas.drawRoundRect(r, 8f * d, 8f * d, fillPaint)
        ringPaint.color = Color.parseColor("#B0822C")
        ringPaint.strokeWidth = 1.5f * d
        canvas.drawRoundRect(r, 8f * d, 8f * d, ringPaint)
        ringPaint.strokeWidth = 3f * d
        canvas.save()
        canvas.translate(r.left + 8f * d, r.top + 6f * d)
        layoutFor(t).draw(canvas)
        canvas.restore()
        if (selection?.kind == "text" && selection?.id == t.id) drawHandles(canvas, r, "#1565C0")
    }

    private fun drawImage(canvas: Canvas, i: MapImageModel) {
        val r = imageRect(i)
        val bmp = imageProvider?.invoke(i)
        if (bmp != null) {
            canvas.drawBitmap(bmp, null, r, null)
        } else {
            fillPaint.color = Color.parseColor("#CFD8DC")
            canvas.drawRect(r, fillPaint)
        }
        ringPaint.color = Color.parseColor("#2E7D32")
        ringPaint.strokeWidth = 2f * d
        canvas.drawRect(r, ringPaint)
        ringPaint.strokeWidth = 3f * d
        if (i.label.isNotBlank()) canvas.drawText(i.label.take(24), r.centerX(), r.bottom + 12f * d, labelPaint)
        if (selection?.kind == "image" && selection?.id == i.id) drawHandles(canvas, r, "#2E7D32")
    }

    private fun drawHandles(canvas: Canvas, r: RectF, color: String) {
        handlePaint.color = Color.parseColor(color)
        val s = 4f * d
        for ((x, y) in listOf(r.left to r.top, r.right to r.top, r.left to r.bottom, r.right to r.bottom)) {
            canvas.drawRect(x - s, y - s, x + s, y + s, handlePaint)
        }
    }

    private fun icon(type: String): android.graphics.drawable.Drawable? = iconCache.getOrPut(type) {
        val res = when (type) {
            "internet" -> R.drawable.ic_network
            "router" -> R.drawable.ic_router
            "switch" -> R.drawable.ic_server
            "access_point", "repeater" -> R.drawable.ic_wifi
            "computer", "pos" -> R.drawable.ic_monitor
            "printer" -> R.drawable.ic_document
            "camera" -> R.drawable.ic_camera
            else -> R.drawable.ic_devices
        }
        ContextCompat.getDrawable(context, res)?.mutate()
    }

    // ---- consulta de elementos --------------------------------------------------------------------
    private fun exists(sel: Selection) = when (sel.kind) {
        "node" -> map.nodes.any { it.id == sel.id }
        "link" -> map.links.any { it.id == sel.id }
        "text" -> map.texts.any { it.id == sel.id }
        else -> map.images.any { it.id == sel.id }
    }

    fun select(sel: Selection?) {
        selection = sel
        listener?.onSelectionChanged(sel)
        invalidate()
    }

    private fun hit(bx: Float, by: Float): Selection? {
        for (t in map.texts.asReversed()) if (textRect(t).contains(bx, by)) return Selection("text", t.id)
        for (n in map.nodes.asReversed()) {
            val (cx, cy) = nodePos(n)
            if (hypot(bx - cx, by - cy) <= nodeRadius + 6f * d) return Selection("node", n.id)
        }
        for (i in map.images.asReversed()) if (imageRect(i).contains(bx, by)) return Selection("image", i.id)
        val byId = map.nodes.associateBy { it.id }
        for (l in map.links) {
            val a = byId[l.source]?.let { nodePos(it) } ?: continue
            val b = byId[l.target]?.let { nodePos(it) } ?: continue
            if (distToSegment(bx, by, a.first, a.second, b.first, b.second) <= 9f * d) return Selection("link", l.id)
        }
        return null
    }

    private fun distToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        if (len2 == 0f) return hypot(px - ax, py - ay)
        val t = (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0f, 1f)
        return hypot(px - (ax + t * dx), py - (ay + t * dy))
    }

    // ---- gestos ---------------------------------------------------------------------------------------
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var moved = false
    private var dragging: Selection? = null
    private var panning = false
    private var longPressed = false
    private var dragSnapshotTaken = false
    private val touchSlop = 8f * d
    private val longPressRunnable = Runnable {
        val sel = dragging
        if (!moved && sel?.kind == "node" && mode != Mode.CONNECT) {
            map.nodes.firstOrNull { it.id == sel.id }?.let {
                longPressed = true
                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                listener?.onNodeLongPress(it)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (staticPreview) return super.onTouchEvent(event)
        scaleDetector.onTouchEvent(event)
        if (event.pointerCount > 1) {
            removeCallbacks(longPressRunnable)
            dragging = null
            panning = false
            moved = true
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                downX = event.x; downY = event.y; lastX = event.x; lastY = event.y
                moved = false; longPressed = false; dragSnapshotTaken = false
                val h = if (mode == Mode.VIEW) null else hit(toBaseX(event.x), toBaseY(event.y))
                dragging = if (mode == Mode.EDIT) h?.takeIf { it.kind != "link" } else null
                panning = dragging == null
                if (dragging?.kind == "node") postDelayed(longPressRunnable, 550)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!moved && hypot(event.x - downX, event.y - downY) > touchSlop) {
                    moved = true
                    removeCallbacks(longPressRunnable)
                }
                if (!moved || longPressed) return true
                val dx = event.x - lastX
                val dy = event.y - lastY
                lastX = event.x; lastY = event.y
                val sel = dragging
                if (sel != null) dragItem(sel, toBaseX(event.x), toBaseY(event.y))
                else if (panning) { tx += dx; ty += dy; invalidate() }
                return true
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(longPressRunnable)
                if (!moved && !longPressed) handleTap(toBaseX(event.x), toBaseY(event.y))
                if (dragSnapshotTaken) listener?.onMapChanged(map)
                dragging = null; panning = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPressRunnable)
                dragging = null; panning = false
                return true
            }
        }
        return true
    }

    private fun dragItem(sel: Selection, bx: Float, by: Float) {
        if (!dragSnapshotTaken) {
            listener?.onBeforeChange()
            dragSnapshotTaken = true
            if (selection != sel) { selection = sel; listener?.onSelectionChanged(sel) }
        }
        val nx = normX(bx)
        val ny = normY(by)
        map = when (sel.kind) {
            "node" -> map.copy(nodes = map.nodes.map { if (it.id == sel.id) it.copy(x = nx, y = ny) else it })
            "text" -> map.copy(texts = map.texts.map { if (it.id == sel.id) it.copy(x = nx, y = ny) else it })
            else -> map.copy(images = map.images.map { if (it.id == sel.id) it.copy(x = nx, y = ny) else it })
        }
    }

    private fun handleTap(bx: Float, by: Float) {
        val h = hit(bx, by)
        if (mode == Mode.CONNECT) {
            val node = h?.takeIf { it.kind == "node" } ?: return
            val src = connectSourceId
            if (src == null) {
                connectSourceId = node.id
                listener?.onConnectSourcePicked(node.id)
                invalidate()
            } else if (src != node.id) {
                connectSourceId = null
                listener?.onConnect(src, node.id)
                invalidate()
            }
            return
        }
        select(h)
    }

    /** Vuelve el zoom y el desplazamiento al estado inicial. */
    fun resetView() {
        scale = 1f; tx = 0f; ty = 0f
        invalidate()
    }

    /** Foto reducida para dibujar (evita decodificar la original en cada frame). */
    companion object {
        fun decodeThumb(path: String, maxPx: Int = 480): Bitmap? = runCatching {
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
            android.graphics.BitmapFactory.decodeFile(path, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull()

        fun clampSize(size: String, delta: Int): String {
            val order = listOf("S", "M", "L")
            return order[(order.indexOf(size) + delta).coerceIn(0, order.lastIndex)]
        }
    }
}
