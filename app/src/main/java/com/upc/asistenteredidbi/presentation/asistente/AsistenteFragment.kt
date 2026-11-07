package com.upc.asistenteredidbi.presentation.asistente

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.upc.asistenteredidbi.presentation.common.PdfFileUtils
import com.upc.asistenteredidbi.presentation.reportes.DocBlocksView
import com.upc.asistenteredidbi.presentation.reportes.Ui007
import com.upc.asistenteredidbi.presentation.reportes.Ui007.dp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Asistente IA 0.0.8 (doc 01): conversación libre, sin botones de opciones. Barra inferior con 📎 adjuntar evidencias, 🎤 audio
 * (se transcribe y se revisa antes de enviar), caja de texto y enviar; botón para reiniciar la conversación. Las respuestas del
 * servidor llegan como tarjetas (borrador de minuta, actualizar minuta, consulta a la base, pendientes, reportes…).
 */
@AndroidEntryPoint
class AsistenteFragment : Fragment() {

    private val vm: AsistenteViewModel by viewModels()

    private lateinit var scroll: ScrollView
    private lateinit var lista: LinearLayout
    private lateinit var input: EditText
    private lateinit var progress: ProgressBar
    private lateinit var subtitulo: TextView
    private lateinit var chips: LinearLayout
    private lateinit var chipsScroll: HorizontalScrollView
    private var pintados = 0
    private var tipoAdjunto: String? = null

    private val elegirFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val tipo = tipoAdjunto
        tipoAdjunto = null
        if (uri != null && tipo != null) vm.agregarAdjunto(tipo, uri.toString())
    }

    private val dictar = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val t = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (t.isNotBlank()) {
                input.setText(if (input.text.isBlank()) t else input.text.toString().trimEnd() + " " + t)
                input.setSelection(input.text.length)
                Toast.makeText(requireContext(), "Audio transcrito. Revísalo y envíalo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui007.BG) }

        // Encabezado azul: volver, avatar, título con «En línea» y reiniciar.
        val header = Ui007.row(ctx).apply {
            setBackgroundColor(Ui007.BLUE); setPadding(dp(ctx, 12), dp(ctx, 38), dp(ctx, 12), dp(ctx, 14))
        }
        header.addView(Ui007.text(ctx, "←", 24f, Color.WHITE, true).apply {
            setPadding(dp(ctx, 8), 0, dp(ctx, 12), 0); setOnClickListener { findNavController().navigateUp() }
        })
        header.addView(Ui007.text(ctx, "✦", 20f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER; background = Ui007.round(0x33FFFFFF, 24, ctx); layoutParams = LinearLayout.LayoutParams(dp(ctx, 42), dp(ctx, 42))
        })
        val titulos = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 12), 0, 0, 0) }
        titulos.addView(Ui007.text(ctx, "Asistente IA", 18f, Color.WHITE, true))
        subtitulo = Ui007.text(ctx, "En línea", 12f, 0xCCFFFFFF.toInt())
        titulos.addView(subtitulo)
        header.addView(titulos, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(Ui007.text(ctx, "↻", 22f, Color.WHITE, true).apply {
            contentDescription = "Reiniciar la conversación"
            setPadding(dp(ctx, 12), dp(ctx, 4), dp(ctx, 8), dp(ctx, 4)); setOnClickListener { reiniciar() }
        })
        root.addView(header)

        scroll = ScrollView(ctx).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        lista = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 14), dp(ctx, 8), dp(ctx, 14), dp(ctx, 16)) }
        scroll.addView(lista)
        root.addView(scroll)

        progress = ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply { isIndeterminate = true; visibility = View.GONE }
        root.addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 4)))

        // Evidencias adjuntas, antes de enviar.
        chips = Ui007.row(ctx).apply { setPadding(dp(ctx, 12), dp(ctx, 6), dp(ctx, 12), 0) }
        chipsScroll = HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; visibility = View.GONE; setBackgroundColor(Color.WHITE); addView(chips) }
        root.addView(chipsScroll)

        val bar = Ui007.row(ctx).apply { setPadding(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 12)); setBackgroundColor(Color.WHITE) }
        bar.addView(circulo(ctx, "📎", "Adjuntar evidencias") { adjuntar() })
        bar.addView(circulo(ctx, "🎤", "Dictar un audio") { dictarAudio() }.apply { (layoutParams as LinearLayout.LayoutParams).marginStart = dp(ctx, 8) })
        input = EditText(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(ctx, 8) }
            background = Ui007.round(Ui007.BG, 22, ctx, Ui007.LINE); setPadding(dp(ctx, 14), dp(ctx, 10), dp(ctx, 14), dp(ctx, 10))
            hint = "Escribe o dicta…"; textSize = 14f; maxLines = 5; imeOptions = EditorInfo.IME_ACTION_SEND
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setOnEditorActionListener { _, a, _ -> if (a == EditorInfo.IME_ACTION_SEND) { enviar(); true } else false }
        }
        bar.addView(input)
        bar.addView(Ui007.text(ctx, "➤", 18f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER; background = Ui007.round(Ui007.BLUE, 24, ctx); contentDescription = "Enviar"
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 46), dp(ctx, 46)).apply { marginStart = dp(ctx, 8) }
            setOnClickListener { enviar() }
        })
        root.addView(bar)
        return root
    }

    private fun circulo(ctx: android.content.Context, glifo: String, desc: String, onClick: () -> Unit): TextView =
        Ui007.text(ctx, glifo, 18f, Ui007.BLUE, true).apply {
            gravity = Gravity.CENTER; contentDescription = desc
            background = Ui007.round(Color.WHITE, 24, ctx, Ui007.LINE)
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 44), dp(ctx, 44))
            setOnClickListener { onClick() }
        }

    private fun enviar() {
        val t = input.text.toString()
        input.setText("")
        vm.enviar(t)
    }

    private fun reiniciar() {
        lista.removeAllViews(); pintados = 0
        vm.reiniciar()
    }

    private fun dictarAudio() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Cuéntame…")
        }
        runCatching { dictar.launch(i) }.onFailure { Toast.makeText(requireContext(), "Este teléfono no tiene dictado por voz. Escribe el mensaje.", Toast.LENGTH_LONG).show() }
    }

    // ---------------- Adjuntar evidencias ----------------

    private val tiposEvidencia = listOf(
        "Frontis del local", "Speedtest", "ipconfig", "Escáner de IP", "Ubicación del router", "Puertos del router",
        "Etiqueta del router", "AnyDesk de caja", "Equipo de caja", "Selftest de impresora", "Etiqueta de impresora", "Conexiones de impresora",
        "Lugar de preparación", "Zona de atención", "Switch", "Punto de red", "Documento (RUC, factura o dirección)", "Otra foto"
    )

    private fun adjuntar() {
        val ctx = requireContext()
        val sheet = BottomSheetDialog(ctx)
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 16), dp(ctx, 18), dp(ctx, 16), dp(ctx, 16)) }
        col.addView(Ui007.text(ctx, "Adjuntar evidencias", 18f, Ui007.INK, true))
        col.addView(Ui007.text(ctx, "Elige qué foto envías. En la app tomas o subes la foto y la IA reconoce qué es.", 13f, Ui007.GREY).apply { setPadding(0, dp(ctx, 4), 0, dp(ctx, 10)) })
        val grid = GridLayout(ctx).apply { columnCount = 3 }
        tiposEvidencia.forEach { t ->
            grid.addView(Ui007.text(ctx, t, 12f, Ui007.INK, false).apply {
                gravity = Gravity.CENTER; background = Ui007.round(Color.WHITE, 14, ctx, Ui007.LINE)
                setPadding(dp(ctx, 6), dp(ctx, 16), dp(ctx, 6), dp(ctx, 16))
                layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
                    width = 0; height = dp(ctx, 74); setMargins(dp(ctx, 3), dp(ctx, 3), dp(ctx, 3), dp(ctx, 3))
                }
                setOnClickListener { tipoAdjunto = t; sheet.dismiss(); elegirFoto.launch("image/*") }
            })
        }
        col.addView(grid)
        col.addView(Ui007.button(ctx, "Cerrar", false) { sheet.dismiss() })
        sheet.setContentView(ScrollView(ctx).apply { addView(col) })
        sheet.show()
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
        val ctx = requireContext()
        Ui007.gone(progress, !s.cargando)
        subtitulo.text = s.subtitulo
        input.hint = s.pista
        chips.removeAllViews()
        s.adjuntos.forEachIndexed { i, a ->
            chips.addView(Ui007.chip(ctx, "📷 ${a.tipo}  ✕", true) { vm.quitarAdjunto(i) }.apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(ctx, 6) })
        }
        Ui007.gone(chipsScroll, s.adjuntos.isEmpty())
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
        if (m.progreso) { wrap.addView(progresoRedaccion()); return wrap }
        if (m.texto.isNotBlank() || m.adjuntos.isNotEmpty()) {
            val texto = (if (m.adjuntos.isNotEmpty()) m.adjuntos.joinToString("\n") { "📷 $it" } + (if (m.texto.isNotBlank()) "\n" else "") else "") + m.texto
            wrap.addView(Ui007.text(ctx, texto, 14f, if (m.deUsuario) Color.WHITE else Ui007.INK).apply {
                background = Ui007.round(if (m.deUsuario) Ui007.BLUE else Color.WHITE, 16, ctx, if (m.deUsuario) null else Ui007.LINE)
                setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9)); maxWidth = (resources.displayMetrics.widthPixels * 0.82).toInt()
                setTextIsSelectable(!m.deUsuario)
            })
        }
        m.bloques.forEach { wrap.addView(tarjeta(it)) }
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

    /** «Redactando la minuta»: los cinco pasos que ve el técnico mientras el servidor arma el documento. */
    private fun progresoRedaccion(): View {
        val ctx = requireContext()
        val c = Ui007.card(ctx)
        val fila = Ui007.row(ctx)
        fila.addView(Ui007.text(ctx, "Redactando la minuta", 15f, Ui007.INK, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        fila.addView(Ui007.pill(ctx, "IA", 0xFFE3F2FD.toInt(), Ui007.BLUE))
        c.addView(fila)
        listOf("Leyendo las evidencias", "Extrayendo los datos del cliente", "Armando la topología", "Aplicando las reglas del rubro", "Redactando la minuta").forEach {
            c.addView(Ui007.text(ctx, "✓  $it", 14f, Ui007.INK).apply { setPadding(0, dp(ctx, 8), 0, 0) })
        }
        return c
    }

    // ---------------- Tarjetas ----------------

    @Suppress("UNCHECKED_CAST")
    private fun tarjeta(b: Map<String, Any?>): View {
        val ctx = requireContext()
        val tipo = b["tipo"]?.toString().orEmpty()
        if (tipo == "texto" || tipo == "texto_ia" && b["titulo"] == null) {
            val ia = tipo == "texto_ia"
            return LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                if (ia) addView(Ui007.pill(ctx, "IA", 0xFFE3F2FD.toInt(), Ui007.BLUE).apply { (layoutParams as? LinearLayout.LayoutParams)?.bottomMargin = dp(ctx, 4) })
                addView(Ui007.text(ctx, b["texto"]?.toString().orEmpty(), 14f, Ui007.INK).apply {
                    background = Ui007.round(Color.WHITE, 16, ctx, Ui007.LINE); setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9))
                    maxWidth = (resources.displayMetrics.widthPixels * 0.82).toInt(); setTextIsSelectable(true)
                })
            }
        }
        val card = Ui007.card(ctx)
        val cab = Ui007.row(ctx)
        cab.addView(Ui007.text(ctx, b["titulo"]?.toString().orEmpty(), 15f, Ui007.INK, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        (b["badge"] as? String)?.let { cab.addView(Ui007.pill(ctx, it, 0xFFE3F2FD.toInt(), Ui007.BLUE)) }
        card.addView(cab)
        (b["texto"] as? String)?.takeIf { it.isNotBlank() }?.let { card.addView(Ui007.text(ctx, it, 13f, Ui007.GREY).apply { setPadding(0, dp(ctx, 4), 0, 0) }) }

        (b["filas"] as? List<List<String>>)?.forEach { f ->
            val r = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 8), 0, 0); gravity = Gravity.TOP }
            r.addView(Ui007.text(ctx, f[0], 13f, Ui007.GREY), LinearLayout.LayoutParams(dp(ctx, 112), LinearLayout.LayoutParams.WRAP_CONTENT))
            val sinDato = f[1] == "Sin dato"
            r.addView(Ui007.text(ctx, f[1], 13f, if (sinDato) 0xFFB45309.toInt() else Ui007.INK, !sinDato), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(r)
        }
        (b["lineas"] as? List<String>)?.forEach { l ->
            card.addView(Ui007.text(ctx, "• $l", 13f, Ui007.INK).apply { setPadding(dp(ctx, 4), dp(ctx, 6), 0, 0) })
        }
        (b["items"] as? List<Map<String, Any?>>)?.takeIf { tipo != "actualizar_minuta" }?.forEach { it0 ->
            val fila = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL; background = Ui007.round(Color.WHITE, 12, ctx, Ui007.LINE)
                setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 8) }
                setOnClickListener { (it0["minutaId"] as? Number)?.toLong()?.let { id -> vm.abrirMinuta(id) } }
            }
            fila.addView(Ui007.text(ctx, it0["titulo"]?.toString().orEmpty(), 14f, Ui007.INK, true))
            fila.addView(Ui007.text(ctx, it0["detalle"]?.toString().orEmpty(), 12f, Ui007.GREY))
            card.addView(fila)
        }
        if (tipo == "actualizar_minuta") {
            val cb = CheckBox(ctx).apply {
                text = b["nuevaVisitaTexto"]?.toString() ?: "Fue una nueva visita al local"; textSize = 13f
                isChecked = b["nuevaVisita"] == true; setOnCheckedChangeListener { _, v -> vm.setNuevaVisita(v) }
            }
            card.addView(cb)
        }
        (b["avisos"] as? List<String>)?.forEach { a ->
            card.addView(Ui007.text(ctx, a, 13f, 0xFFB45309.toInt()).apply {
                background = Ui007.round(0xFFFFF3E8.toInt(), 12, ctx); setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 10) }
            })
        }
        (b["acciones"] as? List<Map<String, Any?>>)?.takeIf { it.isNotEmpty() }?.let { acc ->
            val fila = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 10), 0, 0) }
            acc.forEach { a ->
                fila.addView(Ui007.chip(ctx, a["label"].toString(), a["principal"] == true) { vm.accion(b, a) }.apply {
                    (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(ctx, 8)
                })
            }
            card.addView(HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false; addView(fila) })
        }
        return card
    }

    // ---------------- Eventos ----------------

    private fun evento(e: AsistenteEvento) {
        when (e) {
            is AsistenteEvento.NuevaImplementacion -> {
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
            is AsistenteEvento.PrefijarTexto -> { input.setText(e.texto); input.setSelection(input.text.length); input.requestFocus() }
            is AsistenteEvento.GuardarCsv -> guardarCsv(e)
            is AsistenteEvento.AbrirMinuta -> verMinuta(e.id)
        }
    }

    private fun guardarCsv(e: AsistenteEvento.GuardarCsv) {
        val ctx = requireContext()
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, e.nombre); put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Downloads.IS_PENDING, 0)
            }
            val uri: Uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("sin uri")
            ctx.contentResolver.openOutputStream(uri)!!.use { it.write(e.bytes) }
            Toast.makeText(ctx, "Reporte guardado en Descargas (se abre en Excel).", Toast.LENGTH_LONG).show()
            startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "text/csv").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Abrir reporte"))
        }.onFailure { Toast.makeText(ctx, "No pude guardar el reporte.", Toast.LENGTH_LONG).show() }
    }

    /** «Ver minuta»: con evaluación abre la propuesta de siempre; las minutas redactadas desde el asistente se muestran en una hoja. */
    private fun verMinuta(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val m = runCatching { vm.cargarMinuta(id) }.getOrNull()
            if (m == null) { Toast.makeText(requireContext(), "No pude abrir la minuta.", Toast.LENGTH_SHORT).show(); return@launch }
            val ev = (m["evaluationId"] as? Number)?.toLong()
            if (ev != null) {
                findNavController().navigate(com.upc.asistenteredidbi.R.id.minutaProposalFragment,
                    Bundle().apply { putString("evaluationId", ev.toString()); putLong("minutaId", id) })
            } else hojaMinuta(m)
        }
    }

    private fun hojaMinuta(m: Map<String, Any?>) {
        val ctx = requireContext()
        val sheet = BottomSheetDialog(ctx)
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 18), dp(ctx, 18), dp(ctx, 18), dp(ctx, 20)) }
        col.addView(Ui007.text(ctx, m["clientName"]?.toString() ?: "Minuta", 18f, Ui007.INK, true))
        col.addView(Ui007.text(ctx, "Minuta v${(m["version"] as? Number)?.toInt() ?: 1} · ${m["status"] ?: ""}", 13f, Ui007.GREY).apply { setPadding(0, dp(ctx, 2), 0, dp(ctx, 8)) })
        val c = runCatching { JSONObject(m["contentJson"]?.toString() ?: "{}") }.getOrDefault(JSONObject())
        fun fila(k: String, v: String?) { if (!v.isNullOrBlank()) col.addView(Ui007.text(ctx, "$k: $v", 14f, Ui007.INK).apply { setPadding(0, dp(ctx, 6), 0, 0) }) }
        c.optJSONObject("datos")?.let { d ->
            fila("Razón social", d.optString("razon")); fila("RUC", d.optString("ruc")); fila("Dirección", d.optString("direccion"))
            fila("Encargado", d.optString("encargado")); fila("Celular", d.optString("celular")); fila("Rubro", d.optString("rubro")); fila("Internet", d.optString("internet"))
        }
        c.optJSONArray("conexiones")?.let { a ->
            if (a.length() > 0) {
                col.addView(Ui007.text(ctx, "Conexiones de red", 14f, Ui007.INK, true).apply { setPadding(0, dp(ctx, 12), 0, 0) })
                for (i in 0 until a.length()) a.getJSONObject(i).let { col.addView(Ui007.text(ctx, "• ${it.optString("desde")} → ${it.optString("hacia")} · ${it.optString("medio")}", 13f, Ui007.INK)) }
            }
        }
        fila("Observaciones", c.optString("obsTec"))
        c.optJSONArray("recs")?.let { a -> for (i in 0 until a.length()) fila("Recomendación", a.optJSONObject(i)?.optString("t") ?: a.optString(i)) }
        c.optJSONArray("pasos")?.let { a -> for (i in 0 until a.length()) a.optJSONObject(i)?.let { p -> fila("Paso (${p.optString("p")})", p.optString("t")) } }
        c.optJSONArray("evidenciasFaltantes")?.let { a ->
            if (a.length() > 0) col.addView(Ui007.text(ctx, "Evidencias por subir: " + (0 until a.length()).joinToString(", ") { a.optString(it) }, 13f, 0xFFB45309.toInt()).apply { setPadding(0, dp(ctx, 12), 0, 0) })
        }
        col.addView(Ui007.button(ctx, "Cerrar", false) { sheet.dismiss() })
        sheet.setContentView(ScrollView(ctx).apply { addView(col) })
        sheet.show()
    }

    companion object {
        const val KEY_NUEVA_IMPLEMENTACION = "asistente_nueva_implementacion"
    }
}
