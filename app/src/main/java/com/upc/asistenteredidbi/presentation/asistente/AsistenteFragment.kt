package com.upc.asistenteredidbi.presentation.asistente

import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.presentation.common.PdfFileUtils
import com.upc.asistenteredidbi.presentation.reportes.DocBlocksView
import com.upc.asistenteredidbi.presentation.reportes.Ui007
import com.upc.asistenteredidbi.presentation.reportes.Ui007.dp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Pantalla del Asistente IA 0.0.7. Conversación con opciones tocables; las opciones solo se muestran en el último mensaje
 * del asistente para que no se repitan. La vista previa de la minuta de soporte usa los mismos bloques que Reportes.
 */
@AndroidEntryPoint
class AsistenteFragment : Fragment() {

    private val vm: AsistenteViewModel by viewModels()

    private lateinit var scroll: ScrollView
    private lateinit var lista: LinearLayout
    private lateinit var input: EditText
    private lateinit var progress: ProgressBar
    private var pintados = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui007.BG) }

        val header = Ui007.row(ctx).apply { setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 10)); setBackgroundColor(Color.WHITE) }
        header.addView(Ui007.text(ctx, "←", 20f, Ui007.BLUE, true).apply { setPadding(0, 0, dp(ctx, 14), 0); setOnClickListener { findNavController().navigateUp() } })
        header.addView(Ui007.text(ctx, "Asistente IA", 18f, Ui007.INK, true))
        root.addView(header)

        scroll = ScrollView(ctx).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        lista = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 14), dp(ctx, 8), dp(ctx, 14), dp(ctx, 16)) }
        scroll.addView(lista)
        root.addView(scroll)

        progress = ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply { isIndeterminate = true; visibility = View.GONE }
        root.addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 4)))

        val bar = Ui007.row(ctx).apply { setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 12)); setBackgroundColor(Color.WHITE) }
        input = EditText(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            background = Ui007.round(Ui007.BG, 22, ctx, Ui007.LINE); setPadding(dp(ctx, 14), dp(ctx, 10), dp(ctx, 14), dp(ctx, 10))
            textSize = 14f; maxLines = 5; imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, a, _ -> if (a == EditorInfo.IME_ACTION_SEND) { enviar(); true } else false }
        }
        bar.addView(input)
        bar.addView(Ui007.button(ctx, "Enviar", true) { enviar() }.apply {
            (layoutParams as LinearLayout.LayoutParams).apply { topMargin = 0; marginStart = dp(ctx, 8); marginEnd = 0 }
        })
        root.addView(bar)
        return root
    }

    private fun enviar() {
        val t = input.text.toString()
        if (t.isBlank()) return
        input.setText("")
        vm.enviar(t)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.state.collect { render(it) } }
                launch { vm.eventos.collect { evento(it) } }
            }
        }
    }

    private fun render(s: AsistenteUiState) {
        if (context == null) return
        Ui007.gone(progress, !s.cargando)
        input.hint = s.pista
        // Solo se agregan los mensajes nuevos; las opciones de mensajes anteriores se quitan (ya se respondieron).
        if (pintados > s.mensajes.size) { lista.removeAllViews(); pintados = 0 }
        for (i in 0 until lista.childCount) lista.getChildAt(i).findViewWithTag<View>("opciones")?.visibility = View.GONE
        s.mensajes.drop(pintados).forEach { lista.addView(burbuja(it)) }
        pintados = s.mensajes.size
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun burbuja(m: Mensaje): View {
        val ctx = requireContext()
        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (m.deUsuario) Gravity.END else Gravity.START
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 8) }
        }
        val b = Ui007.text(ctx, m.texto, 14f, if (m.deUsuario) Color.WHITE else Ui007.INK).apply {
            background = Ui007.round(if (m.deUsuario) Ui007.BLUE else Color.WHITE, 16, ctx, if (m.deUsuario) null else Ui007.LINE)
            setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9)); maxWidth = (resources.displayMetrics.widthPixels * 0.82).toInt()
            setTextIsSelectable(!m.deUsuario)
        }
        wrap.addView(b)
        m.doc?.let { json ->
            val c = Ui007.card(ctx)
            DocBlocksView.render(ctx, json, c)
            wrap.addView(c)
        }
        if (m.opciones.isNotEmpty()) {
            val fila = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 4), 0, 0) }
            m.opciones.forEach { o -> fila.addView(Ui007.chip(ctx, o.label, o.principal) { vm.opcion(o) }.apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(ctx, 6) }) }
            wrap.addView(HorizontalScrollView(ctx).apply { tag = "opciones"; isHorizontalScrollBarEnabled = false; addView(fila) })
        }
        return wrap
    }

    private fun evento(e: AsistenteEvento) {
        when (e) {
            is AsistenteEvento.NuevaImplementacion -> {
                // El inicio abre la evaluación nueva con el flujo de siempre (incluye el aviso si hay una evaluación previa).
                findNavController().previousBackStackEntry?.savedStateHandle?.set(KEY_NUEVA_IMPLEMENTACION, true)
                findNavController().navigateUp()
            }
            is AsistenteEvento.AbrirPdf -> {
                val ctx = requireContext()
                runCatching {
                    val bytes = Base64.decode(e.base64, Base64.DEFAULT)
                    PdfFileUtils.downloadToPublicDownloads(ctx, bytes, e.nombre)
                    PdfFileUtils.openPdf(ctx, PdfFileUtils.saveToCache(ctx, bytes, e.nombre))
                }.onFailure { Toast.makeText(ctx, "La ficha se guardó, pero no se pudo abrir el PDF.", Toast.LENGTH_LONG).show() }
            }
        }
    }

    companion object {
        const val KEY_NUEVA_IMPLEMENTACION = "asistente_nueva_implementacion"
    }
}
