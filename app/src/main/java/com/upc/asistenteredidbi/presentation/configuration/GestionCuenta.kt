package com.upc.asistenteredidbi.presentation.configuration

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.provider.MediaStore
import android.text.InputType
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.upc.asistenteredidbi.data.remote.Api007Service
import com.upc.asistenteredidbi.presentation.reportes.Ui007
import com.upc.asistenteredidbi.presentation.reportes.Ui007.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Perfil y configuración (Mi Cuenta) según el documento 0.0.7:
 *  - EMPRESA (todos la ven; solo el administrador la cambia): nombre y logo que salen en la minuta (§16).
 *  - AMBIENTE (administrador): modo demo o producción, irreversible (§31).
 *  - COPIA DE SEGURIDAD (administrador): frecuencia, correo, copia ahora e historial (§6).
 *  - TRAZABILIDAD (administrador): bitácora de la empresa (§1, §24).
 */
class GestionCuenta(
    private val ctx: Context,
    private val raiz: LinearLayout,
    private val api: Api007Service,
    private val scope: CoroutineScope,
    private val admin: Boolean,
    private val elegirLogo: () -> Unit
) {
    private var empresa: Map<String, Any?> = emptyMap()

    fun cargar() {
        scope.launch {
            empresa = runCatching { api.empresa() }.getOrNull() ?: return@launch
            pintar()
            if (admin) cargarAdmin()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun pintar() {
        raiz.removeAllViews()
        titulo("EMPRESA")
        val c = Ui007.card(ctx)
        val fila = Ui007.row(ctx)
        val logo = ImageView(ctx).apply { layoutParams = LinearLayout.LayoutParams(dp(ctx, 120), dp(ctx, 46)); scaleType = ImageView.ScaleType.FIT_START; contentDescription = "Logo de la empresa" }
        (empresa["logo"] as? String)?.substringAfter("base64,")?.let { b64 ->
            runCatching { val b = Base64.decode(b64, Base64.DEFAULT); logo.setImageBitmap(BitmapFactory.decodeByteArray(b, 0, b.size)) }
        }
        if (logo.drawable != null) fila.addView(logo)
        fila.addView(Ui007.text(ctx, empresa["nombre"]?.toString().orEmpty(), 17f, Ui007.INK, true).apply { setPadding(dp(ctx, if (logo.drawable != null) 12 else 0), 0, 0, 0) })
        c.addView(fila)
        if (admin) {
            val nombre = EditText(ctx).apply {
                setText(empresa["nombre"]?.toString().orEmpty()); hint = "Nombre de la empresa"; inputType = InputType.TYPE_CLASS_TEXT
                background = Ui007.round(Ui007.BG, 14, ctx, Ui007.LINE); setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10)); textSize = 14f
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 10) }
            }
            c.addView(nombre)
            c.addView(Ui007.button(ctx, "Guardar nombre", true) { accion("No se pudo guardar el nombre.") { api.empresaNombre(mapOf("nombre" to nombre.text.toString())) } })
            c.addView(Ui007.button(ctx, "Cambiar logo (PNG o JPG, hasta 2 MB)", false) { elegirLogo() })
            c.addView(Ui007.button(ctx, "Restablecer IDBI", false) { accion("No se pudo restablecer.") { api.empresaRestablecer() } })
        } else {
            c.addView(Ui007.text(ctx, "Solo el administrador puede cambiar la empresa.", 13f, Ui007.GREY).apply { setPadding(0, dp(ctx, 8), 0, 0) })
        }
        raiz.addView(c)
        c.addView(Ui007.text(ctx, "El logo y el nombre salen en la cabecera de la minuta, en el PDF y en el pie.", 12f, Ui007.GREY).apply { setPadding(0, dp(ctx, 8), 0, 0) })
    }

    /** Sube el logo elegido (la imagen se envía en base64; el servidor valida tipo y tamaño). */
    fun subirLogo(bytes: ByteArray, mime: String) {
        if (bytes.size > 2 * 1024 * 1024) { Toast.makeText(ctx, "El logo debe pesar hasta 2 MB.", Toast.LENGTH_LONG).show(); return }
        accion("No se pudo cambiar el logo (usa PNG o JPG).") { api.empresaLogo(mapOf("base64" to Base64.encodeToString(bytes, Base64.NO_WRAP), "mime" to mime)) }
    }

    private fun accion(error: String, bloque: suspend () -> Map<String, Any?>) {
        scope.launch {
            runCatching { bloque() }.onSuccess { cargar() }.onFailure { Toast.makeText(ctx, error, Toast.LENGTH_LONG).show() }
        }
    }

    private fun titulo(t: String) {
        raiz.addView(Ui007.text(ctx, t, 12f, Ui007.GREY, true).apply { setPadding(dp(ctx, 4), dp(ctx, 18), 0, dp(ctx, 8)) })
    }

    // ---------------- Administrador ----------------

    @Suppress("UNCHECKED_CAST")
    private suspend fun cargarAdmin() {
        // AMBIENTE
        titulo("AMBIENTE")
        val amb = Ui007.card(ctx)
        val enProd = empresa["enProduccion"] == true
        if (!enProd) {
            amb.addView(Ui007.text(ctx, "Modo demo (pruebas)", 15f, Ui007.INK, true))
            amb.addView(Ui007.text(ctx, "Las minutas de prueba cuentan en Reportes, Inicio y Panel. Al pasar a producción empiezan en 0. No se puede deshacer.", 13f, Ui007.GREY).apply { setPadding(0, dp(ctx, 4), 0, 0) })
            amb.addView(Ui007.button(ctx, "Pasar a producción", true) { confirmarProduccion() })
        } else {
            amb.addView(Ui007.text(ctx, "En producción", 15f, 0xFF2E7D32.toInt(), true))
            amb.addView(Ui007.text(ctx, "desde ${empresa["produccionDesde"]} · por ${empresa["produccionPor"]} · no se puede volver a demo", 13f, Ui007.GREY).apply { setPadding(0, dp(ctx, 4), 0, 0) })
        }
        val demo = empresa["baseConocimientoDemo"] as? Map<String, Any?>
        amb.addView(Ui007.text(ctx, "Base de conocimiento de la IA (etapa demo): ${(demo?.get("minutas") as? Number)?.toInt() ?: 0} minutas con sus evidencias y recomendaciones. No se borra al pasar a producción.", 12f, Ui007.GREY).apply { setPadding(0, dp(ctx, 8), 0, 0) })
        raiz.addView(amb)

        // COPIA DE SEGURIDAD
        titulo("COPIA DE SEGURIDAD")
        val cc = Ui007.card(ctx)
        val copia = empresa["copia"] as? Map<String, Any?> ?: emptyMap()
        var frec = copia["frecuencia"]?.toString() ?: "off"
        val chips = Ui007.row(ctx)
        val correo = EditText(ctx).apply {
            setText(copia["correo"]?.toString().orEmpty()); hint = "Correo de la empresa"; inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            background = Ui007.round(Ui007.BG, 14, ctx, Ui007.LINE); setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10)); textSize = 14f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 10) }
        }
        fun chipsFrec() {
            chips.removeAllViews()
            listOf("off" to "Apagada", "diaria" to "Diaria", "semanal" to "Semanal", "mensual" to "Mensual").forEach { (k, l) ->
                chips.addView(Ui007.chip(ctx, l, frec == k) { frec = k; chipsFrec() }.apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(ctx, 6) })
            }
        }
        chipsFrec()
        cc.addView(Ui007.text(ctx, "Frecuencia de la copia automática", 13f, Ui007.GREY).apply { setPadding(0, 0, 0, dp(ctx, 6)) })
        cc.addView(android.widget.HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; addView(chips) })
        cc.addView(correo)
        cc.addView(Ui007.button(ctx, "Guardar", true) { accion("No se pudo guardar (revisa el correo).") { api.empresaCopia(mapOf("frecuencia" to frec, "correo" to correo.text.toString())) } })
        cc.addView(Ui007.button(ctx, "Hacer copia ahora", false) {
            scope.launch { runCatching { api.copiaAhora() }.onSuccess { Toast.makeText(ctx, "Copia generada.", Toast.LENGTH_SHORT).show(); cargar() }.onFailure { Toast.makeText(ctx, "No se pudo generar la copia.", Toast.LENGTH_LONG).show() } }
        })
        cc.addView(Ui007.text(ctx, "Última copia: ${copia["ultima"] ?: "ninguna"}", 12f, Ui007.GREY).apply { setPadding(0, dp(ctx, 8), 0, 0) })
        val historial = runCatching { api.copias() }.getOrDefault(emptyList())
        historial.take(5).forEach { h ->
            val f = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 8), 0, 0) }
            f.addView(Ui007.text(ctx, "${h["fecha"]} · ${h["minutas"]} minutas · ${h["kb"]} KB", 13f, Ui007.INK), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            f.addView(Ui007.chip(ctx, "Descargar", true) { descargar((h["id"] as Number).toLong()) })
            cc.addView(f)
        }
        raiz.addView(cc)

        // TRAZABILIDAD
        titulo("TRAZABILIDAD")
        val tc = Ui007.card(ctx)
        val eventos = runCatching { api.bitacora() }.getOrDefault(emptyList())
        if (eventos.isEmpty()) tc.addView(Ui007.text(ctx, "Todavía no hay movimientos.", 13f, Ui007.GREY))
        tc.addView(Ui007.text(ctx, "Nada se borra: cada acción queda registrada con quién y cuándo.", 12f, Ui007.GREY).apply { setPadding(0, 0, 0, dp(ctx, 6)) })
        eventos.take(25).forEach { e ->
            tc.addView(Ui007.text(ctx, "${e["ts"]} · ${e["usuario"]}\n${e["accion"]}", 12.5f, Ui007.INK).apply { setPadding(0, dp(ctx, 6), 0, dp(ctx, 2)) })
        }
        raiz.addView(tc)
    }

    private fun descargar(id: Long) {
        scope.launch {
            runCatching {
                val bytes = api.copiaDescargar(id).use { it.bytes() }
                val values = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, "copia-seguridad-$id.zip"); put(MediaStore.Downloads.MIME_TYPE, "application/zip") }
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("sin uri")
                ctx.contentResolver.openOutputStream(uri)!!.use { it.write(bytes) }
            }.onSuccess { Toast.makeText(ctx, "Copia guardada en Descargas.", Toast.LENGTH_LONG).show() }
                .onFailure { Toast.makeText(ctx, "No se pudo descargar la copia.", Toast.LENGTH_LONG).show() }
        }
    }

    /** Hoja que explica qué pasa y pide escribir «PRODUCCIÓN» para habilitar el botón. */
    private fun confirmarProduccion() {
        val sheet = BottomSheetDialog(ctx)
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 18), dp(ctx, 18), dp(ctx, 18), dp(ctx, 20)) }
        col.addView(Ui007.text(ctx, "Pasar a producción", 18f, Ui007.INK, true))
        col.addView(Ui007.text(ctx, "No se puede deshacer.\n\n" +
            "• Reportes, indicadores de Inicio y Panel empiezan en 0.\n" +
            "• Las minutas demo Completas, En revisión y Validadas se conservan (etapa demo, no cuentan). Las que están en Borrador o Pendiente dejan de verse.\n" +
            "• Respuestas, evidencias y recomendaciones de la etapa demo quedan como base de conocimiento de la IA.\n" +
            "• Reglas, preguntas, equipo y datos de la empresa se conservan.\n" +
            "• Solo quedan las notificaciones de minutas Completas, En revisión o Validadas.", 13.5f, Ui007.INK).apply { setPadding(0, dp(ctx, 8), 0, dp(ctx, 12)) })
        val campo = EditText(ctx).apply {
            hint = "Escribe PRODUCCIÓN"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            background = Ui007.round(Ui007.BG, 14, ctx, Ui007.LINE); setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10)); textSize = 14f
        }
        col.addView(campo)
        val ok = Ui007.button(ctx, "Pasar a producción", true) {}.apply { alpha = 0.4f; isEnabled = false }
        campo.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val v = s.toString().trim().uppercase().replace("Ó", "O") == "PRODUCCION"
                ok.isEnabled = v; ok.alpha = if (v) 1f else 0.4f
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        ok.setOnClickListener {
            if (!ok.isEnabled) return@setOnClickListener
            sheet.dismiss()
            accion("No se pudo pasar a producción.") { api.empresaProduccion() }
        }
        col.addView(ok)
        col.addView(Ui007.button(ctx, "Cancelar", false) { sheet.dismiss() })
        sheet.setContentView(android.widget.ScrollView(ctx).apply { addView(col) })
        sheet.show()
    }
}
