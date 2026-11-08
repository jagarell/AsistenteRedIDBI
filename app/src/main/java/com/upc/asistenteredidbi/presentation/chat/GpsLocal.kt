package com.upc.asistenteredidbi.presentation.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.upc.asistenteredidbi.presentation.reportes.Ui007
import com.upc.asistenteredidbi.presentation.reportes.Ui007.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ubicación GPS del local al pedir la dirección (docs/0.0.7/03-gps-direccion.md): toma la posición del teléfono, la API devuelve
 * calle, distrito, departamento y si un lugar cercano coincide con el nombre del local, y el técnico elige «Usar esta dirección»
 * o «Ver en el mapa» (Google Maps). Las coordenadas quedan guardadas en la minuta como enlace al mapa.
 */
object GpsLocal {

    data class Fix(val lat: Double, val lng: Double, val acc: Int, val t: String) {
        fun mapaUrl() = "https://www.google.com/maps?q=" + String.format(Locale.US, "%.6f,%.6f", lat, lng)
        fun texto() = String.format(Locale.US, "%.5f, %.5f", lat, lng)
    }

    fun tienePermiso(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun sello(): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date())

    /** Una sola lectura de alta precisión (GPS, y red si el GPS no responde); si en 15 s no hay nada, devuelve null. */
    @Suppress("MissingPermission", "DEPRECATION")
    fun ubicar(ctx: Context, onFix: (Fix?) -> Unit) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null || !tienePermiso(ctx)) { onFix(null); return }
        val main = Handler(Looper.getMainLooper())
        var listo = false
        val oyentes = mutableListOf<LocationListener>()
        fun entrega(l: Location?) {
            if (listo) return
            listo = true
            oyentes.forEach { runCatching { lm.removeUpdates(it) } }
            onFix(l?.let { Fix(it.latitude, it.longitude, if (it.hasAccuracy()) Math.round(it.accuracy) else 0, sello()) })
        }
        val proveedores = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        if (proveedores.isEmpty()) { entrega(null); return }
        proveedores.forEach { p ->
            val oyente = object : LocationListener {
                override fun onLocationChanged(location: Location) { if (p == LocationManager.GPS_PROVIDER || oyentes.size == 1) entrega(location) }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                @Deprecated("Solo para Android 9 y anteriores") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            oyentes.add(oyente)
            runCatching { lm.requestSingleUpdate(p, oyente, Looper.getMainLooper()) }
        }
        // Si el GPS tarda (interiores), se usa la lectura de red o la última conocida a los 8 s.
        main.postDelayed({
            if (!listo) {
                val ultima = proveedores.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
                if (ultima != null) entrega(ultima)
            }
        }, 8000)
        main.postDelayed({ entrega(null) }, 15000)
    }

    /** Tarjeta «Ubicación GPS del local». [onUsar] recibe la dirección encontrada; [onActualizar] vuelve a tomar la ubicación. */
    @Suppress("UNCHECKED_CAST")
    fun hoja(f: Fragment, fix: Fix, geo: Map<String, Any?>?, nombreLocal: String, onUsar: (String) -> Unit, onActualizar: () -> Unit) {
        val ctx = f.context ?: return
        val sheet = BottomSheetDialog(ctx)
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 18), dp(ctx, 18), dp(ctx, 18), dp(ctx, 20)) }
        col.addView(Ui007.text(ctx, "Ubicación GPS del local", 18f, Ui007.INK, true))
        col.addView(Ui007.text(ctx, "${fix.texto()}  ·  precisión ±${fix.acc} m  ·  ${fix.t}", 13f, Ui007.GREY).apply { setPadding(0, dp(ctx, 2), 0, dp(ctx, 8)) })
        fun fila(k: String, v: String?) { if (!v.isNullOrBlank()) col.addView(Ui007.text(ctx, "$k: $v", 14f, Ui007.INK).apply { setPadding(0, dp(ctx, 6), 0, 0) }) }
        val direccion = geo?.get("direccion")?.toString()?.takeIf { it.isNotBlank() }
        if (direccion != null) {
            fila("Dirección", geo["calle"]?.toString() ?: direccion)
            val dist = geo["distrito"]?.toString().orEmpty(); val dep = geo["departamento"]?.toString().orEmpty(); val prov = geo["provincia"]?.toString().orEmpty()
            fila("Distrito / Dpto.", listOf(dist, dep).filter { it.isNotBlank() }.joinToString(" / "))
            if (prov.isNotBlank() && prov != dist && prov != "Lima") fila("Provincia", prov)
            val coin = geo["coincidencia"] as? Map<String, Any?>
            if (coin != null) {
                col.addView(Ui007.text(ctx, "✓ Coincide con «${coin["nombre"]}» en el mapa, a ${(coin["distancia_m"] as? Number)?.toInt() ?: 0} m: la ubicación del local queda confirmada.", 13f, 0xFF1B5E20.toInt()).apply { setPadding(0, dp(ctx, 10), 0, 0) })
            } else if (nombreLocal.isBlank()) {
                col.addView(Ui007.text(ctx, "Aún no hay nombre del local para compararlo con el mapa.", 13f, 0xFF8A4B00.toInt()).apply { setPadding(0, dp(ctx, 10), 0, 0) })
            }
        } else {
            col.addView(Ui007.text(ctx, "No se encontró la dirección para estas coordenadas. Escríbela a mano; el enlace al mapa sí se guarda en la minuta.", 13f, 0xFF8A4B00.toInt()).apply { setPadding(0, dp(ctx, 8), 0, 0) })
        }
        if (direccion != null) col.addView(Ui007.button(ctx, "Usar esta dirección", true) { sheet.dismiss(); onUsar(direccion) }.apply { setPadding(0, dp(ctx, 12), 0, dp(ctx, 12)) })
        col.addView(Ui007.button(ctx, "Ver en Google Maps", false) {
            runCatching { f.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fix.mapaUrl()))) }
                .onFailure { Toast.makeText(ctx, "No hay una app para abrir el mapa.", Toast.LENGTH_SHORT).show() }
        }.apply { setPadding(0, dp(ctx, 12), 0, dp(ctx, 12)) })
        col.addView(Ui007.button(ctx, "Actualizar ubicación", false) { sheet.dismiss(); onActualizar() }.apply { setPadding(0, dp(ctx, 12), 0, dp(ctx, 12)) })
        sheet.setContentView(ScrollView(ctx).apply { addView(col) })
        sheet.show()
    }
}
