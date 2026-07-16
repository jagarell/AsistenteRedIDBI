package com.upc.asistenteredidbi.presentation.common

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/** Guarda y abre/descarga el PDF real de la propuesta técnica (bytes del gateway). */
object PdfFileUtils {

    /** Guarda los bytes en la caché de la app; se puede compartir vía FileProvider. */
    fun saveToCache(context: Context, bytes: ByteArray, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        file.writeBytes(bytes)
        return file
    }

    /** Abre el PDF con el visor que el usuario tenga instalado. */
    fun openPdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Abrir PDF"))
    }

    /**
     * Copia el PDF a la carpeta de Descargas. En Android 10+ usa MediaStore
     * (no requiere permisos); en versiones anteriores usa el almacenamiento
     * externo específico de la app (tampoco requiere permisos, aunque no
     * aparece en la app "Archivos" del sistema, sí en un explorador de archivos
     * bajo Android/data/<paquete>/files/Download).
     */
    fun downloadToPublicDownloads(context: Context, bytes: ByteArray, displayName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                ) ?: return false

                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                true
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return false
                if (!dir.exists()) dir.mkdirs()
                File(dir, displayName).writeBytes(bytes)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
