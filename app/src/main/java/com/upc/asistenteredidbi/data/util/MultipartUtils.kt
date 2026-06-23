package com.upc.asistenteredidbi.data.util

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * CameraX/el selector de imágenes entregan un `Uri`; Retrofit/OkHttp
 * necesitan un `File` real para el multipart. Compartido por
 * ChatRepositoryImpl, EvidenceRepositoryImpl y ProfileRepositoryImpl.
 */
object MultipartUtils {

    fun uriToTempFile(context: Context, uri: Uri, prefix: String = "upload_"): File {
        val tempFile = File.createTempFile(prefix, ".jpg", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        return tempFile
    }

    fun filePart(file: File, partName: String = "file"): MultipartBody.Part {
        val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, file.name, requestBody)
    }

    fun textPart(value: String) = value.toRequestBody("text/plain".toMediaTypeOrNull())
}
