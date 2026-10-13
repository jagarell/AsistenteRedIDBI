package com.upc.asistenteredidbi.presentation.common

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Abre el PDF una vez y renderiza cada hoja cuando se pide (una a la vez: PdfRenderer solo permite una página abierta). */
class PdfPageSource(file: File, private val widthPx: Int) : AutoCloseable {
    private val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(descriptor)
    private val lock = kotlinx.coroutines.sync.Mutex()
    val pageCount: Int get() = renderer.pageCount

    suspend fun render(index: Int): Bitmap = withContext(Dispatchers.IO) {
        lock.withLock {
            renderer.openPage(index).use { page ->
                val scale = widthPx.toFloat() / page.width
                val bitmap = Bitmap.createBitmap(widthPx, (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    }

    override fun close() {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }
}

/** Renderiza cada página del PDF real (ya generado por el gateway) a un bitmap, para previsualizarlo sin inventar contenido. */
object PdfPageRenderer {

    suspend fun renderPages(file: File, targetWidthPx: Int): List<Bitmap> = withContext(Dispatchers.IO) {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        PdfRenderer(descriptor).use { renderer ->
            (0 until renderer.pageCount).map { index ->
                renderer.openPage(index).use { page ->
                    val scale = targetWidthPx.toFloat() / page.width
                    val bitmap = Bitmap.createBitmap(
                        targetWidthPx,
                        (page.height * scale).toInt(),
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }
}
