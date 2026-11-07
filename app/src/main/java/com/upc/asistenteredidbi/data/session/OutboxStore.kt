package com.upc.asistenteredidbi.data.session

import android.content.Context
import com.upc.asistenteredidbi.data.remote.Api007Service
import com.upc.asistenteredidbi.data.remote.MinutaApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cola «por subir» del modo sin conexión (doc 0.0.7 §5). Sin señal, estas acciones quedan guardadas en el celular y se envían solas al
 * volver la conexión: solicitar la validación de una minuta y subir las fotos de una minuta. Se guarda en disco, así sobrevive a cerrar la app.
 */
@Singleton
class OutboxStore @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val api: Api007Service,
    private val minutas: MinutaApiService
) {
    data class Pendiente(val id: String, val tipo: String, val minutaId: Long, val etiqueta: String, val archivo: String? = null, val fotoTipo: String? = null)

    private val carpeta get() = File(ctx.filesDir, "outbox").also { it.mkdirs() }
    private val indice get() = File(carpeta, "cola.json")
    private val candado = Mutex()
    private val _cola = MutableStateFlow(leer())
    val cola: StateFlow<List<Pendiente>> = _cola.asStateFlow()

    /** Última vez que se vació la cola (texto corto para «Modo sin conexión y sincronización»). */
    private val _ultima = MutableStateFlow<Long?>(null)
    val ultimaSubida: StateFlow<Long?> = _ultima.asStateFlow()

    private fun leer(): List<Pendiente> = runCatching {
        if (!indice.exists()) return emptyList()
        val a = JSONArray(indice.readText())
        (0 until a.length()).map { i ->
            a.getJSONObject(i).let { o ->
                Pendiente(o.getString("id"), o.getString("tipo"), o.getLong("minutaId"), o.optString("etiqueta"), o.optString("archivo").ifEmpty { null }, o.optString("fotoTipo").ifEmpty { null })
            }
        }
    }.getOrDefault(emptyList())

    private fun guardar(l: List<Pendiente>) {
        _cola.value = l
        val a = JSONArray()
        l.forEach { p -> a.put(JSONObject().put("id", p.id).put("tipo", p.tipo).put("minutaId", p.minutaId).put("etiqueta", p.etiqueta).put("archivo", p.archivo ?: "").put("fotoTipo", p.fotoTipo ?: "")) }
        runCatching { indice.writeText(a.toString()) }
    }

    suspend fun encolarCompletar(minutaId: Long, etiqueta: String) = candado.withLock {
        if (_cola.value.none { it.tipo == "completar" && it.minutaId == minutaId }) guardar(_cola.value + Pendiente(UUID.randomUUID().toString(), "completar", minutaId, etiqueta))
    }

    suspend fun encolarFoto(minutaId: Long, tipo: String, jpeg: ByteArray, etiqueta: String) = candado.withLock {
        val id = UUID.randomUUID().toString()
        withContext(Dispatchers.IO) { File(carpeta, "$id.jpg").writeBytes(jpeg) }
        guardar(_cola.value + Pendiente(id, "foto", minutaId, etiqueta, "$id.jpg", tipo))
    }

    /** Envía lo pendiente en orden. Si vuelve a faltar la conexión se detiene y deja el resto. Devuelve cuántos cambios se subieron. */
    suspend fun subir(): Int = candado.withLock {
        var subidos = 0
        for (p in _cola.value) {
            val ok = runCatching {
                when (p.tipo) {
                    "completar" -> minutas.completeMinuta(p.minutaId)
                    "foto" -> {
                        val f = File(carpeta, p.archivo.orEmpty())
                        val parte = MultipartBody.Part.createFormData("archivo", "foto.jpg", f.readBytes().toRequestBody("image/jpeg".toMediaType()))
                        api.subirFoto(p.minutaId, (p.fotoTipo ?: "foto").toRequestBody("text/plain".toMediaType()), parte)
                    }
                }
            }
            val error = ok.exceptionOrNull()
            // Sin conexión: se conserva y se reintenta luego. Un rechazo del servidor (p. ej. ya estaba completa) no se reintenta.
            if (error is IOException) break
            if (error is HttpException && error.code() >= 500) break
            if (ok.isSuccess) subidos++
            p.archivo?.let { File(carpeta, it).delete() }
            guardar(_cola.value.filterNot { it.id == p.id })
        }
        if (subidos > 0 || _cola.value.isEmpty()) _ultima.value = System.currentTimeMillis()
        subidos
    }
}
