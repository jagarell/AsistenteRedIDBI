package com.upc.asistenteredidbi.presentation.chat

import android.Manifest
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentTechnicalChatBinding
import com.upc.asistenteredidbi.domain.model.ChatEvidenceResult
import com.upc.asistenteredidbi.domain.model.ChatNodePrompt
import com.upc.asistenteredidbi.domain.model.ChatOption
import com.upc.asistenteredidbi.domain.model.ChatTopology
import com.upc.asistenteredidbi.domain.model.TechnicalChatInputType
import com.upc.asistenteredidbi.domain.model.TechnicalEquipmentRecommendation
import com.upc.asistenteredidbi.presentation.common.toHierarchicalText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class TechnicalChatFragment : Fragment() {

    private var _binding: FragmentTechnicalChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TechnicalChatViewModel by viewModels()

    private lateinit var messagesAdapter: ChatMessagesAdapter

    /** Selección acumulada para la pregunta MULTI_SELECT actual: valor -> etiqueta. Se
     * envía con el botón de enviar de siempre. */
    private val multiSelectAnswers = linkedMapOf<String, String>()

    /** Último nodo para el que se renderizaron controles — permite limpiar (o
     * prellenar) la caja de texto exactamente cuando cambia de pregunta, sin
     * depender de que el usuario haya usado el botón de enviar. */
    private var lastRenderedKey: String? = null

    @Inject
    lateinit var moshi: Moshi

    // --- Captura de foto para las evidencias — mismo patrón de EvidenceFragment.kt
    // (cámara/galería/compresión reusados). ---

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }

    private var photoUri: Uri? = null
    private var currentPhotoFile: File? = null

    /** true cuando la cámara se abrió desde «Adjuntar» (documento del cliente) y no desde una evidencia. */
    private var cameraForAttach = false
    private var attachTipo: String? = null

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && photoUri != null) {
                if (cameraForAttach) viewModel.leerAdjunto(listOf(photoUri!!), attachTipo)
                else {
                    val target = pendingTarget
                    pendingTarget = null
                    if (target != null) viewModel.sendPendingPhoto(target, listOf(photoUri!!), "tomar")
                    else viewModel.sendPhotos(listOf(photoUri!!), "tomar")
                }
            }
        }

    private val pickAttachment =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) viewModel.leerAdjunto(listOf(uri), attachTipo)
        }

    /** Pregunta que se escribió a mano en una pregunta con opciones: si no coincide se conserva en la caja. */
    private var restoreTyped: String? = null
    private var restoreNodeId: String? = null
    private var attachDialog: com.google.android.material.bottomsheet.BottomSheetDialog? = null

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            val target = pendingTarget
            pendingTarget = null
            if (uris.isNotEmpty()) {
                if (target != null) viewModel.sendPendingPhoto(target, uris, "subir") else viewModel.sendPhotos(uris, "subir")
            }
        }

    /** Foto pendiente de «Subir después» que se está tomando o eligiendo (null = la evidencia actual del chat). */
    private var pendingTarget: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTechnicalChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupInitialState()
        setupClicks()
        setupKeyboardInsets()
        observeViewModel()
        // Al volver del editor del mapa se refresca la tarjeta con lo guardado.
        parentFragmentManager.setFragmentResultListener("map_saved", viewLifecycleOwner) { _, _ ->
            viewModel.refreshMapAfterEdit()
        }
    }

    private fun setupRecycler() {
        messagesAdapter = ChatMessagesAdapter(
            onEditClick = { message -> viewModel.editAnswer(message.id) },
            onCardAction = { action ->
                when {
                    action.startsWith("RD_DEL:") -> viewModel.deleteReading(action.removePrefix("RD_DEL:"))
                    action == "RD_OK" -> viewModel.confirmReading()
                    action == "RD_WRONG" -> viewModel.wrongReading()
                    action == "RD_RESTORE" -> viewModel.restoreReadings()
                    action == "RD_EDIT" -> showReadingEditor()
                    else -> handleCardAction(action)
                }
            },
            onPhotoClick = { message -> showEvidenceOptions(message) },
            onMapClick = { map ->
                val evidences = viewModel.uiState.value.evidences
                com.upc.asistenteredidbi.presentation.map.MapZoomDialog.show(requireContext(), map) { image ->
                    evidences.firstOrNull { it.code == image.evidenceCode && it.scope == image.scope }
                        ?.paths?.firstOrNull()
                        ?.let { com.upc.asistenteredidbi.presentation.map.MapCanvasView.decodeThumb(it) }
                }
            }
        )
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = messagesAdapter
        }
    }

    private fun handleCardAction(action: String) {
        run {
            run {
                when (action) {
                    "VIEW_ALL" -> showEvidenceGallery(unresolvedOnly = false)
                    "RESOLVE" -> showEvidenceGallery(unresolvedOnly = true)
                    "GENERATE_MAP", "REGENERATE_MAP" -> viewModel.generateMap()
                    "EDIT_MAP" -> findNavController().navigate(
                        R.id.action_chat_to_topologyEditor,
                        bundleOf("evaluationId" to viewModel.evaluationId.toString())
                    )
                    "CONTINUE_DIAGNOSIS" -> viewModel.sendAnswer("GENERAR_MINUTA", "Continuar al diagnóstico")
                    "VIEW_PROPOSAL", "VIEW_ANALYSIS" -> navigateToDiagnosis()
                }
            }
        }
    }

    /** El teclado ya empuja toda la pantalla (MainActivity); aquí solo se muestra el último mensaje al abrirlo. */
    private fun setupKeyboardInsets() {
        binding.rvMessages.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top < oldBottom - oldTop) {
                val last = (binding.rvMessages.adapter?.itemCount ?: 0) - 1
                if (last >= 0) binding.rvMessages.post { binding.rvMessages.scrollToPosition(last) }
            }
        }
    }

    private fun setupInitialState() {
        binding.btnGoEvidence.isVisible = false
        binding.etTextInput.isEnabled = false
        binding.btnSubmit.isEnabled = false

        binding.progressEvaluation.progress = 0
        binding.tvProgressPercent.text = "0%"
        binding.tvQuestionCounter.text = "Preparando evaluación"
    }

    /** Dictar la respuesta por voz (QA D3/G16): se transcribe en el campo de texto y se revisa antes de enviar. */
    private val dictar = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == android.app.Activity.RESULT_OK && _binding != null) {
            val t = r.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (t.isNotBlank()) {
                val et = binding.etTextInput
                et.setText(if (et.text.isBlank()) t else et.text.toString().trimEnd() + " " + t)
                et.setSelection(et.text.length)
            }
        }
    }

    private fun dictarRespuesta() {
        val i = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Di tu respuesta…")
        }
        runCatching { dictar.launch(i) }.onFailure { Toast.makeText(requireContext(), "Este teléfono no tiene dictado por voz. Escribe la respuesta.", Toast.LENGTH_LONG).show() }
    }

    private fun setupClicks() {
        binding.btnDictate.setOnClickListener { dictarRespuesta() }
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnSubmit.setOnClickListener { submitAnswer() }
        binding.btnAttach.setOnClickListener { showAttachSheet(viewModel.uiState.value.node?.attachHint) }

        binding.etTextInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitAnswer()
                true
            } else {
                false
            }
        }

        binding.btnGoEvidence.setOnClickListener { navigateToDiagnosis() }
        binding.btnMore.setOnClickListener { showProposal() }
        binding.btnHelp.setOnClickListener {
            ChatHelpSheet.newInstance(viewModel.uiState.value.node?.text.orEmpty())
                .show(childFragmentManager, ChatHelpSheet.TAG)
        }
    }

    private fun submitAnswer() {
        val state = viewModel.uiState.value
        if (state.completed && state.editSnapshot == null) {
            val text = binding.etTextInput.text?.toString().orEmpty().trim()
            if (text.isNotBlank()) {
                viewModel.sendFreeText(text)
                binding.etTextInput.text?.clear()
            }
            return
        }
        val node = state.node ?: return
        if (node.inputType == TechnicalChatInputType.MULTI_SELECT) {
            if (multiSelectAnswers.isEmpty() && node.required && node.minSelected > 0) {
                Toast.makeText(requireContext(), "Selecciona al menos una opción", Toast.LENGTH_SHORT).show()
                return
            }
            val values = multiSelectAnswers.values.toList()
            viewModel.sendAnswer(
                value = com.upc.asistenteredidbi.data.mapper.AnyJson.toJson(values),
                display = if (values.isEmpty()) "Ninguno" else values.joinToString(", ")
            )
            multiSelectAnswers.clear()
            return
        }

        val answer = binding.etTextInput.text?.toString().orEmpty().trim()
        if (answer.isBlank() && node.required) {
            Toast.makeText(requireContext(), "Escribe una respuesta", Toast.LENGTH_SHORT).show()
            return
        }
        // «no sé» en razón social, nombre del local o dirección: se ofrece adjuntar el documento.
        if (node.attachHint != null && node.nodeId in setOf("P01", "P01c", "P03") &&
            Regex("^no\\s*s[eé]\\b", RegexOption.IGNORE_CASE).containsMatchIn(answer)
        ) {
            showAttachSheet(node.attachHint)
            return
        }
        if (node.inputType == TechnicalChatInputType.CHOICE) {
            // Una duda con «?» va a Ayuda y no se envía al chat ni a la minuta.
            if (answer.endsWith("?")) {
                showHelpDialog(answer)
                return
            }
            // Si no coincide con ninguna opción, lo escrito se queda en la caja.
            restoreTyped = answer
            restoreNodeId = node.nodeId
            viewModel.sendAnswer(answer)
            return
        }
        viewModel.sendAnswer(answer)
        binding.etTextInput.text?.clear()
    }

    private fun showProposal() {
        val proposal = viewModel.uiState.value.proposal

        val text = if (proposal == null) {
            "Completa la evaluación para generar la propuesta."
        } else {
            buildString {
                appendLine(proposal.summary)
                proposal.score?.let { score ->
                    appendLine()
                    appendLine("Puntaje de infraestructura: $score/100")
                }
                if (proposal.asIsFindings.isNotEmpty()) {
                    appendLine()
                    appendLine("Diagnóstico actual (AS-IS):")
                    proposal.asIsFindings.forEach { finding -> appendLine("• $finding") }
                }
                appendLine()
                appendLine("Propuesta recomendada (TO-BE):")
                proposal.recommendations.forEach { recommendation -> appendLine("• $recommendation") }
                appendLine()
                appendLine("Topología:")
                // Preferimos la topología estructurada (nodos/enlaces con tipo
                // de conexión) construida a partir del chat; si no llegó,
                // usamos el resumen en texto plano como respaldo.
                append(proposal.topology?.toHierarchicalText() ?: proposal.topologyText)
            }
        }

        Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
    }

    /** Fechas (visita, inicio): calendario; la respuesta viaja como AAAA-MM-DD y se ve como dd/MM/aaaa. */
    private fun showDatePicker(initialIso: String? = null) {
        val cal = java.util.Calendar.getInstance()
        initialIso?.takeIf { Regex("\\d{4}-\\d{2}-\\d{2}").matches(it) }?.let {
            cal.set(it.substring(0, 4).toInt(), it.substring(5, 7).toInt() - 1, it.substring(8, 10).toInt())
        }
        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val iso = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                val shown = String.format(java.util.Locale.US, "%02d/%02d/%04d", day, month + 1, year)
                viewModel.sendAnswer(iso, shown)
            },
            cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showEvidenceGallery(unresolvedOnly: Boolean) {
        EvidenceGalleryDialog.newInstance(unresolvedOnly).show(childFragmentManager, "evidence_gallery")
    }

    // ---- diálogos de lectura, enlaces, pasos y WiFi -----------------------------------
    /** "Editar datos" / "Corregir": un campo por cada lectura, con sus opciones cuando es una lista. */
    private fun showReadingEditor() {
        val state = viewModel.uiState.value
        val node = state.node ?: return
        val fields = if (node.inputType == TechnicalChatInputType.READ_CARD) node.fields else node.evidence?.fields.orEmpty()
        if (fields.isEmpty()) return
        val draft = state.reviewDraft
        val shown = fields.filter { it.key !in state.reviewDeleted }
        val views = linkedMapOf<String, View>()
        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
        }
        shown.forEach { f ->
            form.addView(TextView(requireContext()).apply {
                text = f.label
                textSize = 12f
                setTextColor(com.upc.asistenteredidbi.presentation.common.NightColors.parse("#6B7280"))
                setPadding(0, dp(10), 0, 0)
            })
            if (f.type == "sel") {
                val spinner = android.widget.Spinner(requireContext())
                spinner.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, f.options.map { it.label })
                val current = draft[f.key]?.toString().orEmpty()
                spinner.setSelection(f.options.indexOfFirst { it.value == current }.coerceAtLeast(0))
                views[f.key] = spinner
                form.addView(spinner)
            } else {
                val input = EditText(requireContext()).apply {
                    setText(EvidencePresentation.fieldValue(f, draft[f.key]))
                    inputType = if (f.type == "number") InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    else InputType.TYPE_CLASS_TEXT
                }
                views[f.key] = input
                form.addView(input)
            }
        }
        AlertDialog.Builder(requireContext())
            .setTitle(if (node.inputType == TechnicalChatInputType.READ_CARD) "Corregir el router" else "Editar lo que leí")
            .setView(android.widget.ScrollView(requireContext()).apply { addView(form) })
            .setPositiveButton("Guardar") { _, _ ->
                val changes = linkedMapOf<String, Any?>()
                shown.forEach { f ->
                    val v = views[f.key]
                    changes[f.key] = when {
                        f.type == "sel" -> f.options.getOrNull((v as android.widget.Spinner).selectedItemPosition)?.value ?: ""
                        f.type == "number" -> (v as EditText).text.toString().trim().replace(',', '.').toDoubleOrNull()
                            ?.let { if (it == it.toLong().toDouble()) it.toLong() else it }
                        else -> (v as EditText).text.toString().trim()
                    }
                }
                viewModel.updateReading(changes)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Anexo B: varios enlaces de video, cada uno con lo que muestra. */
    private fun showLinksDialog(node: ChatNodePrompt) {
        val types = node.videoTypes.ifEmpty { listOf("Recorrido del local", "Otro") }
        val rows = mutableListOf<Pair<android.widget.Spinner, EditText>>()
        val list = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        fun addRow() {
            val spinner = android.widget.Spinner(requireContext())
            spinner.adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
            val url = EditText(requireContext()).apply {
                hint = "Pega el enlace (Drive, OneDrive, YouTube…)"
                inputType = InputType.TYPE_TEXT_VARIATION_URI
            }
            val box = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(10), 0, 0) }
            box.addView(spinner)
            box.addView(url)
            box.addView(TextView(requireContext()).apply {
                text = "Quitar"
                setTextColor(com.upc.asistenteredidbi.presentation.common.NightColors.parse("#D32F2F"))
                textSize = 13f
                setPadding(0, dp(4), 0, 0)
                setOnClickListener { list.removeView(box); rows.removeAll { it.second === url } }
            })
            list.addView(box)
            rows += spinner to url
        }
        addRow()
        val root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        root.addView(android.widget.ScrollView(requireContext()).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(requireContext()).apply {
            text = "+ Agregar otro enlace"
            setTextColor(com.upc.asistenteredidbi.presentation.common.NightColors.parse("#1565C0"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(dp(24), dp(10), dp(24), dp(10))
            setOnClickListener { addRow() }
        })
        AlertDialog.Builder(requireContext())
            .setTitle("Videos del local y los equipos")
            .setView(root)
            .setPositiveButton("Listo") { _, _ ->
                val entries = rows.mapNotNull { (sp, url) ->
                    url.text.toString().trim().takeIf { it.isNotEmpty() }?.let { mapOf("d" to types[sp.selectedItemPosition], "u" to it) }
                }
                val display = if (entries.isEmpty()) "Continuar sin enlaces" else "${entries.size} enlace${if (entries.size > 1) "s" else ""}"
                viewModel.sendAnswer(com.upc.asistenteredidbi.data.mapper.AnyJson.toJson(entries), display)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Pasos a seguir: quitar, editar, agregar, deshacer y rehacer. Se lee cada paso completo al tocarlo. */
    private fun showStepsDialog(node: ChatNodePrompt) {
        @Suppress("UNCHECKED_CAST")
        val initial = (com.upc.asistenteredidbi.data.mapper.AnyJson.fromJson(node.valueJson) as? List<Map<String, Any?>>).orEmpty()
            .map { (it["p"]?.toString() ?: "ID") to (it["t"]?.toString() ?: "") }
        var steps = initial.toMutableList()
        val undo = ArrayDeque<List<Pair<String, String>>>()
        val redo = ArrayDeque<List<Pair<String, String>>>()
        val box = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        lateinit var render: () -> Unit
        fun push() { undo.addLast(steps.toList()); redo.clear() }
        render = {
            box.removeAllViews()
            listOf("Cliente" to "Por parte del cliente", "ID" to "Por parte de ID").forEach { (who, title) ->
                box.addView(TextView(requireContext()).apply {
                    text = title; textSize = 14f; typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding(0, dp(12), 0, dp(2))
                })
                steps.forEachIndexed { i, (p, t) ->
                    if ((p == "Cliente") != (who == "Cliente")) return@forEachIndexed
                    val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
                    row.addView(EditText(requireContext()).apply {
                        setText(t)
                        textSize = 13f
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        setOnFocusChangeListener { _, focus ->
                            if (!focus && text.toString() != steps[i].second) { push(); steps[i] = steps[i].first to text.toString() }
                        }
                    }, LinearLayout.LayoutParams(0, -2, 1f))
                    row.addView(TextView(requireContext()).apply {
                        text = "✕"; textSize = 16f; setTextColor(com.upc.asistenteredidbi.presentation.common.NightColors.parse("#D32F2F")); setPadding(dp(12), 0, dp(4), 0)
                        setOnClickListener { push(); steps.removeAt(i); render() }
                    })
                    box.addView(row)
                }
            }
            val addFor = android.widget.Spinner(requireContext()).apply {
                adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("Cliente", "ID"))
            }
            val addText = EditText(requireContext()).apply { hint = "Agregar un paso" }
            box.addView(TextView(requireContext()).apply { text = "Agregar"; textSize = 13f; setPadding(0, dp(14), 0, 0) })
            box.addView(addFor)
            box.addView(addText)
            box.addView(TextView(requireContext()).apply {
                text = "+ Agregar paso"; setTextColor(com.upc.asistenteredidbi.presentation.common.NightColors.parse("#1565C0")); typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, dp(8), 0, dp(4))
                setOnClickListener {
                    val t = addText.text.toString().trim()
                    if (t.isNotEmpty()) { push(); steps.add(addFor.selectedItem.toString() to t); render() }
                }
            })
        }
        render()
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Pasos a seguir")
            .setView(android.widget.ScrollView(requireContext()).apply { addView(box) })
            .setPositiveButton("Confirmar pasos") { _, _ ->
                val clean = steps.filter { it.second.isNotBlank() }.map { mapOf("p" to it.first, "t" to it.second) }
                viewModel.sendAnswer(com.upc.asistenteredidbi.data.mapper.AnyJson.toJson(clean), "Pasos confirmados (${clean.size})")
            }
            .setNeutralButton("Deshacer", null)
            .setNegativeButton("Rehacer", null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            if (undo.isNotEmpty()) { redo.addLast(steps.toList()); steps = undo.removeLast().toMutableList(); render() }
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            if (redo.isNotEmpty()) { undo.addLast(steps.toList()); steps = redo.removeLast().toMutableList(); render() }
        }
    }

    /** Revisar WiFi por zona: señal (dBm) y latencia en cada zona; Regular o Mala = sin buena señal. */
    private fun showWifiDialog(node: ChatNodePrompt) {
        val values = linkedMapOf<String, Pair<Int, Int>>()
        val box = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        fun rate(dbm: Int, ping: Int): String {
            val rk = listOf("Excelente", "Buena", "Regular", "Mala")
            val rs = if (dbm >= -60) 0 else if (dbm >= -67) 1 else if (dbm >= -75) 2 else 3
            val rp = if (ping < 50) 0 else if (ping <= 100) 1 else if (ping <= 200) 2 else 3
            return rk[maxOf(rs, rp)]
        }
        lateinit var render: () -> Unit
        render = {
            box.removeAllViews()
            box.addView(TextView(requireContext()).apply {
                text = "Toca «Medir aquí» en cada zona. Puedes cambiar los valores. Señal: ≥ −60 excelente · −60 a −67 buena · −67 a −75 regular · menos mala. Latencia: <50 · 50–100 · 100–200 · >200."
                textSize = 12f; setTextColor(com.upc.asistenteredidbi.presentation.common.NightColors.parse("#6B7280"))
            })
            node.zones.forEach { z ->
                val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(10), 0, 0) }
                row.addView(TextView(requireContext()).apply { text = z; typeface = android.graphics.Typeface.DEFAULT_BOLD })
                val m = values[z]
                if (m == null) {
                    row.addView(TextView(requireContext()).apply {
                        text = "Medir aquí"; setTextColor(com.upc.asistenteredidbi.presentation.common.NightColors.parse("#1565C0")); typeface = android.graphics.Typeface.DEFAULT_BOLD
                        setPadding(0, dp(6), 0, dp(6))
                        setOnClickListener { values[z] = measureWifi(); render() }
                    })
                } else {
                    val dbm = EditText(requireContext()).apply { hint = "Señal (dBm)"; setText(m.first.toString()); inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED }
                    val ping = EditText(requireContext()).apply { hint = "Latencia (ms)"; setText(m.second.toString()); inputType = InputType.TYPE_CLASS_NUMBER }
                    val result = TextView(requireContext()).apply { text = "→ ${rate(m.first, m.second)}"; setTextColor(com.upc.asistenteredidbi.presentation.common.NightColors.parse("#1565C0")) }
                    val watcher = object : android.text.TextWatcher {
                        override fun afterTextChanged(e: android.text.Editable?) {
                            val a = dbm.text.toString().toIntOrNull() ?: return
                            val b = ping.text.toString().toIntOrNull() ?: return
                            values[z] = a to b
                            result.text = "→ ${rate(a, b)}"
                        }
                        override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                        override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                    }
                    dbm.addTextChangedListener(watcher); ping.addTextChangedListener(watcher)
                    row.addView(dbm); row.addView(ping); row.addView(result)
                }
                box.addView(row)
            }
        }
        render()
        AlertDialog.Builder(requireContext())
            .setTitle("Revisar WiFi por zona")
            .setView(android.widget.ScrollView(requireContext()).apply { addView(box) })
            .setPositiveButton("Guardar mediciones") { _, _ ->
                val out = values.mapValues { mapOf("dbm" to it.value.first, "ping" to it.value.second) }
                val rows = values.entries.joinToString("\n") { "${it.key}: ${it.value.first} dBm, ${it.value.second} ms → ${rate(it.value.first, it.value.second)}" }
                viewModel.sendAnswer(com.upc.asistenteredidbi.data.mapper.AnyJson.toJson(out), rows.ifBlank { "Sin mediciones" })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Señal real del WiFi conectado y latencia al router (la puerta de enlace). Si no se puede, valores editables. */
    @Suppress("DEPRECATION")
    private fun measureWifi(): Pair<Int, Int> {
        return try {
            val wm = requireContext().applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val rssi = wm.connectionInfo?.rssi?.takeIf { it in -100..-1 } ?: -58
            val gateway = wm.dhcpInfo?.gateway ?: 0
            var ping = 30
            if (gateway != 0) {
                val ip = java.net.InetAddress.getByAddress(byteArrayOf(
                    (gateway and 0xff).toByte(), (gateway shr 8 and 0xff).toByte(),
                    (gateway shr 16 and 0xff).toByte(), (gateway shr 24 and 0xff).toByte()))
                val t0 = System.nanoTime()
                if (ip.isReachable(800)) ping = ((System.nanoTime() - t0) / 1_000_000).toInt().coerceAtLeast(1)
            }
            rssi to ping
        } catch (_: Exception) {
            -58 to 30
        }
    }

    /** Controles según el tipo de nodo actual (texto, chips, evidencia, resumen). */
    private fun renderQuickReplies(state: TechnicalChatUiState) {
        binding.containerQuickReplies.removeAllViews()
        binding.actionBar.removeAllViews()

        val node = state.node
        val editing = state.editSnapshot != null
        val canAnswer = !state.isLoading && !state.isSending && (!state.completed || editing)
        val type = node?.inputType

        binding.etTextInput.inputType = when {
            node?.multiline == true -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            node?.keyboard == "number" -> InputType.TYPE_CLASS_NUMBER
            node?.keyboard == "phone" -> InputType.TYPE_CLASS_PHONE
            node?.keyboard == "date" -> InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE
            else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        // Texto tipeado para una pregunta abandonada no debe colarse en la siguiente:
        // se limpia (o se prellena con el valor por defecto) apenas cambia el nodo.
        val key = node?.let { "${it.nodeId}|${it.scope}|${state.messages.size}" }
        if (key != lastRenderedKey) {
            val keep = restoreTyped?.takeIf { node?.nodeId == restoreNodeId }
            if (keep == null) { restoreTyped = null; restoreNodeId = null }
            binding.etTextInput.setText(keep ?: if (node?.keyboard == "date") "" else node?.defaultValue.orEmpty())
            binding.etTextInput.setSelection(binding.etTextInput.text?.length ?: 0)
            multiSelectAnswers.clear()
            node?.takeIf { it.inputType == TechnicalChatInputType.MULTI_SELECT }?.selected?.forEach { multiSelectAnswers[it] = it }
            // El scroll horizontal de chips conserva su posición entre preguntas:
            // sin esto, las opciones de la siguiente pregunta quedan fuera de pantalla.
            binding.scrollQuickReplies.scrollTo(0, 0)
            lastRenderedKey = key
        }

        val freeChat = state.completed && !editing
        val atSummary = type == TechnicalChatInputType.SUMMARY
        val isText = (type == TechnicalChatInputType.TEXT || type == TechnicalChatInputType.NUMBER) && node?.keyboard != "date"
        val isChoice = type == TechnicalChatInputType.CHOICE
        val needsTextInput = freeChat || atSummary || isText || isChoice
        if (atSummary) {
            // Como en el prototipo: el campo está, pero hasta el diagnóstico no recibe texto.
            binding.etTextInput.isEnabled = false
            binding.btnSubmit.isEnabled = false
            binding.btnSubmit.alpha = 0.45f
            binding.etTextInput.hint = "Pregúntale algo al asistente…"
            binding.btnAttach.isEnabled = false
            binding.btnAttach.alpha = 0.45f
        }
        binding.etTextInput.isVisible = needsTextInput
        // Con la evaluación terminada ya no se adjunta: el chat solo atiende cambios al mapa.
        binding.btnAttach.isVisible = !freeChat
        binding.btnDictate.isVisible = true
        binding.containerTextRow.isVisible = needsTextInput

        if (freeChat) {
            // Evaluación terminada: el asistente atiende pedidos de cambio al mapa.
            if (!state.isSending && state.currentMap != null) {
                listOf("PC de caja por cable", "Agrega red de invitados", "Agrega tablets").forEach { suggestion ->
                    addQuickReplyChip(suggestion, compact = true) { viewModel.sendFreeText(suggestion) }
                }
            }
            binding.scrollQuickReplies.isVisible = binding.containerQuickReplies.childCount > 0
            binding.actionBar.isVisible = false
            capQuickRepliesHeight()
            return
        }

        if (!canAnswer) {
            binding.scrollQuickReplies.isVisible = false
            binding.actionBar.isVisible = false
            return
        }

        if (editing && state.completed) {
            // Modo edición: ya se respondió lo que se cambió.
            addQuickReplyChip("Continuar con diagnóstico", filled = true) { viewModel.finishEditing(); navigateToDiagnosis() }
            addQuickReplyChip("Descartar cambios") { viewModel.discardEdits() }
            binding.scrollQuickReplies.isVisible = true
            binding.actionBar.isVisible = false
            capQuickRepliesHeight()
            return
        }

        when (type) {
            TechnicalChatInputType.YES_NO -> {
                addQuickReplyChip("Sí") { viewModel.sendAnswer("SI", "Sí") }
                addQuickReplyChip("No") { viewModel.sendAnswer("NO", "No") }
            }

            TechnicalChatInputType.CHOICE -> {
                // Editando: la respuesta anterior se puede mantener con un toque y su opción sale marcada (doc 0.0.15).
                val actual = node?.takeIf { it.editing }?.currentValue
                if (actual != null) {
                    val etiqueta = node?.currentLabel?.takeIf { it.isNotBlank() } ?: actual.removePrefix("OTRO:")
                    addQuickReplyChip("✓ Mantener: $etiqueta", filled = true) { viewModel.sendAnswer(actual, etiqueta) }
                }
                node?.options?.forEach { option ->
                    val marca = if (actual != null && option.value == actual) "✓ " else ""
                    addQuickReplyChip(marca + option.label.replace(" (lo más común)", "").let { if (option.label.contains("(lo más común)")) option.label else it }) {
                        if (option.other) askOther(node, option) else viewModel.sendAnswer(option.value, option.label.replace(" (lo más común)", "").replace(" (se ve en la foto)", ""))
                    }
                }
                addAttachShortcut(node)
            }

            TechnicalChatInputType.MULTI_SELECT -> {
                node?.options?.let { addMultiSelectChips(it) }
                if (node?.allowOther == true) addQuickReplyChip("Otro (escribir)") { askCustomOption(node) }
                node?.noneLabel?.let { none -> addQuickReplyChip(none) { viewModel.sendAnswer("@NONE", none) } }
                addPrimaryAction("Continuar") { submitAnswer() }
            }

            TechnicalChatInputType.TEXT, TechnicalChatInputType.NUMBER -> {
                if (node?.keyboard == "date") {
                    node.defaultValue?.takeIf { it.length == 10 && node.required }?.let { iso ->
                        val shown = "${iso.substring(8, 10)}/${iso.substring(5, 7)}/${iso.substring(0, 4)}"
                        addQuickReplyChip((if (node.editing) "✓ Mantener · " else "Hoy · ") + shown, filled = true) { viewModel.sendAnswer(iso, shown) }
                    }
                    addQuickReplyChip("📅 Elegir fecha") { showDatePicker(node.defaultValue) }
                    if (node.required == false) addQuickReplyChip("Sin fecha") { viewModel.sendAnswer("", "Sin fecha") }
                } else {
                    node?.suggestions?.forEach { s -> addQuickReplyChip(s) { viewModel.sendAnswer(s, s) } }
                    if (node?.required == false) addQuickReplyChip("Omitir") { viewModel.sendAnswer("", "Omitir") }
                    addAttachShortcut(node)
                }
            }

            TechnicalChatInputType.EVIDENCE -> addEvidenceActions(node)

            TechnicalChatInputType.READ_CARD -> Unit // la tarjeta del chat lleva Correcto / Corregir

            TechnicalChatInputType.LINKS -> {
                node?.let { n -> addPrimaryAction("Agregar enlaces de video") { showLinksDialog(n) } }
                addQuickReplyChip("Continuar sin enlaces") { viewModel.sendAnswer("[]", "Continuar sin enlaces") }
            }

            TechnicalChatInputType.STEPS -> node?.let { n ->
                addPrimaryAction("Revisar pasos a seguir") { showStepsDialog(n) }
            }

            TechnicalChatInputType.WIFI_MEASURE -> node?.let { n ->
                addPrimaryAction("Medir WiFi por zona") { showWifiDialog(n) }
            }

            TechnicalChatInputType.ALERT -> addPrimaryAction("Entendido") { viewModel.sendAnswer("", "Entendido") }

            // El resumen se cierra con "Continuar al diagnóstico →" dentro de su tarjeta.
            TechnicalChatInputType.SUMMARY -> Unit

            else -> Unit
        }

        binding.scrollQuickReplies.isVisible = binding.containerQuickReplies.childCount > 0
        binding.actionBar.isVisible = binding.actionBar.childCount > 0
        capQuickRepliesHeight()
    }

    /** "Otro (escribir)" en una pregunta de una sola opción: pide el texto (o el número). */
    private fun askOther(node: ChatNodePrompt, option: ChatOption) {
        val input = EditText(requireContext()).apply {
            hint = if (option.numeric) "Escribe el número${node.unit?.let { " ($it)" }.orEmpty()}" else "Escribe cuál es"
            inputType = if (option.numeric) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(dp(24), dp(16), dp(24), dp(16))
        }
        AlertDialog.Builder(requireContext())
            .setTitle(if (option.numeric) "¿Cuántos?" else "Otro")
            .setView(input)
            .setPositiveButton("Enviar") { _, _ ->
                val text = input.text?.toString().orEmpty().trim()
                if (text.isNotBlank()) viewModel.sendAnswer("OTRO:$text", if (option.numeric) "$text ${node.unit.orEmpty()}".trim() else text)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Muchas opciones (13 áreas, 9 tipos de negocio) se acomodan en varias filas, con un tope de alto y scroll. */
    private fun capQuickRepliesHeight() {
        val scroll = binding.scrollQuickReplies
        scroll.post {
            val content = binding.containerQuickReplies.height
            scroll.layoutParams = scroll.layoutParams.apply {
                height = if (content > dp(180)) dp(180) else ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    // ---- evidencias: Tomar foto / Subir foto / Saltar evidencia ----------------------------
    /** 0.0.15: opciones al tocar la foto de una evidencia en el chat. */
    private fun showEvidenceOptions(message: ChatMessage) {
        val state = viewModel.uiState.value
        val item = state.evidences.firstOrNull { it.messageId == message.id }
        if (item == null) {
            Toast.makeText(requireContext(), "Esta foto todavía no quedó guardada como evidencia.", Toast.LENGTH_SHORT).show()
            return
        }
        val nota = viewModel.evidenceNote(item)
        val opciones = arrayOf("Cambiar foto", if (nota.isBlank()) "Agregar descripción" else "Editar descripción")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(item.title + if (nota.isNotBlank()) "\nDescripción: $nota" else "")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Cambiar foto · ${item.title}")
                        .setMessage("Vuelves a pedir solo esta evidencia para subir otra foto. Tus demás respuestas y la descripción se mantienen.")
                        .setPositiveButton("Cambiar foto") { _, _ -> viewModel.retakeEvidence(item.messageId) }
                        .setNegativeButton("Cancelar", null)
                        .show()
                    else -> EvidenceNoteDialog.show(requireContext(), "Descripción · ${item.title}", nota) { t ->
                        viewModel.saveEvidenceNote(viewModel.evidenceNodeId(item), t)
                    }
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun addEvidenceActions(node: ChatNodePrompt?) {
        val ev = node?.evidence
        if (ev?.status == "REVIEW") return // la tarjeta del chat trae Correcto / Editar / No es correcto
        val status = ev?.status ?: "WAITING"
        val bar = binding.actionBar
        // 0.0.15: descripción opcional antes de subir la foto («¿Qué muestra esta foto?»); viaja con la foto
        val pendiente = viewModel.uiState.value.pendingDescription
        bar.addView(TextView(requireContext()).apply {
            text = if (pendiente.isNullOrBlank()) "✎ Agregar descripción (opcional)" else "✎ Descripción: $pendiente"
            textSize = 13f
            setTextColor(Color.parseColor("#1565C0"))
            setPadding(0, 0, 0, dp(8))
            setOnClickListener {
                EvidenceNoteDialog.show(requireContext(), "Descripción de la foto", pendiente.orEmpty()) { t -> viewModel.setPendingDescription(t) }
            }
        })
        val takeLabel = if (status == "IL" || status == "WRONG") "Volver a tomar foto" else "Tomar foto"
        val pickLabel = if (status == "NC" || status == "WRONG") "Volver a subir foto" else "Subir foto"
        val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        if (status != "NC") row.addView(actionButton(takeLabel, filled = true, iconRes = R.drawable.ic_camera) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }, LinearLayout.LayoutParams(0, dp(46), 1f))
        if (status != "IL") row.addView(actionButton(pickLabel, filled = status == "NC", iconRes = R.drawable.ic_image) {
            pickImages.launch("image/*")
        }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { if (row.childCount > 0) marginStart = dp(10) })
        bar.addView(row)
        // «Guardar como correcta»: la IA observó la foto (no corresponde / no legible); en cualquier estado del local.
        if (ev?.canForce == true) bar.addView(actionButton("Guardar como correcta", filled = true, iconRes = R.drawable.ic_check_circle) {
            showForceDialog(ev?.message, ev?.enObra == true) { nota -> viewModel.forceEvidence(nota) }
        }.apply { backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E7D52")) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(8) })
        // «Subir después»: fotos obligatorias del local; se piden antes de generar la minuta.
        if (ev?.canLater == true) bar.addView(TextView(requireContext()).apply {
            text = "Subir después"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1565C0"))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(4))
            setOnClickListener { viewModel.laterEvidence() }
        })
        // Las fotos obligatorias (frontis, preparación, caja) no se pueden saltar.
        if (node?.skippable != false) bar.addView(TextView(requireContext()).apply {
            text = if (status == "WRONG") "Continuar sin esta lectura" else "Saltar evidencia"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(com.upc.asistenteredidbi.presentation.common.NightColors.parse("#1565C0"))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(4))
            setOnClickListener { viewModel.skipEvidence() }
        })
    }

    private fun addPrimaryAction(label: String, onClick: () -> Unit) {
        binding.actionBar.addView(
            actionButton(label, filled = true, onClick = onClick),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { bottomMargin = dp(8) }
        )
    }

    private fun actionButton(
        label: String,
        filled: Boolean,
        iconRes: Int? = null,
        onClick: () -> Unit
    ): MaterialButton {
        val blue = com.upc.asistenteredidbi.presentation.common.NightColors.parse("#1565C0")
        return MaterialButton(
            requireContext(),
            null,
            if (filled) com.google.android.material.R.attr.materialButtonStyle
            else com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            cornerRadius = dp(24)
            insetTop = 0
            insetBottom = 0
            if (filled) {
                backgroundTintList = ColorStateList.valueOf(blue)
                setTextColor(Color.WHITE)
            } else {
                backgroundTintList = ColorStateList.valueOf(com.upc.asistenteredidbi.presentation.common.NightColors.parse("#FFFFFF"))
                setTextColor(blue)
                strokeColor = ColorStateList.valueOf(blue)
                strokeWidth = dp(2)
            }
            iconRes?.let {
                setIconResource(it)
                iconTint = ColorStateList.valueOf(if (filled) Color.WHITE else blue)
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            }
            setOnClickListener { onClick() }
        }
    }

    // ---- adjuntos del cliente --------------------------------------------------------------
    private val attachTypes = linkedMapOf(
        "ruc" to "Ficha RUC o consulta RUC (SUNAT)",
        "comp" to "Boleta o factura del local",
        "recibo" to "Recibo de internet",
        "tarjeta" to "Tarjeta o datos del encargado",
        "frontis" to "Foto del frontis o letrero"
    )
    private val attachShortcutText = mapOf(
        "ruc" to "📎 Adjuntar ficha RUC o comprobante", "frontis" to "📎 Adjuntar foto del letrero",
        "comp" to "📎 Adjuntar boleta o factura del local", "tarjeta" to "📎 Adjuntar tarjeta del encargado",
        "recibo" to "📎 Adjuntar recibo de internet"
    )

    private fun addAttachShortcut(node: ChatNodePrompt?) {
        val hint = node?.attachHint ?: return
        addQuickReplyChip(attachShortcutText[hint] ?: "📎 Adjuntar documento", compact = true) { showAttachSheet(hint) }
    }

    /** Hoja «Adjuntar al Chat IA»: tipo de documento (el sugerido va marcado) y de dónde sale. */
    private fun showAttachSheet(suggested: String?) {
        if (viewModel.uiState.value.isSending) return
        val ctx = requireContext()
        var chosen: String? = suggested
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(22)) }
        col.addView(TextView(ctx).apply {
            text = "Adjuntar al Chat IA"; textSize = 20f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1F2937"))
        })
        col.addView(TextView(ctx).apply {
            text = "La IA lee el documento y te muestra los datos para que los revises."
            textSize = 13f; setTextColor(Color.parseColor("#6B7280")); setPadding(0, dp(4), 0, dp(10))
        })
        val group = android.widget.RadioGroup(ctx)
        val auto = android.widget.RadioButton(ctx).apply { id = View.generateViewId(); text = "Que la IA lo reconozca" }
        group.addView(auto)
        val ids = mutableMapOf<Int, String>()
        attachTypes.forEach { (k, label) ->
            group.addView(android.widget.RadioButton(ctx).apply {
                id = View.generateViewId(); text = label; ids[id] = k; isChecked = k == suggested
            })
        }
        if (suggested == null) auto.isChecked = true
        group.setOnCheckedChangeListener { _, id -> chosen = ids[id] }
        col.addView(group)
        fun go(camera: Boolean) {
            sheet.dismiss()
            attachTipo = chosen
            cameraForAttach = camera
            if (camera) {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) openCamera()
                else requestCameraPermission.launch(Manifest.permission.CAMERA)
            } else {
                pickAttachment.launch("image/*")
            }
        }
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(12), 0, 0) }
        row.addView(MaterialButton(ctx).apply { text = "Tomar foto"; backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1565C0")); setOnClickListener { go(true) } },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) })
        row.addView(MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Subir archivo"; setOnClickListener { go(false) } },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        col.addView(row)
        sheet.setContentView(col)
        sheet.show()
    }

    /** Hoja «Datos leídos del adjunto»: una fila por dato con casilla y valor editable. */
    private fun renderAttachment(state: TechnicalChatUiState) {
        val review = state.attachment
        if (review == null) { attachDialog?.dismiss(); attachDialog = null; return }
        if (attachDialog?.isShowing == true) return
        val ctx = requireContext()
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(22)) }
        col.addView(TextView(ctx).apply {
            text = "Datos leídos del adjunto"; textSize = 20f; setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1F2937"))
        })
        col.addView(TextView(ctx).apply {
            text = review.titulo; textSize = 13f; setTextColor(Color.parseColor("#6B7280")); setPadding(0, dp(2), 0, dp(8))
        })
        val checks = mutableListOf<Triple<com.google.android.material.checkbox.MaterialCheckBox, EditText, String>>()
        val use = MaterialButton(ctx).apply { backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1565C0")) }
        fun refresh() {
            val n = checks.count { it.first.isChecked }
            use.text = "Usar $n dato${if (n == 1) "" else "s"}"
            use.isEnabled = n > 0
        }
        review.campos.forEach { f ->
            val box = com.google.android.material.checkbox.MaterialCheckBox(ctx).apply {
                text = f.label; isChecked = f.usar; isEnabled = !f.respondido && f.error == null
                setOnCheckedChangeListener { _, _ -> refresh() }
            }
            val value = EditText(ctx).apply {
                setText(f.valor); isEnabled = !f.respondido; textSize = 15f
                if (f.respondido) setTextColor(Color.parseColor("#9CA3AF"))
            }
            col.addView(box); col.addView(value)
            when {
                f.respondido -> col.addView(TextView(ctx).apply { text = "Ya respondido"; textSize = 12f; setTextColor(Color.parseColor("#9CA3AF")) })
                f.error != null -> col.addView(TextView(ctx).apply { text = f.error; textSize = 12f; setTextColor(Color.parseColor("#D32F2F")) })
            }
            checks += Triple(box, value, f.nodeId)
        }
        review.nota?.let { col.addView(TextView(ctx).apply { text = it; textSize = 12f; setTextColor(Color.parseColor("#6B7280")); setPadding(0, dp(8), 0, 0) }) }
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(14), 0, 0) }
        row.addView(MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Descartar"; setOnClickListener { sheet.dismiss() } },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) })
        use.setOnClickListener {
            val picked = checks.filter { it.first.isChecked }.map { it.third to it.second.text.toString().trim() }
                .filter { it.second.isNotBlank() }
            sheet.setOnDismissListener(null)
            sheet.dismiss()
            viewModel.usarAdjunto(picked)
        }
        row.addView(use, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        col.addView(row)
        refresh()
        sheet.setContentView(ScrollViewWrap(ctx, col))
        sheet.setOnDismissListener { attachDialog = null; viewModel.descartarAdjunto() }
        attachDialog = sheet
        sheet.show()
    }

    private fun ScrollViewWrap(ctx: android.content.Context, child: View): View =
        android.widget.ScrollView(ctx).apply { addView(child) }

    /** Ayuda: las dudas con «?» no van al chat ni a la minuta. */
    private fun showHelpDialog(question: String) {
        ChatHelpSheet.newInstance(viewModel.uiState.value.node?.text.orEmpty())
            .show(childFragmentManager, ChatHelpSheet.TAG)
        if (question.isNotBlank()) Toast.makeText(requireContext(), "Escribe tu duda en la Ayuda; no se envía al chat.", Toast.LENGTH_SHORT).show()
    }

    private fun openCamera() {
        // La foto anterior de la cámara ya se copió/comprimió: no se deja acumulando en caché.
        currentPhotoFile?.delete()
        val file = File.createTempFile("chat_photo_", ".jpg", requireContext().cacheDir)
        currentPhotoFile = file
        photoUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.provider", file
        )
        takePicture.launch(photoUri)
    }

    // ---- chips de respuesta rápida ---------------------------------------------------
    private fun addQuickReplyChip(label: String, filled: Boolean = false, compact: Boolean = false, onClick: () -> Unit) {
        val blue = com.upc.asistenteredidbi.presentation.common.NightColors.parse("#1565C0")
        val button = MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = label
            isAllCaps = false
            textSize = if (compact) 12f else 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            cornerRadius = dp(18)
            insetTop = 0
            insetBottom = 0
            minHeight = 0
            minimumHeight = dp(if (compact) 30 else 36)
            setPadding(dp(if (compact) 12 else 14), 0, dp(if (compact) 12 else 14), 0)
            if (compact) {
                // Sugerencias del chat libre: píldoras celestes sin borde, como en el prototipo.
                backgroundTintList = ColorStateList.valueOf(com.upc.asistenteredidbi.presentation.common.NightColors.parse("#E8F1FD"))
                strokeWidth = 0
                setTextColor(blue)
                typeface = android.graphics.Typeface.DEFAULT
                textSize = 14f
            } else if (filled) {
                backgroundTintList = ColorStateList.valueOf(blue)
                setTextColor(Color.WHITE)
            } else {
                setTextColor(blue)
                strokeColor = ColorStateList.valueOf(blue)
            }
            setOnClickListener {
                // El estado se actualiza sincrónicamente al responder, así que sin
                // esto los botones desaparecen en el mismo gesto del tap sin ningún
                // feedback visual. Se marca el tocado, se deshabilitan los hermanos
                // y se espera un frame antes de enviar para que ese estado se pinte.
                for (i in 0 until binding.containerQuickReplies.childCount) {
                    binding.containerQuickReplies.getChildAt(i).isEnabled = false
                }
                backgroundTintList = ColorStateList.valueOf(com.upc.asistenteredidbi.presentation.common.NightColors.parse("#1565C0"))
                setTextColor(Color.WHITE)
                viewLifecycleOwner.lifecycleScope.launch {
                    kotlinx.coroutines.delay(120)
                    onClick()
                }
            }
        }
        binding.containerQuickReplies.addView(button)
    }

    private fun addMultiSelectChips(options: List<ChatOption>) {
        val extra = multiSelectAnswers.keys.filter { k -> options.none { it.value == k } }
        (options.map { it.value to it.label } + extra.map { it to it }).forEach { (value, label) ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isClickable = true
                isChecked = multiSelectAnswers.containsKey(value)
                // El Chip ya alterna isChecked solo al tocarlo (es un CompoundButton).
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) multiSelectAnswers[value] = label else multiSelectAnswers.remove(value)
                }
                // Como el prototipo: contorno azul con fondo blanco; marcado = relleno azul.
                val blue = com.upc.asistenteredidbi.presentation.common.NightColors.parse("#1565C0")
                val checkedStates = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
                chipBackgroundColor = ColorStateList(checkedStates, intArrayOf(blue, Color.WHITE))
                setTextColor(ColorStateList(checkedStates, intArrayOf(Color.WHITE, blue)))
                chipStrokeColor = ColorStateList.valueOf(blue)
                chipStrokeWidth = dp(1).toFloat()
                isCheckedIconVisible = false
                chipMinHeight = dp(36).toFloat()
            }
            binding.containerQuickReplies.addView(chip)
        }
    }

    /** "Otro (escribir)" en una selección múltiple: agrega la opción escrita ya marcada. */
    private fun askCustomOption(node: ChatNodePrompt) {
        val input = EditText(requireContext()).apply {
            hint = "Escribe cuál es"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(dp(24), dp(16), dp(24), dp(16))
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Otro")
            .setView(input)
            .setPositiveButton("Agregar") { _, _ ->
                val name = input.text?.toString().orEmpty().trim()
                if (name.isNotBlank()) {
                    multiSelectAnswers[name] = name
                    // Se vuelve a dibujar para que la opción nueva aparezca marcada.
                    binding.containerQuickReplies.removeAllViews()
                    node.options.let { addMultiSelectChips(it) }
                    addQuickReplyChip("Otro (escribir)") { askCustomOption(node) }
                    node.noneLabel?.let { none -> addQuickReplyChip(none) { viewModel.sendAnswer("@NONE", none) } }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /** Las evidencias ya se capturaron dentro del chat: se pasa directo al diagnóstico. */
    private fun navigateToDiagnosis() {
        val state = viewModel.uiState.value
        if (state.pendingMandatory.isNotEmpty()) {
            showPendingDialog()
            return
        }

        if (!state.completed) {
            Toast.makeText(requireContext(), "Completa primero la evaluación técnica", Toast.LENGTH_SHORT).show()
            return
        }

        val proposal = state.proposal

        val equipmentJson = proposal?.equipment?.let { equipment ->
            val type = Types.newParameterizedType(List::class.java, TechnicalEquipmentRecommendation::class.java)
            moshi.adapter<List<TechnicalEquipmentRecommendation>>(type).toJson(equipment)
        }.orEmpty()

        val topologyJson = proposal?.topology?.let {
            moshi.adapter(ChatTopology::class.java).toJson(it)
        }.orEmpty()

        val answersType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        val answersJson = moshi.adapter<Map<String, String>>(answersType).toJson(state.answers)

        findNavController().navigate(
            R.id.action_chat_to_minuta,
            bundleOf(
                "evaluationId" to viewModel.evaluationId.toString(),
                "proposalSummary" to proposal?.summary.orEmpty(),
                "topologyText" to proposal?.topologyText.orEmpty(),
                "equipmentJson" to equipmentJson,
                "topologyJson" to topologyJson,
                "answersJson" to answersJson,
                "score" to (proposal?.score ?: -1),
                "minutaId" to (state.minutaId ?: -1L)
            )
        )
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    controlarPings(state)
                    renderLoading(state)
                    renderProgress(state)
                    renderMessages(state)
                    renderQuickReplies(state)
                    renderCompletion(state)
                    renderError(state)
                }
            }
        }
    }

    // ---------- Ubicar el puerto del switch (QA D1/F3) ----------
    // La pregunta dice «la app está enviando pings a su IP durante 20 s, 2 s sí y 2 s no»: aquí se envían de verdad, desde el celular
    // conectado al WiFi del local, mientras la pregunta siga abierta. Así la luz del puerto parpadea al ritmo que se le pide al técnico.
    private var pingJob: kotlinx.coroutines.Job? = null
    private var pingNodo: String? = null

    private fun controlarPings(state: TechnicalChatUiState) {
        val node = state.node
        val ip = node?.text?.let { Regex("""IP (\d{1,3}(?:\.\d{1,3}){3})\): la app est[aá] enviando pings""").find(it)?.groupValues?.get(1) }
        val clave = if (ip != null) "${node.nodeId}|${node.scope}|$ip" else null
        if (clave == pingNodo) return
        pingJob?.cancel(); pingJob = null; pingNodo = clave
        if (ip == null) return
        pingJob = viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val fin = System.currentTimeMillis() + 20_000
            while (System.currentTimeMillis() < fin) {
                val ronda = System.currentTimeMillis() + 2_000        // 2 s con pings seguidos…
                while (System.currentTimeMillis() < ronda) {
                    runCatching { java.net.InetAddress.getByName(ip).isReachable(250) }
                    kotlinx.coroutines.delay(50)
                }
                kotlinx.coroutines.delay(2_000)                        // …y 2 s sin nada
            }
        }
        android.widget.Toast.makeText(requireContext(), "Enviando pings a $ip durante 20 s…", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun renderLoading(state: TechnicalChatUiState) {
        val inputEnabled = !state.isLoading && !state.isSending

        binding.etTextInput.isEnabled = inputEnabled
        binding.btnSubmit.isEnabled = inputEnabled
        binding.btnSubmit.alpha = if (inputEnabled) 1f else 0.45f
        if (state.node?.inputType != TechnicalChatInputType.SUMMARY) {
            binding.btnAttach.isEnabled = inputEnabled
            binding.btnAttach.alpha = if (inputEnabled) 1f else 0.45f
        }
        renderAttachment(state)

        binding.etTextInput.hint = when {
            state.isLoading -> "Iniciando evaluación..."
            state.isSending && state.completed -> "Espera un momento…"
            state.isSending -> "Procesando respuesta..."
            state.completed && state.currentMap != null -> "Pide un cambio al mapa…"
            state.completed -> "Pregúntale algo al asistente…"
            else -> state.node?.hint?.takeIf { it.isNotBlank() } ?: "Escribe tu respuesta..."
        }

        binding.tvSubtitle.text = when {
            state.isSending && state.node?.inputType == TechnicalChatInputType.EVIDENCE -> "Analizando imagen…"
            state.isSending && state.completed -> "Generando mapa…"
            state.completed && state.currentMap != null -> "Mapa generado"
            state.completed || state.node?.inputType == TechnicalChatInputType.SUMMARY -> "Evaluación completada"
            else -> "Evaluación en curso"
        }
    }

    /** Total estimado de preguntas del último nodo visto: sirve para el "N de N preguntas" final. */
    private var lastQuestionTotal = 0

    private fun renderProgress(state: TechnicalChatUiState) {
        binding.progressEvaluation.progress = state.progressPercent
        binding.tvProgressPercent.text = "${state.progressPercent}%"

        // "Pregunta N de T": T es una estimación (los loops por caja/impresora/área
        // la hacen crecer), por eso lleva "~" cuando todavía puede cambiar.
        val node = state.node
        val atSummary = node?.inputType == TechnicalChatInputType.SUMMARY
        if (node != null && node.questionTotal > 0) lastQuestionTotal = node.questionTotal
        if (atSummary) {
            binding.progressEvaluation.progress = 100
            binding.tvProgressPercent.text = "100%"
        }
        val block = node?.blockLabel?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
        binding.tvQuestionCounter.text = when {
            state.editSnapshot != null && !atSummary -> "Modo edición" + if (node != null && node.questionTotal > 0) " · Pregunta ${node.questionNumber} de ~${node.questionTotal}" else ""
            atSummary && node != null && node.questionsTotal() > 0 -> "${node.questionsTotal()} preguntas · ${node.evidencesTotal} evidencias"
            state.completed || atSummary ->
                if (lastQuestionTotal > 0) "$lastQuestionTotal de $lastQuestionTotal preguntas"
                else "Evaluación completa"
            node != null && node.evidenceTotal > 0 && node.kind == "evidence" ->
                "Evidencia ${node.evidenceNumber} de ~${node.evidenceTotal}$block"
            node != null && node.evidenceTotal > 0 && node.questionTotal == 0 ->
                "Evidencia ${node.evidenceNumber} de ~${node.evidenceTotal}$block"
            node != null && node.questionTotal > 0 ->
                "Pregunta ${node.questionNumber} de ~${node.questionTotal}$block"
            else -> "Preparando evaluación"
        }
        val pend = state.pendingMandatory.size
        if (pend > 0) binding.tvQuestionCounter.append(" · $pend ${if (pend == 1) "obligatoria" else "obligatorias"} por completar")
    }

    private fun ChatNodePrompt.questionsTotal(): Int = lastQuestionTotal

    private fun renderMessages(state: TechnicalChatUiState) {
        messagesAdapter.submitList(state.messages.toList()) {
            if (state.messages.isNotEmpty()) {
                binding.rvMessages.post {
                    binding.rvMessages.scrollToPosition(state.messages.lastIndex)
                }
            }
        }
    }

    private fun renderCompletion(state: TechnicalChatUiState) {
        binding.btnGoEvidence.isVisible = false
        val n = state.pendingMandatory.size
        if (n > 0 && state.completed && state.editSnapshot == null) {
            binding.actionBar.addView(
                actionButton("Subir fotos pendientes ($n)", filled = true, iconRes = R.drawable.ic_camera) { showPendingDialog() },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { bottomMargin = dp(8) }
            )
        }
        pendingDialogRefresh?.invoke(state)
    }

    // ---- 0.0.7: «Guardar como correcta» y «Subir después» ---------------------------------
    private var pendingDialog: AlertDialog? = null
    private var pendingDialogRefresh: ((TechnicalChatUiState) -> Unit)? = null

    /** Hoja de «Guardar como correcta»: muestra lo que vio la IA y pide una nota opcional (no obligatoria).
     *  En la minuta solo se muestra la nota, si se escribió. */
    private fun showForceDialog(message: String?, enObra: Boolean, onSave: (String?) -> Unit) {
        val ctx = requireContext()
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0) }
        message?.let { box.addView(TextView(ctx).apply { text = it; textSize = 13f; setTextColor(Color.parseColor("#7A4A00")) }) }
        val nota = EditText(ctx).apply { hint = "Nota (opcional). Ej.: aquí irá la impresora; aún no hay punto de red"; textSize = 14f }
        box.addView(nota)
        val chips = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        (listOf("La foto es correcta", "Aún no hay puntos", "El cliente no permite") + if (enObra) listOf("Local en obra") else emptyList())
            .forEach { t -> chips.addView(TextView(ctx).apply {
                text = t; textSize = 13f; setTextColor(Color.parseColor("#1565C0")); setPadding(0, dp(8), 0, dp(4))
                setOnClickListener { nota.setText(t); nota.setSelection(t.length) }
            }) }
        box.addView(chips)
        AlertDialog.Builder(ctx)
            .setTitle("Guardar como correcta")
            .setMessage("Se guarda la foto. Si escribes una nota, se mostrará en la minuta junto a la foto.")
            .setView(box)
            .setPositiveButton("Guardar y continuar") { _, _ -> onSave(nota.text?.toString()?.trim()?.ifBlank { null }) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** «Evidencias obligatorias por completar»: una fila por foto dejada con «Subir después». La minuta no se genera
     *  hasta que la lista quede vacía. */
    private fun showPendingDialog() {
        val ctx = requireContext()
        val list = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(8)) }
        fun fill(state: TechnicalChatUiState) {
            list.removeAllViews()
            if (state.pendingMandatory.isEmpty()) {
                list.addView(TextView(ctx).apply { text = "Ya están todas las fotos obligatorias. Puedes generar la minuta."; textSize = 14f })
                return
            }
            state.pendingMandatory.forEach { p ->
                list.addView(TextView(ctx).apply {
                    text = p.name + (p.text.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: "")
                    textSize = 14f; setTextColor(Color.parseColor("#1F2937")); setPadding(0, dp(10), 0, dp(2))
                })
                p.message?.let { m -> list.addView(TextView(ctx).apply { text = m; textSize = 12f; setTextColor(Color.parseColor("#9E2B23")) }) }
                val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                row.addView(actionButton("Tomar foto", filled = true, iconRes = R.drawable.ic_camera) {
                    pendingTarget = p.nodeId; requestCameraPermission.launch(Manifest.permission.CAMERA)
                }, LinearLayout.LayoutParams(0, dp(42), 1f))
                row.addView(actionButton("Subir foto", filled = false, iconRes = R.drawable.ic_image) {
                    pendingTarget = p.nodeId; pickImages.launch("image/*")
                }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginStart = dp(8) })
                list.addView(row)
                if (p.canForce) list.addView(actionButton("Guardar como correcta", filled = true, iconRes = R.drawable.ic_check_circle) {
                    showForceDialog(p.message, p.enObra) { nota -> viewModel.sendPendingPhoto(p.nodeId, emptyList(), "forzar", nota) }
                }.apply { backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E7D52")) },
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)).apply { topMargin = dp(6) })
            }
        }
        fill(viewModel.uiState.value)
        pendingDialogRefresh = { st -> if (pendingDialog?.isShowing == true) fill(st) }
        pendingDialog = AlertDialog.Builder(ctx)
            .setTitle("Evidencias obligatorias por completar")
            .setMessage("Súbelas para generar la minuta. Las opcionales que saltaste no la bloquean.")
            .setView(android.widget.ScrollView(ctx).apply { addView(list) })
            .setPositiveButton("Cerrar", null)
            .setOnDismissListener { pendingDialogRefresh = null }
            .show()
    }

    private fun renderError(state: TechnicalChatUiState) {
        val message = state.errorMessage ?: return
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        viewModel.clearError()
    }

    override fun onDestroyView() {
        pingJob?.cancel(); pingNodo = null
        binding.rvMessages.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
