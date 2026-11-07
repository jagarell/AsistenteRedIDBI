package com.upc.asistenteredidbi.presentation.perfil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.TextView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.upc.asistenteredidbi.data.remote.Api007Service
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Foto de perfil (doc 0.0.7 §35): JPG/PNG de hasta 3 MB. Se guarda en el teléfono para mostrarla sin conexión en el menú y en Mi perfil;
 * dentro de los módulos se siguen viendo las iniciales. Se reduce a 640 px antes de subirla.
 */
@Singleton
class FotoPerfilStore @Inject constructor(@ApplicationContext private val ctx: Context, private val api: Api007Service) {

    private val archivo get() = File(ctx.filesDir, "foto_perfil.jpg")
    private val _foto = MutableStateFlow<Bitmap?>(leer())
    val foto: StateFlow<Bitmap?> = _foto.asStateFlow()

    private fun leer(): Bitmap? = runCatching { if (archivo.exists()) BitmapFactory.decodeFile(archivo.path) else null }.getOrNull()

    /** Trae la foto de la cuenta (o la quita si ya no hay). Sin conexión se queda con la del teléfono. */
    suspend fun sincronizar() {
        val r = runCatching { api.fotoPerfil() }.getOrNull() ?: return
        withContext(Dispatchers.IO) {
            if (r.code() == 204) {
                archivo.delete(); _foto.value = null
            } else if (r.isSuccessful) {
                r.body()?.bytes()?.let { archivo.writeBytes(it); _foto.value = leer() }
            }
        }
    }

    /** Al cerrar sesión: la foto de una cuenta no se ve en otra. */
    fun limpiar() {
        archivo.delete(); _foto.value = null
    }

    /** Reduce, sube y guarda. Devuelve un mensaje de error o null si salió bien. */
    suspend fun subir(uri: Uri): String? = withContext(Dispatchers.IO) {
        val original = runCatching { ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } }.getOrNull()
            ?: return@withContext "No se pudo leer la imagen."
        val lado = 640f / maxOf(original.width, original.height).coerceAtLeast(1)
        val bmp = if (lado < 1f) Bitmap.createScaledBitmap(original, (original.width * lado).toInt(), (original.height * lado).toInt(), true) else original
        val out = ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }.toByteArray()
        val parte = MultipartBody.Part.createFormData("file", "perfil.jpg", out.toRequestBody("image/jpeg".toMediaType()))
        runCatching { api.fotoPerfilSubir(parte) }.fold(
            onSuccess = { archivo.writeBytes(out); _foto.value = leer(); null },
            onFailure = { "No se pudo subir la foto. Revisa tu conexión." }
        )
    }

    suspend fun quitar(): String? = withContext(Dispatchers.IO) {
        runCatching { api.fotoPerfilQuitar() }.fold(
            onSuccess = { archivo.delete(); _foto.value = null; null },
            onFailure = { "No se pudo quitar la foto. Revisa tu conexión." }
        )
    }

    /** Pinta la foto en un avatar circular o, si no hay, las iniciales sobre su fondo. */
    fun pintar(avatar: TextView, iniciales: String, fondoIniciales: android.graphics.drawable.Drawable?, foto: Bitmap?) {
        if (foto == null) {
            avatar.text = iniciales; avatar.background = fondoIniciales
        } else {
            avatar.text = ""
            avatar.background = RoundedBitmapDrawableFactory.create(avatar.resources, foto).apply { isCircular = true; setAntiAlias(true) }
        }
    }
}
