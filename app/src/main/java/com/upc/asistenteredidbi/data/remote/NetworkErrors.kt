package com.upc.asistenteredidbi.data.remote

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/**
 * Traduce cualquier excepción de red/HTTP a un mensaje que el técnico pueda
 * entender, en vez de dejar pasar texto crudo tipo "HTTP 403" hasta un
 * Toast — el JWT dura 60 minutos, así que un 401/403 en cualquier pantalla
 * después de haber iniciado sesión casi siempre significa sesión expirada,
 * no falta de permisos. El mensaje que mande el backend en el error body
 * ({"message": "..."}) tiene prioridad sobre el default por código.
 */
fun Throwable.toFriendlyMessage(codeOverrides: Map<Int, String> = emptyMap()): String = when (this) {
    is HttpException -> extractBackendMessage() ?: codeOverrides[code()] ?: defaultMessageForCode(code())
    is IOException -> "No se pudo conectar. Revisa tu conexión a internet."
    else -> message ?: "Ocurrió un error inesperado."
}

private fun HttpException.extractBackendMessage(): String? = try {
    val body = response()?.errorBody()?.string()
    if (body.isNullOrBlank()) null else JSONObject(body).optString("message").takeIf { it.isNotBlank() }
} catch (_: Exception) {
    null
}

private fun defaultMessageForCode(code: Int): String = when (code) {
    401, 403 -> "Tu sesión expiró. Cierra sesión y vuelve a iniciar sesión."
    404 -> "No se encontró lo que buscabas."
    409 -> "Los datos ingresados ya existen."
    in 500..599 -> "El servidor tuvo un problema. Intenta de nuevo en unos segundos."
    else -> "No se pudo completar la solicitud (código $code)."
}
