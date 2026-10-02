package com.upc.asistenteredidbi.data.session

import android.content.Context
import android.net.Uri
import com.upc.asistenteredidbi.data.util.MultipartUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Archivos locales del chat de una evaluación:
 *  - fotos de evidencia ya comprimidas (las que se suben y las que se ven como
 *    miniatura en el chat y en la galería), y
 *  - el `state` del flujo en cada pregunta, para poder reabrirla ("Editar
 *    respuesta" / "Cambiar foto") sin volver a pedirle nada al servidor. Se
 *    guardan en archivos y no dentro del snapshot del chat para no inflar el
 *    DataStore con un estado completo por cada mensaje.
 */
@Singleton
class ChatLocalFiles @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun photosDir(evaluationId: Long) = File(context.filesDir, "chat_photos/$evaluationId")
    private fun statesDir(evaluationId: Long) = File(context.filesDir, "chat_states/$evaluationId")

    /** Comprime cada foto (misma compresión que las demás subidas) y la deja en filesDir. */
    suspend fun savePhotos(evaluationId: Long, uris: List<Uri>): List<File> = withContext(Dispatchers.IO) {
        val dir = photosDir(evaluationId).apply { mkdirs() }
        uris.map { uri ->
            val temp = MultipartUtils.uriToTempFile(context, uri, prefix = "chat_photo_")
            val target = File(dir, "${UUID.randomUUID()}.jpg")
            temp.copyTo(target, overwrite = true)
            temp.delete()
            target
        }
    }

    /** Reemplaza el contenido de las fotos locales por las versiones procesadas por el servidor (base64). */
    suspend fun replaceWithProcessed(files: List<File>, processed: List<String>) = withContext(Dispatchers.IO) {
        files.zip(processed).forEach { (file, base64) ->
            if (base64.isNotBlank()) {
                file.writeBytes(android.util.Base64.decode(base64, android.util.Base64.DEFAULT))
            }
        }
    }

    suspend fun saveState(evaluationId: Long, messageId: Long, state: String) = withContext(Dispatchers.IO) {
        val dir = statesDir(evaluationId).apply { mkdirs() }
        File(dir, "$messageId.json").writeText(state)
    }

    suspend fun loadState(evaluationId: Long, messageId: Long): String? = withContext(Dispatchers.IO) {
        File(statesDir(evaluationId), "$messageId.json").takeIf { it.exists() }?.readText()
    }

    /** Borra los estados guardados de mensajes desde `fromMessageId` en adelante (al volver atrás). */
    suspend fun dropStatesFrom(evaluationId: Long, fromMessageId: Long) = withContext(Dispatchers.IO) {
        statesDir(evaluationId).listFiles()?.forEach { file ->
            val id = file.nameWithoutExtension.toLongOrNull()
            if (id != null && id >= fromMessageId) file.delete()
        }
    }

    suspend fun clear(evaluationId: Long) = withContext(Dispatchers.IO) {
        photosDir(evaluationId).deleteRecursively()
        statesDir(evaluationId).deleteRecursively()
    }
}
