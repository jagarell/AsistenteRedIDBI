package com.upc.asistenteredidbi.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * CameraX/el selector de imágenes entregan un `Uri`; Retrofit/OkHttp
 * necesitan un `File` real para el multipart. Compartido por
 * EvidenceRepositoryImpl y ProfileRepositoryImpl.
 */
object MultipartUtils {

    /** Lado más largo al que se reduce la foto antes de subirla. OpenAI
     *  Vision (motor de análisis de evidencias, ver app.vision en
     *  idbi-fastapi) reescala internamente cualquier imagen a un rango
     *  similar antes de "verla" — subir más resolución que esto no le da
     *  más detalle al análisis de IA, solo pesa más. */
    private const val MAX_DIMENSION = 2048
    private const val JPEG_QUALITY = 90

    /** @throws PhotoProcessingException si la imagen no se pudo decodificar/
     *  comprimir — antes esto silenciosamente subía el archivo original tal
     *  cual, pero etiquetado como `image/jpeg` sin importar qué fuera
     *  realmente (HEIC, un archivo corrupto, etc.), sin que nadie lo
     *  validara ni en el backend ni en el análisis de IA. Mejor fallar acá
     *  con un mensaje claro que el técnico pueda entender. */
    fun uriToTempFile(context: Context, uri: Uri, prefix: String = "upload_"): File {
        val compressed = runCatching { compressToJpeg(context, uri) }.getOrNull()
            ?: throw PhotoProcessingException("No se pudo procesar esta foto, intenta con otra.")

        val tempFile = File.createTempFile(prefix, ".jpg", context.cacheDir)
        tempFile.outputStream().use { it.write(compressed) }
        return tempFile
    }

    private fun compressToJpeg(context: Context, uri: Uri): ByteArray? {
        val resolver = context.contentResolver

        // 1) Solo leer las dimensiones primero, para poder submuestrear al
        //    decodificar — una foto de 48MP sin submuestrear puede agotar la
        //    memoria del proceso.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        // 2) La cámara guarda la rotación real como metadato EXIF, no en los
        //    píxeles. Al recomprimir hay que "hornearla" en la imagen, o
        //    quedaría de lado tanto en el checklist como en lo que recibe la IA.
        val rotationDegrees = readExifRotationDegrees(resolver, uri)
        val rotated = if (rotationDegrees != 0) rotate(decoded, rotationDegrees) else decoded

        // 3) El submuestreo del paso 1 es por potencias de 2 y puede pasarse
        //    del máximo — se ajusta al tamaño final exacto.
        val resized = downscaleIfNeeded(rotated, MAX_DIMENSION)

        val bytes = ByteArrayOutputStream().use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }

        if (resized !== rotated) resized.recycle()
        if (rotated !== decoded) rotated.recycle()
        decoded.recycle()

        return bytes
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        while (width / (sampleSize * 2) >= maxDimension && height / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun downscaleIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / largestSide
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun readExifRotationDegrees(resolver: android.content.ContentResolver, uri: Uri): Int {
        val orientation = resolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun filePart(file: File, partName: String = "file"): MultipartBody.Part {
        val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, file.name, requestBody)
    }

    fun textPart(value: String) = value.toRequestBody("text/plain".toMediaTypeOrNull())
}

class PhotoProcessingException(message: String) : Exception(message)
