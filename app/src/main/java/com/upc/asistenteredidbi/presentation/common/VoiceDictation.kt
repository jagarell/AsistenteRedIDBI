package com.upc.asistenteredidbi.presentation.common

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.WeakHashMap

/**
 * Dictado por voz: graba el audio en la app y el servidor lo transcribe (el reconocimiento del teléfono fallaba con los
 * términos del oficio: AnyDesk, switch, IP, nombres de áreas). Un toque empieza a grabar y otro termina; el texto se
 * entrega a [onText] para que el técnico lo revise antes de enviarlo.
 *
 * Se crea como propiedad del fragmento (registra el permiso del micrófono) y se conecta al botón con [toggle].
 */
class VoiceDictation(
    private val fragment: Fragment,
    private val transcribir: suspend (File) -> Pair<String, String?>,
    private val onText: (String) -> Unit,
    private val onState: (grabando: Boolean, ocupado: Boolean) -> Unit
) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private val main = Handler(Looper.getMainLooper())
    private val tope = Runnable { if (recorder != null) detener() }
    private var ocupado = false

    private val permiso = fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) iniciar() else aviso("Permite el micrófono para dictar.")
    }

    init {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) { cancelar() }
        })
    }

    fun toggle() {
        if (ocupado) return
        if (recorder != null) { detener(); return }
        val ctx = fragment.requireContext()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) iniciar()
        else permiso.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun aviso(m: String) {
        if (fragment.context != null) Toast.makeText(fragment.requireContext(), m, Toast.LENGTH_LONG).show()
    }

    private fun iniciar() {
        val ctx = fragment.requireContext()
        val f = File.createTempFile("dictado_", ".m4a", ctx.cacheDir)
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(16000)
            r.setAudioEncodingBitRate(64000)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
        } catch (e: Exception) {
            runCatching { r.release() }
            f.delete()
            aviso("No se pudo usar el micrófono. Escribe tu respuesta.")
            return
        }
        recorder = r; file = f
        onState(true, false)
        aviso("Grabando… toca el micrófono otra vez para terminar.")
        main.postDelayed(tope, MAX_MS)
    }

    private fun detener() {
        main.removeCallbacks(tope)
        val r = recorder ?: return
        val f = file
        recorder = null; file = null
        val ok = try { r.stop(); true } catch (e: RuntimeException) { false } // dura muy poco: no hay audio
        runCatching { r.release() }
        if (f == null) { onState(false, false); return }
        if (!ok || f.length() < 1500) {
            f.delete(); onState(false, false)
            aviso("No se escuchó nada. Mantén la grabación un poco más.")
            return
        }
        ocupado = true
        onState(false, true)
        fragment.lifecycleScope.launch {
            val resultado = try {
                transcribir(f)
            } catch (e: IOException) {
                "" to "Sin conexión: no se pudo transcribir. Escribe tu respuesta."
            } catch (e: Exception) {
                "" to "No se pudo transcribir el audio. Inténtalo de nuevo."
            } finally {
                f.delete()
            }
            ocupado = false
            if (fragment.view == null) return@launch
            onState(false, false)
            val (texto, error) = resultado
            if (texto.isNotBlank()) onText(texto) else aviso(error ?: "No se entendió el audio. Inténtalo de nuevo.")
        }
    }

    private fun cancelar() {
        main.removeCallbacks(tope)
        recorder?.let { runCatching { it.stop() }; runCatching { it.release() } }
        recorder = null
        file?.delete(); file = null
    }

    companion object {
        private const val MAX_MS = 120_000L
        private val tintOriginal = WeakHashMap<ImageView, android.content.res.ColorStateList?>()

        /** Pinta el botón del micrófono: rojo mientras graba y atenuado mientras transcribe. */
        fun pintar(boton: View?, grabando: Boolean, ocupado: Boolean) {
            if (boton == null) return
            boton.isEnabled = !ocupado
            boton.alpha = if (ocupado) 0.45f else 1f
            if (boton is ImageView) {
                if (!tintOriginal.containsKey(boton)) tintOriginal[boton] = boton.imageTintList
                boton.imageTintList = if (grabando) android.content.res.ColorStateList.valueOf(0xFFD32F2F.toInt()) else tintOriginal[boton]
            }
        }
    }
}
