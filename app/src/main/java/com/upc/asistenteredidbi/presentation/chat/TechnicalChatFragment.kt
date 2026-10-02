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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
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

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && photoUri != null) {
                viewModel.sendPhotos(listOf(photoUri!!))
            }
        }

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNotEmpty()) viewModel.sendPhotos(uris)
        }

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
                when (action) {
                    "VIEW_ALL" -> showEvidenceGallery(unresolvedOnly = false)
                    "RESOLVE" -> showEvidenceGallery(unresolvedOnly = true)
                    "GENERATE_MAP", "REGENERATE_MAP" -> viewModel.generateMap()
                    "EDIT_MAP" -> findNavController().navigate(
                        R.id.action_chat_to_topologyEditor,
                        bundleOf("evaluationId" to viewModel.evaluationId.toString())
                    )
                    "VIEW_PROPOSAL", "VIEW_ANALYSIS" -> navigateToDiagnosis()
                }
            }
        )

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messagesAdapter
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

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnSubmit.setOnClickListener { submitAnswer() }

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
    }

    private fun submitAnswer() {
        if (viewModel.uiState.value.completed) {
            val text = binding.etTextInput.text?.toString().orEmpty().trim()
            if (text.isNotBlank()) {
                viewModel.sendFreeText(text)
                binding.etTextInput.text?.clear()
            }
            return
        }
        val node = viewModel.uiState.value.node ?: return
        val isMultiSelect = node.inputType == TechnicalChatInputType.MULTI_SELECT

        if (isMultiSelect) {
            if (multiSelectAnswers.isEmpty() && node.required) {
                Toast.makeText(requireContext(), "Selecciona al menos una opción", Toast.LENGTH_SHORT).show()
                return
            }
            viewModel.sendAnswer(
                value = multiSelectAnswers.keys.joinToString(","),
                display = multiSelectAnswers.values.joinToString(", ")
            )
            multiSelectAnswers.clear()
            return
        }

        val answer = binding.etTextInput.text?.toString().orEmpty().trim()
        if (answer.isBlank() && node.required) {
            Toast.makeText(requireContext(), "Escribe una respuesta", Toast.LENGTH_SHORT).show()
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

    /** P06 (fecha de la visita): calendario en vez de teclear dd/mm/aaaa. */
    private fun showDatePicker() {
        val cal = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                binding.etTextInput.setText(String.format(java.util.Locale.US, "%02d/%02d/%04d", day, month + 1, year))
                binding.etTextInput.setSelection(binding.etTextInput.text?.length ?: 0)
            },
            cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showEvidenceGallery(unresolvedOnly: Boolean) {
        EvidenceGalleryDialog.newInstance(unresolvedOnly).show(childFragmentManager, "evidence_gallery")
    }

    /** "Corregir": un campo editable por cada dato que leyó la IA. */
    private fun showCorrectDialog(evidence: ChatEvidenceResult) {
        val fields = EvidencePresentation.editableFields(evidence)
        val inputs = fields.map { field ->
            EditText(requireContext()).apply {
                hint = field.label
                setText(field.value)
                inputType = if (field.numeric) {
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                } else {
                    InputType.TYPE_CLASS_TEXT
                }
            }
        }
        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
            fields.forEachIndexed { i, field ->
                addView(TextView(requireContext()).apply {
                    text = field.label
                    textSize = 12f
                    setTextColor(Color.parseColor("#6B7280"))
                    setPadding(0, dp(10), 0, 0)
                })
                addView(inputs[i])
            }
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Corregir lo que leí")
            .setView(android.widget.ScrollView(requireContext()).apply { addView(form) })
            .setPositiveButton("Guardar") { _, _ ->
                val changed = linkedMapOf<String, Any?>()
                fields.forEachIndexed { i, field ->
                    val text = inputs[i].text?.toString().orEmpty().trim()
                    if (text != field.value && text.isNotEmpty()) {
                        changed[field.key] = if (field.numeric) text.replace(',', '.').toDoubleOrNull() ?: text else text
                    }
                }
                viewModel.correctEvidence(changed)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Controles según el tipo de nodo actual (texto, chips, evidencia, aviso, resumen). */
    private fun renderQuickReplies(state: TechnicalChatUiState) {
        binding.containerQuickReplies.removeAllViews()
        binding.actionBar.removeAllViews()

        val node = state.node
        val canAnswer = !state.isLoading && !state.isSending && !state.completed
        val confirming = state.pending != null
        val type = node?.inputType

        binding.etTextInput.inputType = when (node?.keyboard) {
            "number" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            "phone" -> InputType.TYPE_CLASS_PHONE
            "date" -> InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE
            else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        // Texto tipeado para una pregunta abandonada no debe colarse en la siguiente:
        // se limpia (o se prellena con el valor por defecto) apenas cambia el nodo.
        val key = node?.let { "${it.nodeId}|${it.scope}|${state.messages.size}" }
        if (key != lastRenderedKey) {
            binding.etTextInput.setText(node?.defaultValue.orEmpty())
            binding.etTextInput.setSelection(binding.etTextInput.text?.length ?: 0)
            multiSelectAnswers.clear()
            // El scroll horizontal de chips conserva su posición entre preguntas:
            // sin esto, las opciones de la siguiente pregunta quedan fuera de pantalla.
            binding.scrollQuickReplies.scrollTo(0, 0)
            lastRenderedKey = key
        }

        val freeChat = state.completed
        val needsTextInput = freeChat || (!confirming &&
            (type == TechnicalChatInputType.TEXT || type == TechnicalChatInputType.NUMBER))
        val showTextRow = needsTextInput
        binding.etTextInput.isVisible = needsTextInput
        binding.containerTextRow.isVisible = showTextRow

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

        if (confirming) {
            val pending = state.pending
            val followUp = pending?.followUps?.firstOrNull()
            if (followUp != null) {
                // Pregunta de confirmación del asistente (proveedor distinto, dispositivo sin documentar).
                followUp.options.forEachIndexed { index, option ->
                    addQuickReplyChip(option, filled = index == 0) { viewModel.answerFollowUp(option) }
                }
            } else {
                addQuickReplyChip("Sí, es correcto", filled = true) { viewModel.confirmEvidence() }
                val evidence = pending?.lastEvidence
                if (evidence != null && EvidencePresentation.editableFields(evidence).isNotEmpty()) {
                    addQuickReplyChip("Corregir") { showCorrectDialog(evidence) }
                }
                addQuickReplyChip("Cambiar foto") { viewModel.changeEvidencePhoto() }
            }
        } else {
            when (type) {
                TechnicalChatInputType.YES_NO -> {
                    addQuickReplyChip("Sí") { viewModel.sendAnswer("SI", "Sí") }
                    addQuickReplyChip("No") { viewModel.sendAnswer("NO", "No") }
                }

                TechnicalChatInputType.CHOICE -> node?.options?.forEach { option ->
                    addQuickReplyChip(option.label) { viewModel.sendAnswer(option.value, option.label) }
                }

                TechnicalChatInputType.MULTI_SELECT -> {
                    node?.options?.let { addMultiSelectChips(it) }
                    addPrimaryAction("Continuar") { submitAnswer() }
                }

                TechnicalChatInputType.TEXT, TechnicalChatInputType.NUMBER -> {
                    if (node?.keyboard == "date") addQuickReplyChip("📅 Elegir fecha") { showDatePicker() }
                    if (node?.required == false) addQuickReplyChip("Omitir") { viewModel.sendAnswer("", "Omitir") }
                }

                TechnicalChatInputType.EVIDENCE -> addEvidenceActions(node)
                TechnicalChatInputType.ALERT -> addPrimaryAction("Entendido") {
                    viewModel.sendAnswer("", "Entendido")
                }

                TechnicalChatInputType.SUMMARY -> {
                    addPrimaryAction("Generar minuta") {
                        viewModel.sendAnswer("GENERAR_MINUTA", "Generar minuta")
                    }
                    addSecondaryAction("Generar mapa con IA") {
                        viewModel.sendAnswer("GENERAR_MAPA_IA", "Generar mapa con IA")
                    }
                }

                else -> Unit
            }
        }

        binding.scrollQuickReplies.isVisible = binding.containerQuickReplies.childCount > 0
        binding.actionBar.isVisible = binding.actionBar.childCount > 0
        capQuickRepliesHeight()
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

    // ---- evidencias: Tomar foto / Galería / Omitir ---------------------------------
    private fun addEvidenceActions(node: ChatNodePrompt?) {
        val bar = binding.actionBar
        val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(actionButton("Tomar foto", filled = true, iconRes = R.drawable.ic_camera) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }, LinearLayout.LayoutParams(0, dp(46), 1f))
        row.addView(actionButton("Galería", filled = false, iconRes = R.drawable.ic_image) {
            pickImages.launch("image/*")
        }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { marginStart = dp(10) })
        bar.addView(row)
        bar.addView(TextView(requireContext()).apply {
            text = "Omitir por ahora · la pediré al final"
            textSize = 13f
            setTextColor(Color.parseColor("#6B7280"))
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(2))
            setOnClickListener { viewModel.skipEvidence() }
        })
    }

    private fun addPrimaryAction(label: String, onClick: () -> Unit) {
        binding.actionBar.addView(
            actionButton(label, filled = true, onClick = onClick),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { bottomMargin = dp(8) }
        )
    }

    private fun addSecondaryAction(label: String, onClick: () -> Unit) {
        binding.actionBar.addView(
            actionButton(label, filled = false, onClick = onClick),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
        )
    }

    private fun actionButton(
        label: String,
        filled: Boolean,
        iconRes: Int? = null,
        onClick: () -> Unit
    ): MaterialButton {
        val blue = Color.parseColor("#2F6FCB")
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
                backgroundTintList = ColorStateList.valueOf(Color.WHITE)
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
        val blue = Color.parseColor("#2F6FCB")
        val button = MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = label
            isAllCaps = false
            textSize = if (compact) 12f else 13f
            cornerRadius = dp(18)
            insetTop = 0
            insetBottom = 0
            minHeight = 0
            minimumHeight = dp(if (compact) 30 else 36)
            if (compact) setPadding(dp(12), 0, dp(12), 0)
            if (filled) {
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
                backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1565C0"))
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
        multiSelectAnswers.clear()

        options.forEach { option ->
            val chip = Chip(requireContext()).apply {
                text = option.label
                isCheckable = true
                isClickable = true
                // El Chip ya alterna isChecked solo al tocarlo (es un
                // CompoundButton); un OnClickListener que también lo alternara
                // duplicaba el toggle y lo dejaba siempre en el estado original.
                setOnCheckedChangeListener { button, isChecked ->
                    when {
                        !isChecked -> multiSelectAnswers.keys.removeAll { it == option.value || it.startsWith("OTRO:") && option.value == "OTRO" }
                        option.value == "OTRO" -> askCustomArea(button as Chip)
                        else -> multiSelectAnswers[option.value] = option.label
                    }
                }
                // Como el prototipo: contorno azul con fondo blanco; marcado = relleno azul.
                val blue = Color.parseColor("#2F6FCB")
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

    /** "Otro": pide el nombre del área y lo agrega como opción personalizada. */
    private fun askCustomArea(chip: Chip) {
        val input = EditText(requireContext()).apply {
            hint = "Nombre del área"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setPadding(dp(24), dp(16), dp(24), dp(16))
        }
        AlertDialog.Builder(requireContext())
            .setTitle("¿Cómo se llama el área?")
            .setView(input)
            .setPositiveButton("Agregar") { _, _ ->
                val name = input.text?.toString().orEmpty().trim()
                if (name.isBlank()) {
                    chip.isChecked = false
                } else {
                    multiSelectAnswers["OTRO:$name"] = name
                    chip.text = "Otro: $name"
                }
            }
            .setNegativeButton("Cancelar") { _, _ -> chip.isChecked = false }
            .setOnCancelListener { chip.isChecked = false }
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /** Las evidencias ya se capturaron dentro del chat: se pasa directo al diagnóstico. */
    private fun navigateToDiagnosis() {
        val state = viewModel.uiState.value

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

    private fun renderLoading(state: TechnicalChatUiState) {
        val inputEnabled = !state.isLoading && !state.isSending

        binding.etTextInput.isEnabled = inputEnabled
        binding.btnSubmit.isEnabled = inputEnabled
        binding.btnSubmit.alpha = if (inputEnabled) 1f else 0.45f

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
            state.completed -> "Evaluación completada"
            else -> "Evaluación en curso"
        }
    }

    private fun renderProgress(state: TechnicalChatUiState) {
        binding.progressEvaluation.progress = state.progressPercent
        binding.tvProgressPercent.text = "${state.progressPercent}%"

        // Progreso por bloque (A..I): como las ramas del flujo cambian cuántas
        // preguntas habrá, no se muestra "X de N".
        val node = state.node
        binding.tvQuestionCounter.text = when {
            state.completed -> "Evaluación completa"
            node != null && node.blockCount > 0 ->
                "Bloque ${node.blockIndex} de ${node.blockCount} · ${node.blockLabel}"
            else -> "Preparando evaluación"
        }
    }

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
    }

    private fun renderError(state: TechnicalChatUiState) {
        val message = state.errorMessage ?: return
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        viewModel.clearError()
    }

    override fun onDestroyView() {
        binding.rvMessages.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
