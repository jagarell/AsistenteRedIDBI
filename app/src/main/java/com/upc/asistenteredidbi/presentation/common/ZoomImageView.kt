package com.upc.asistenteredidbi.presentation.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.min

/**
 * Hoja del PDF con zoom: la hoja se ajusta al ancho de la pantalla (si no entra entera se desliza hacia abajo);
 * pellizcar con dos dedos, doble toque o `zoomBy` acercan de 100 % a 400 %; con zoom se desplaza en las dos direcciones.
 * Sin zoom, el deslizamiento horizontal se lo deja al ViewPager2 (cambio de página).
 */
class ZoomImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val matrixValues = FloatArray(9)
    private val drawMatrix = Matrix()
    private var fitScale = 1f
    /** Zoom relativo al ajuste al ancho: 1 = 100 %. */
    var zoom = 1f
        private set
    var onZoomChanged: ((Float) -> Unit)? = null

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            applyZoom(zoom * d.scaleFactor, d.focusX, d.focusY)
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (zoom > 1.05f) applyZoom(1f, e.x, e.y) else applyZoom(2.5f, e.x, e.y)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (scaleDetector.isInProgress) return false
            pan(-dx, -dy)
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        zoom = 1f
        post { resetToFit() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetToFit()
    }

    private fun resetToFit() {
        val d = drawable ?: return
        if (width == 0) return
        fitScale = width.toFloat() / d.intrinsicWidth
        zoom = 1f
        drawMatrix.reset()
        drawMatrix.postScale(fitScale, fitScale)
        imageMatrix = drawMatrix
        clamp()
        onZoomChanged?.invoke(zoom)
    }

    fun zoomBy(factor: Float) = applyZoom(zoom * factor, width / 2f, height / 2f)

    private fun applyZoom(target: Float, fx: Float, fy: Float) {
        val z = target.coerceIn(1f, 4f)
        val factor = z / zoom
        drawMatrix.postScale(factor, factor, fx, fy)
        zoom = z
        clamp()
        onZoomChanged?.invoke(zoom)
    }

    private fun pan(dx: Float, dy: Float) {
        drawMatrix.postTranslate(dx, dy)
        clamp()
    }

    /** Mantiene la hoja dentro de la vista: centrada si es más chica, sin huecos si es más grande. */
    private fun clamp() {
        val d = drawable ?: return
        val rect = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        drawMatrix.mapRect(rect)
        var tx = 0f
        var ty = 0f
        tx = if (rect.width() <= width) (width - rect.width()) / 2f - rect.left else when {
            rect.left > 0 -> -rect.left
            rect.right < width -> width - rect.right
            else -> 0f
        }
        ty = if (rect.height() <= height) (height - rect.height()) / 2f - rect.top else when {
            rect.top > 0 -> -rect.top
            rect.bottom < height -> height - rect.bottom
            else -> 0f
        }
        drawMatrix.postTranslate(tx, ty)
        imageMatrix = drawMatrix
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        // Con zoom, la hoja se mueve en las dos direcciones: el ViewPager2 no debe quitarnos el gesto.
        val canScrollX = run {
            val d = drawable ?: return@run false
            drawMatrix.getValues(matrixValues)
            d.intrinsicWidth * matrixValues[Matrix.MSCALE_X] > width + 1
        }
        parent?.requestDisallowInterceptTouchEvent(canScrollX || event.pointerCount > 1)
        return true
    }
}
