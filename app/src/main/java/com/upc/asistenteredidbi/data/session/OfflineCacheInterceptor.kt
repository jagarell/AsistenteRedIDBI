package com.upc.asistenteredidbi.data.session

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modo sin conexión (doc 0.0.7 §5): la app trabaja primero en el celular. Cada consulta al servidor que sale bien (Inicio, Reportes,
 * Notificaciones, Panel, listas de minutas…) se guarda en el celular y, cuando no hay señal, se muestra la última copia en vez de
 * un error. Lo que cambia datos (guardar, validar, subir) no pasa por aquí: va a la cola «por subir». Se borra al cerrar sesión.
 */
@Singleton
class OfflineCacheInterceptor @Inject constructor(@ApplicationContext private val ctx: Context) : Interceptor {

    private val dir get() = File(ctx.filesDir, "offline_cache").also { it.mkdirs() }

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val path = req.url.encodedPath
        val cacheable = req.method == "GET" && path.startsWith("/api/") && EXCLUIDOS.none { path.contains(it) }
        if (!cacheable) return chain.proceed(req)
        return try {
            val resp = chain.proceed(req)
            if (resp.isSuccessful && resp.header("Content-Type").orEmpty().contains("json")) {
                val texto = resp.peekBody(LIMITE.toLong() + 1).string()
                if (texto.length <= LIMITE) runCatching { archivo(req.url.toString()).writeText(texto) }
            }
            resp
        } catch (e: IOException) {
            val f = archivo(req.url.toString())
            if (!f.exists()) throw e
            Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(200).message("OK (copia del celular)")
                .header("X-Offline-Cache", "1")
                .body(f.readText().toResponseBody("application/json".toMediaType())).build()
        }
    }

    private fun archivo(url: String) = File(dir, MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) })

    /** Al cerrar sesión: lo guardado de una cuenta no se muestra en otra. */
    fun limpiar() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        const val LIMITE = 3_000_000
        val EXCLUIDOS = listOf("/auth/", "/pdf", ".xlsx", "/foto", "/fotos", "/uploads", "/adjunto", "/copias", "/descargar")
    }
}
