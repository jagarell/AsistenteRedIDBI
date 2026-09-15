package com.upc.asistenteredidbi.presentation.chat

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.upc.asistenteredidbi.domain.model.ChatTopology
import com.upc.asistenteredidbi.domain.model.TechnicalChatInputType
import com.upc.asistenteredidbi.domain.model.TechnicalEquipmentRecommendation
import com.upc.asistenteredidbi.presentation.common.toHierarchicalText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TechnicalChatFragment : Fragment() {

    private var _binding: FragmentTechnicalChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TechnicalChatViewModel by viewModels()

    private lateinit var messagesAdapter: ChatMessagesAdapter

    /** Selección acumulada para la pregunta MULTI_SELECT actual (wifi_zones,
     * services). Se envía con el botón de enviar de siempre — antes había un
     * botón "Enviar selección" al final del scroll horizontal de chips, que
     * quedaba fuera de pantalla y el usuario nunca lo encontraba. */
    private val multiSelectAnswers = mutableSetOf<String>()

    /** Último paso para el que se renderizaron controles — permite limpiar la
     * caja de texto exactamente cuando cambia de pregunta, sin depender de
     * que el usuario haya usado el botón de enviar (ver bug: texto tipeado
     * para una pregunta que quedaba abandonado y se colaba en la siguiente). */
    private var lastRenderedStep: Int = -1

    @Inject
    lateinit var moshi: Moshi

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTechnicalChatBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupInitialState()
        setupClicks()
        observeViewModel()
    }

    private fun setupRecycler() {
        messagesAdapter = ChatMessagesAdapter { message ->
            viewModel.editAnswer(message.id)
        }

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(
                requireContext()
            ).apply {
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
        binding.tvStatus.text = "Cargando"
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSubmit.setOnClickListener {
            submitAnswer()
        }

        binding.etTextInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitAnswer()
                true
            } else {
                false
            }
        }

        binding.btnGoEvidence.setOnClickListener {
            navigateToEvidence()
        }

        binding.btnMore.setOnClickListener {
            showProposal()
        }
    }

    private fun submitAnswer() {
        val isMultiSelect =
            viewModel.uiState.value.currentInputType == TechnicalChatInputType.MULTI_SELECT

        val answer = if (isMultiSelect) {
            multiSelectAnswers.joinToString(", ")
        } else {
            binding.etTextInput.text?.toString().orEmpty().trim()
        }

        if (answer.isBlank()) {
            Toast.makeText(
                requireContext(),
                if (isMultiSelect) "Selecciona al menos una opción" else "Escribe una respuesta",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewModel.sendAnswer(answer)
        binding.etTextInput.text?.clear()
        multiSelectAnswers.clear()
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
                    proposal.asIsFindings.forEach { finding ->
                        appendLine("• $finding")
                    }
                }
                appendLine()
                appendLine("Propuesta recomendada (TO-BE):")
                proposal.recommendations.forEach { recommendation ->
                    appendLine("• $recommendation")
                }
                appendLine()
                appendLine("Topología:")
                // Preferimos la topología estructurada (nodos/enlaces con tipo
                // de conexión) construida a partir del chat; si no llegó,
                // usamos el resumen en texto plano como respaldo.
                append(proposal.topology?.toHierarchicalText() ?: proposal.topologyText)
            }
        }

        Toast.makeText(
            requireContext(),
            text,
            Toast.LENGTH_LONG
        ).show()
    }

    /** Renderiza controles rápidos según el tipo de nodo (HU05: 23 nodos). */
    private fun renderQuickReplies(state: TechnicalChatUiState) {
        binding.containerQuickReplies.removeAllViews()

        val canAnswer = !state.isLoading && !state.isSending && !state.completed

        binding.etTextInput.inputType = when (state.currentInputType) {
            TechnicalChatInputType.NUMBER ->
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

            else -> InputType.TYPE_CLASS_TEXT
        }

        // Antes el texto tipeado para una pregunta abandonada (el usuario
        // empezó a escribir y después tocó un botón Sí/No/opción en vez de
        // enviar) quedaba en la caja y podía terminar enviándose como
        // respuesta de la SIGUIENTE pregunta sin que nadie lo notara. Se
        // limpia apenas cambia el paso, sin depender del camino de envío.
        if (state.currentStep != lastRenderedStep) {
            binding.etTextInput.text?.clear()
            lastRenderedStep = state.currentStep
        }

        // YES_NO/CHOICE responden con un tap directo — no tienen por qué
        // convivir con el teclado de texto. MULTI_SELECT sí necesita el botón
        // de enviar (para confirmar la selección de chips), pero no la caja.
        val needsTextInput = state.currentInputType == TechnicalChatInputType.TEXT ||
            state.currentInputType == TechnicalChatInputType.NUMBER
        val showTextRow = state.currentInputType != TechnicalChatInputType.YES_NO &&
            state.currentInputType != TechnicalChatInputType.CHOICE
        binding.etTextInput.isVisible = needsTextInput
        binding.containerTextRow.isVisible = showTextRow

        if (!canAnswer) {
            binding.scrollQuickReplies.isVisible = false
            return
        }

        when (state.currentInputType) {
            TechnicalChatInputType.YES_NO -> {
                addQuickReplyButton("Sí")
                addQuickReplyButton("No")
            }

            TechnicalChatInputType.CHOICE -> {
                state.currentOptions.forEach { option ->
                    addQuickReplyButton(option)
                }
            }

            TechnicalChatInputType.MULTI_SELECT -> {
                if (state.currentOptions.isNotEmpty()) {
                    addMultiSelectChips(state.currentOptions)
                }
            }

            else -> Unit
        }

        binding.scrollQuickReplies.isVisible =
            binding.containerQuickReplies.childCount > 0
    }

    private fun addQuickReplyButton(label: String) {
        val button = MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            setOnClickListener {
                // El estado se actualiza sincrónicamente al llamar sendAnswer(),
                // así que sin esto los botones desaparecen en el mismo gesto del
                // tap sin ningún feedback visual (de ahí la sensación de "no
                // pinta"). Se marca el tocado, se deshabilitan los hermanos, y
                // se espera un frame antes de enviar para que ese estado sí
                // llegue a renderizarse.
                for (i in 0 until binding.containerQuickReplies.childCount) {
                    binding.containerQuickReplies.getChildAt(i).isEnabled = false
                }
                // No es un widget checkable: se pinta con el mismo azul que
                // usan los chips seleccionados (chip_selector_blue), pero como
                // color plano — un ColorStateList por state_checked no
                // aplicaría nada aquí.
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#1565C0")
                )
                setTextColor(android.graphics.Color.WHITE)
                viewLifecycleOwner.lifecycleScope.launch {
                    kotlinx.coroutines.delay(120)
                    viewModel.sendAnswer(label)
                }
            }
        }
        binding.containerQuickReplies.addView(button)
        applyEndMargin(button)
    }

    private fun addMultiSelectChips(options: List<String>) {
        multiSelectAnswers.clear()

        options.forEach { option ->
            val chip = Chip(requireContext()).apply {
                text = option
                isCheckable = true
                isClickable = true
                // El Chip ya alterna isChecked solo al tocarlo (es un
                // CompoundButton); un OnClickListener que también lo alternara
                // duplicaba el toggle y lo dejaba siempre en el estado
                // original, por lo que nunca se marcaba visualmente.
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) multiSelectAnswers.add(option) else multiSelectAnswers.remove(option)
                }
                // Sin esto el chip alterna su estado pero no se nota: usa los
                // selectores ya definidos para pintar azul/blanco al marcarse.
                chipBackgroundColor = ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.chip_selector_blue
                )
                setTextColor(
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.chip_text_selector
                    )
                )
            }
            binding.containerQuickReplies.addView(chip)
            applyEndMargin(chip)
        }
    }

    /** Aplica un margen final a una vista ya agregada a [containerQuickReplies]. */
    private fun applyEndMargin(view: View) {
        val marginEnd = (8 * resources.displayMetrics.density).toInt()
        (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            it.marginEnd = marginEnd
            view.layoutParams = it
        }
    }

    private fun navigateToEvidence() {
        val state = viewModel.uiState.value

        if (!state.completed) {
            Toast.makeText(
                requireContext(),
                "Completa primero la evaluación técnica",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val proposal = state.proposal

        val equipmentJson = proposal?.equipment?.let { equipment ->
            val type = Types.newParameterizedType(
                List::class.java,
                TechnicalEquipmentRecommendation::class.java
            )
            moshi.adapter<List<TechnicalEquipmentRecommendation>>(type).toJson(equipment)
        }.orEmpty()

        val topologyJson = proposal?.topology?.let {
            moshi.adapter(ChatTopology::class.java).toJson(it)
        }.orEmpty()

        val answersType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        val answersJson = moshi.adapter<Map<String, String>>(answersType).toJson(state.answers)

        findNavController().navigate(
            R.id.action_chat_to_evidence,
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

    private fun renderLoading(
        state: TechnicalChatUiState
    ) {
        val inputEnabled =
            !state.isLoading &&
                    !state.isSending &&
                    !state.completed

        binding.etTextInput.isEnabled = inputEnabled
        binding.btnSubmit.isEnabled = inputEnabled

        binding.btnSubmit.alpha =
            if (inputEnabled) 1f else 0.45f

        binding.etTextInput.hint = when {
            state.isLoading ->
                "Iniciando evaluación..."

            state.isSending ->
                "Procesando respuesta..."

            state.completed ->
                "Evaluación completada"

            else ->
                "Escribe tu respuesta..."
        }
    }

    private fun renderProgress(
        state: TechnicalChatUiState
    ) {
        binding.progressEvaluation.progress =
            state.progressPercent

        binding.tvProgressPercent.text =
            "${state.progressPercent}%"

        binding.tvStatus.text = when {
            state.isLoading -> "Cargando"
            state.completed -> "Completado"
            else -> "En progreso"
        }

        binding.tvQuestionCounter.text = when {
            state.totalQuestions <= 0 -> {
                "Preparando evaluación"
            }

            state.completed -> {
                "${state.totalQuestions} de ${state.totalQuestions} preguntas"
            }

            else -> {
                val currentQuestion = minOf(
                    state.answeredQuestions + 1,
                    state.totalQuestions
                )

                "Pregunta $currentQuestion de ${state.totalQuestions}"
            }
        }
    }

    private fun renderMessages(
        state: TechnicalChatUiState
    ) {
        messagesAdapter.submitList(
            state.messages.toList()
        ) {
            if (state.messages.isNotEmpty()) {
                binding.rvMessages.post {
                    binding.rvMessages.scrollToPosition(
                        state.messages.lastIndex
                    )
                }
            }
        }
    }

    private fun renderCompletion(
        state: TechnicalChatUiState
    ) {
        binding.btnGoEvidence.isVisible =
            state.completed
    }

    private fun renderError(
        state: TechnicalChatUiState
    ) {
        val message = state.errorMessage ?: return

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()

        viewModel.clearError()
    }

    override fun onDestroyView() {
        binding.rvMessages.adapter = null
        _binding = null
        super.onDestroyView()
    }
}