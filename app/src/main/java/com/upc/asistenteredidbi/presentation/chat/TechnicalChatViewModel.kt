package com.upc.asistenteredidbi.presentation.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.upc.asistenteredidbi.data.session.ChatLocalFiles
import com.upc.asistenteredidbi.data.session.ChatProgressSnapshot
import com.upc.asistenteredidbi.data.session.ChatProgressStore
import com.upc.asistenteredidbi.data.util.PhotoProcessingException
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.domain.model.ChatNodePrompt
import com.upc.asistenteredidbi.domain.model.ChatTopology
import com.upc.asistenteredidbi.domain.model.MinutaContentPayload
import com.upc.asistenteredidbi.domain.model.NetworkMap
import com.upc.asistenteredidbi.domain.model.TechnicalChatInputType
import com.upc.asistenteredidbi.domain.model.TechnicalChatProgress
import com.upc.asistenteredidbi.domain.model.TechnicalChatProposal
import com.upc.asistenteredidbi.domain.repository.EvaluationRepository
import com.upc.asistenteredidbi.domain.usecase.AmendTechnicalChatUseCase
import com.upc.asistenteredidbi.domain.usecase.AnswerTechnicalChatUseCase
import com.upc.asistenteredidbi.domain.usecase.AnswerTechnicalChatWithPhotosUseCase
import com.upc.asistenteredidbi.domain.usecase.CreateMinutaUseCase
import com.upc.asistenteredidbi.domain.usecase.GenerateMapUseCase
import com.upc.asistenteredidbi.domain.usecase.GetMapUseCase
import com.upc.asistenteredidbi.domain.usecase.MapCommandUseCase
import com.upc.asistenteredidbi.domain.usecase.SaveMapUseCase
import com.upc.asistenteredidbi.domain.usecase.StartTechnicalChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class TechnicalChatUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    /** Nodo que toca responder; null cuando el flujo terminó. */
    val node: ChatNodePrompt? = null,
    /** Estado opaco del flujo, tal como lo devolvió el motor. */
    val flowState: String? = null,
    val answeredQuestions: Int = 0,
    val totalQuestions: Int = 0,
    val progressPercent: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val completed: Boolean = false,
    val proposal: TechnicalChatProposal? = null,
    val minutaId: Long? = null,
    val errorMessage: String? = null,
    /** Respuesta del motor a una evidencia que el técnico todavía no confirmó. */
    val pending: TechnicalChatProgress? = null,
    val evidences: List<ChatEvidenceItem> = emptyList(),
    /** Mapa de red de la evaluación (tras "Generar mapa con IA"); null mientras no exista. */
    val currentMap: NetworkMap? = null
)

@HiltViewModel
class TechnicalChatViewModel @Inject constructor(
    private val startTechnicalChatUseCase: StartTechnicalChatUseCase,
    private val answerTechnicalChatUseCase: AnswerTechnicalChatUseCase,
    private val answerWithPhotosUseCase: AnswerTechnicalChatWithPhotosUseCase,
    private val amendUseCase: AmendTechnicalChatUseCase,
    private val generateMapUseCase: GenerateMapUseCase,
    private val getMapUseCase: GetMapUseCase,
    private val saveMapUseCase: SaveMapUseCase,
    private val mapCommandUseCase: MapCommandUseCase,
    private val evaluationRepository: EvaluationRepository,
    private val createMinutaUseCase: CreateMinutaUseCase,
    private val chatProgressStore: ChatProgressStore,
    private val localFiles: ChatLocalFiles,
    private val moshi: Moshi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Null si el argumento de navegación no trae un evaluationId real y parseable —
     *  en vez de asumir la evaluación 1, se falla explícito (ver [init]). */
    private val validEvaluationId: Long? =
        savedStateHandle.get<String>("evaluationId")?.toLongOrNull()

    val evaluationId: Long = validEvaluationId ?: -1L

    private val _uiState = MutableStateFlow(TechnicalChatUiState())

    val uiState: StateFlow<TechnicalChatUiState> = _uiState.asStateFlow()

    /** Evita crear la minuta más de una vez si la finalización se procesa de nuevo. */
    private var minutaPersisted = false

    init {
        if (validEvaluationId == null) {
            _uiState.update {
                it.copy(errorMessage = "No se pudo iniciar la evaluación: falta el ID.")
            }
        } else {
            viewModelScope.launch {
                val saved = chatProgressStore.load(evaluationId)
                if (saved != null) {
                    minutaPersisted = saved.minutaId != null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = saved.messages,
                            node = saved.node,
                            flowState = saved.state,
                            answeredQuestions = saved.answeredQuestions,
                            totalQuestions = saved.totalQuestions,
                            progressPercent = saved.progressPercent,
                            answers = saved.answers,
                            completed = saved.completed,
                            proposal = saved.proposal,
                            minutaId = saved.minutaId,
                            pending = saved.pending,
                            evidences = saved.evidences
                        )
                    }
                } else {
                    startChat()
                }
            }
        }
    }

    /** Guarda el progreso actual para poder retomarlo si se sale del chat o
     *  se mata el proceso antes de terminar la propuesta. */
    private fun persistProgress() {
        if (validEvaluationId == null) return
        val state = _uiState.value
        viewModelScope.launch {
            localFiles.saveEvidenceIndex(evaluationId, state.evidences)
            chatProgressStore.save(
                evaluationId,
                ChatProgressSnapshot(
                    messages = state.messages,
                    state = state.flowState,
                    node = state.node,
                    answeredQuestions = state.answeredQuestions,
                    totalQuestions = state.totalQuestions,
                    progressPercent = state.progressPercent,
                    answers = state.answers,
                    completed = state.completed,
                    proposal = state.proposal,
                    minutaId = state.minutaId,
                    pending = state.pending,
                    evidences = state.evidences
                )
            )
        }
    }

    private fun startChat() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            startTechnicalChatUseCase(evaluationId)
                .onSuccess { response ->
                    val first = botMessage(
                        id = 0L,
                        progress = response,
                        evidences = emptyList(),
                        greeting = "¡Hola! Soy tu Asistente de Red con IA.\n\n"
                    )
                    response.state?.let { localFiles.saveState(evaluationId, 0L, it) }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = listOfNotNull(first),
                            node = response.node,
                            flowState = response.state,
                            answeredQuestions = response.answeredQuestions,
                            totalQuestions = response.totalQuestions,
                            progressPercent = response.progressPercent,
                            answers = response.answers,
                            completed = response.completed,
                            proposal = response.proposal,
                            errorMessage = null
                        )
                    }
                    persistProgress()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "No se pudo iniciar el chat"
                        )
                    }
                }
        }
    }

    /**
     * Mensaje del bot para el nodo que pide la respuesta (pregunta, evidencia,
     * aviso o resumen). Null si el flujo terminó.
     */
    private fun botMessage(
        id: Long,
        progress: TechnicalChatProgress,
        evidences: List<ChatEvidenceItem>,
        greeting: String = ""
    ): ChatMessage? {
        val node = progress.node ?: return null
        return when (node.kind) {
            "evidence" -> ChatMessage(
                text = "", isFromUser = false, id = id, prompt = node,
                card = EvidencePresentation.promptCard(node)
            )

            "alert" -> ChatMessage(
                text = "", isFromUser = false, id = id, prompt = node,
                card = ChatCard(kind = "ALERT", title = "Aviso del asistente", text = node.text)
            )

            "summary" -> ChatMessage(
                text = "", isFromUser = false, id = id, prompt = node,
                card = summaryCard(node, evidences)
            )

            else -> ChatMessage(text = greeting + node.text, isFromUser = false, id = id, prompt = node)
        }
    }

    private fun summaryCard(node: ChatNodePrompt, evidences: List<ChatEvidenceItem>): ChatCard {
        val ready = evidences.count { it.status != "missing" }
        val missing = evidences.filter { it.status == "missing" }
        val observed = evidences.filter { it.status == "warn" }
        val warnings = buildList {
            if (missing.isNotEmpty()) {
                add("Falta: ${missing.joinToString(", ") { it.title }}. Quedan como pendientes en la minuta.")
            }
            if (observed.isNotEmpty()) {
                add("Con observaciones: ${observed.joinToString(", ") { it.title }}.")
            }
        }
        return ChatCard(
            kind = "SUMMARY",
            title = "Evidencias de la visita",
            badge = "$ready de ${evidences.size} listas",
            text = node.text,
            thumbs = evidences.map {
                ChatThumb(
                    label = EvidencePresentation.shortLabel(it),
                    path = it.paths.firstOrNull(),
                    status = it.status.takeIf { s -> s == "missing" } ?: if (it.status == "warn") "warn" else "ok"
                )
            },
            warnings = warnings,
            actions = buildList {
                add(ChatAction("Ver todas", "VIEW_ALL"))
                val unresolved = evidences.count { it.status != "ok" }
                if (unresolved > 0) add(ChatAction("Resolver $unresolved", "RESOLVE", "warn"))
                add(ChatAction("Continuar al diagnóstico →", "CONTINUE_DIAGNOSIS", "primary", row = 1))
            }
        )
    }

    /** Texto de la respuesta del técnico tal como se muestra en su burbuja. */
    fun sendAnswer(value: String, display: String = value) {
        val cleanValue = value.trim()
        val state = _uiState.value
        val node = state.node

        if (
            node == null ||
            (cleanValue.isBlank() && node.required && node.inputType != TechnicalChatInputType.ALERT) ||
            state.isLoading ||
            state.isSending ||
            state.completed ||
            state.pending != null ||
            validEvaluationId == null
        ) {
            return
        }
        val flowState = state.flowState ?: return

        val userMessage = ChatMessage(
            display.trim().ifBlank { "—" },
            true,
            id = state.messages.size.toLong(),
            isEditable = node.inputType !in setOf(
                TechnicalChatInputType.ALERT, TechnicalChatInputType.SUMMARY
            )
        )
        val messagesWithUserAnswer = state.messages + userMessage

        _uiState.update {
            it.copy(isSending = true, messages = messagesWithUserAnswer, errorMessage = null)
        }

        viewModelScope.launch {
            answerTechnicalChatUseCase(
                evaluationId = evaluationId,
                state = flowState,
                answer = cleanValue
            ).onSuccess { response ->
                if (response.validationError != null) {
                    // La respuesta no era válida: el nodo no avanzó. Se quita la burbuja del
                    // técnico y el asistente explica qué corregir, como un mensaje más del chat.
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            messages = state.messages + ChatMessage(
                                "⚠ ${response.validationError}", false, id = state.messages.size.toLong()
                            )
                        )
                    }
                    return@onSuccess
                }
                applyResponse(messagesWithUserAnswer, response)
                if (response.completed && cleanValue == "GENERAR_MAPA_IA") generateMap()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        messages = state.messages,
                        errorMessage = error.message ?: "No se pudo enviar la respuesta"
                    )
                }
            }
        }
    }

    /** Agrega el siguiente mensaje del bot (o el cierre) y adopta el estado de la respuesta. */
    private suspend fun applyResponse(base: List<ChatMessage>, response: TechnicalChatProgress) {
        val messages = base.toMutableList()
        val evidences = _uiState.value.evidences

        if (response.completed) {
            messages.add(
                ChatMessage(
                    "Evaluación completada. He procesado tus respuestas y generado la minuta técnica.",
                    false,
                    id = messages.size.toLong()
                )
            )
            response.proposal?.let { proposal ->
                messages.add(
                    ChatMessage("", false, id = messages.size.toLong(), card = diagnosisCard(proposal))
                )
            }
        } else {
            botMessage(messages.size.toLong(), response, evidences)?.let { message ->
                messages.add(message)
                response.state?.let { localFiles.saveState(evaluationId, message.id, it) }
            }
        }

        _uiState.update {
            it.copy(
                isSending = false,
                messages = messages,
                node = response.node,
                flowState = response.state,
                answeredQuestions = response.answeredQuestions,
                totalQuestions = response.totalQuestions,
                progressPercent = response.progressPercent,
                answers = response.answers,
                completed = response.completed,
                proposal = response.proposal,
                pending = null,
                errorMessage = null
            )
        }
        persistProgress()

        if (response.completed) {
            response.proposal?.let { persistMinuta(it) }
        }
    }

    // ---- evidencias ------------------------------------------------------------
    /** Sube 1..N fotos para el nodo EVIDENCE actual. */
    fun sendPhotos(uris: List<Uri>) {
        val state = _uiState.value
        val node = state.node
        if (
            node == null || node.inputType != TechnicalChatInputType.EVIDENCE ||
            uris.isEmpty() || state.isLoading || state.isSending || state.completed ||
            state.pending != null || validEvaluationId == null
        ) {
            return
        }
        val flowState = state.flowState ?: return
        val code = node.evidenceCode.orEmpty()

        _uiState.update { it.copy(isSending = true, errorMessage = null) }

        viewModelScope.launch {
            val files: List<File> = try {
                localFiles.savePhotos(evaluationId, uris.take(node.maxFiles))
            } catch (e: PhotoProcessingException) {
                _uiState.update { it.copy(isSending = false, errorMessage = e.message) }
                return@launch
            }

            val photoMessage = ChatMessage(
                text = EvidencePresentation.title(code).lowercase().replace(' ', '_') + ".jpg",
                isFromUser = true,
                id = state.messages.size.toLong(),
                imagePaths = files.map { it.absolutePath }
            )
            val withPhoto = state.messages + photoMessage
            _uiState.update { it.copy(messages = withPhoto) }

            answerWithPhotosUseCase(evaluationId, flowState, files)
                .onSuccess { response ->
                    if (response.validationError != null) {
                        _uiState.update {
                            it.copy(isSending = false, messages = state.messages, errorMessage = response.validationError)
                        }
                        return@onSuccess
                    }
                    // E3: la miniatura local pasa a ser la versión con credenciales ocultas.
                    localFiles.replaceWithProcessed(files, response.processedImages)
                    val evidence = response.lastEvidence
                    val resultCard = evidence?.let { EvidencePresentation.resultCard(it, response.crossChecks) }
                    val messages = withPhoto.toMutableList()
                    resultCard?.let {
                        messages.add(ChatMessage(text = "", isFromUser = false, id = messages.size.toLong(), card = it))
                    }
                    response.followUps.firstOrNull()?.let {
                        messages.add(ChatMessage(text = it.text, isFromUser = false, id = messages.size.toLong()))
                    }
                    val item = evidence?.let {
                        ChatEvidenceItem(
                            code = it.code,
                            scope = it.scope,
                            title = EvidencePresentation.title(it.code) +
                                (it.area ?: it.equipo?.substringAfter("– ")?.trim())?.takeIf { a -> a.isNotBlank() }?.let { a -> " · $a" }.orEmpty(),
                            section = EvidencePresentation.section(it.code),
                            summary = EvidencePresentation.summary(it),
                            paths = files.map { f -> f.absolutePath },
                            status = if (response.crossChecks.isNotEmpty()) "warn" else "ok",
                            messageId = photoMessage.id
                        )
                    }
                    // Hasta que el técnico confirme ("Sí, es correcto") no se muestra la siguiente pregunta.
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            messages = messages,
                            pending = response.copy(processedImages = emptyList()),
                            evidences = it.evidences + listOfNotNull(item),
                            errorMessage = null
                        )
                    }
                    persistProgress()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            messages = state.messages,
                            errorMessage = error.message ?: "No se pudo enviar la foto"
                        )
                    }
                }
        }
    }

    /** "Omitir por ahora · la pediré al final": la evidencia queda pendiente en la minuta. */
    fun skipEvidence() {
        val state = _uiState.value
        val node = state.node ?: return
        if (node.inputType != TechnicalChatInputType.EVIDENCE) return
        val code = node.evidenceCode.orEmpty()
        val flowState = state.flowState ?: return
        if (state.isSending || state.isLoading || state.pending != null) return

        val userMessage = ChatMessage("Omitir por ahora", true, id = state.messages.size.toLong())
        val withUser = state.messages + userMessage
        _uiState.update { it.copy(isSending = true, messages = withUser, errorMessage = null) }

        viewModelScope.launch {
            answerTechnicalChatUseCase(evaluationId, flowState, "OMITIR")
                .onSuccess { response ->
                    if (response.validationError != null) {
                        _uiState.update {
                            it.copy(isSending = false, messages = state.messages, errorMessage = response.validationError)
                        }
                        return@onSuccess
                    }
                    val missing = ChatEvidenceItem(
                        code = code, scope = node.scope,
                        title = EvidencePresentation.title(code),
                        section = EvidencePresentation.section(code),
                        summary = "Falta esta evidencia", paths = emptyList(),
                        status = "missing", messageId = userMessage.id
                    )
                    _uiState.update { it.copy(evidences = it.evidences + missing) }
                    applyResponse(withUser, response)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSending = false, messages = state.messages, errorMessage = error.message)
                    }
                }
        }
    }

    /** Responde la pregunta de confirmación del asistente (ej. "¿cuál es el proveedor correcto?"). */
    fun answerFollowUp(option: String) {
        val state = _uiState.value
        val pending = state.pending ?: return
        val followUp = pending.followUps.firstOrNull() ?: return
        val flowState = pending.state ?: return
        if (state.isSending) return

        val withUser = state.messages + ChatMessage(option, true, id = state.messages.size.toLong())
        _uiState.update { it.copy(isSending = true, messages = withUser, errorMessage = null) }

        viewModelScope.launch {
            amendUseCase(
                evaluationId = evaluationId,
                state = flowState,
                clarificationKey = followUp.key,
                clarificationAnswer = option
            ).onSuccess { response -> applyResponse(withUser, response) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSending = false, messages = state.messages, errorMessage = error.message)
                    }
                }
        }
    }

    /** "Corregir": el técnico ajusta lo que leyó la IA y se recalculan los avisos. */
    fun correctEvidence(fields: Map<String, Any?>) {
        val state = _uiState.value
        val pending = state.pending ?: return
        val evidence = pending.lastEvidence ?: return
        val flowState = pending.state ?: return
        if (state.isSending || fields.isEmpty()) return

        _uiState.update { it.copy(isSending = true, errorMessage = null) }
        viewModelScope.launch {
            amendUseCase(
                evaluationId = evaluationId,
                state = flowState,
                evidenceCode = evidence.code,
                evidenceScope = evidence.scope,
                fields = fields
            ).onSuccess { response ->
                val updated = response.lastEvidence ?: evidence
                val cardIndex = state.messages.indexOfLast { it.card?.kind == "RESULT" }
                val messages = state.messages.take(if (cardIndex >= 0) cardIndex else state.messages.size).toMutableList()
                messages.add(
                    ChatMessage(
                        text = "", isFromUser = false, id = messages.size.toLong(),
                        card = EvidencePresentation.resultCard(updated, response.crossChecks)
                    )
                )
                response.followUps.firstOrNull()?.let {
                    messages.add(ChatMessage(text = it.text, isFromUser = false, id = messages.size.toLong()))
                }
                _uiState.update { ui ->
                    ui.copy(
                        isSending = false,
                        messages = messages,
                        pending = response.copy(processedImages = emptyList()),
                        evidences = ui.evidences.map { e ->
                            if (e.code == updated.code && e.scope == updated.scope) {
                                e.copy(
                                    summary = EvidencePresentation.summary(updated),
                                    status = if (response.crossChecks.isNotEmpty()) "warn" else "ok"
                                )
                            } else e
                        }
                    )
                }
                persistProgress()
            }.onFailure { error ->
                _uiState.update { it.copy(isSending = false, errorMessage = error.message ?: "No se pudo corregir") }
            }
        }
    }

    /** "Sí, es correcto": muestra por fin la siguiente pregunta. */
    fun confirmEvidence() {
        val state = _uiState.value
        val pending = state.pending ?: return
        viewModelScope.launch { applyResponse(state.messages, pending) }
    }

    /**
     * "Repetir" desde la galería: vuelve al nodo de esa evidencia (omitida o con
     * observaciones) para subirla de nuevo. Las respuestas posteriores se descartan.
     */
    fun retakeEvidence(messageId: Long) {
        val state = _uiState.value
        if (state.isSending || state.isLoading) return
        val index = state.messages.indexOfFirst { it.id == messageId }
        if (index <= 0) return
        reopen(index)
    }

    /** "Cambiar foto": vuelve al nodo de la evidencia con el estado de antes de subirla. */
    fun changeEvidencePhoto() {
        val state = _uiState.value
        if (state.pending == null) return
        val photoIndex = state.messages.indexOfLast { it.isFromUser && it.imagePaths.isNotEmpty() }
        if (photoIndex <= 0) return
        reopen(photoIndex)
    }

    /**
     * "Editar respuesta": el motor es sin estado, así que reabrir una pregunta
     * anterior es restaurar el `state` que se guardó cuando se planteó (ver
     * [ChatLocalFiles]) y dejar que el técnico la responda de nuevo — las
     * respuestas dadas después se descartan.
     */
    fun editAnswer(messageId: Long) {
        val state = _uiState.value
        if (state.isSending || state.isLoading) return
        val index = state.messages.indexOfFirst { it.id == messageId }
        if (index <= 0) return
        reopen(index)
    }

    /** Restaura la pregunta anterior al mensaje de usuario en `userIndex` y descarta lo que le siguió. */
    private fun reopen(userIndex: Int) {
        val state = _uiState.value
        // La pregunta es el último mensaje del bot con nodo antes de la respuesta
        // (puede haber avisos de validación entre ambos).
        val questionIndex = (userIndex - 1 downTo 0).firstOrNull { state.messages[it].prompt != null } ?: return
        val question = state.messages[questionIndex]
        val prompt = question.prompt ?: return

        viewModelScope.launch {
            val savedState = localFiles.loadState(evaluationId, question.id) ?: return@launch
            localFiles.dropStatesFrom(evaluationId, question.id + 1)
            _uiState.update {
                it.copy(
                    messages = state.messages.subList(0, questionIndex + 1),
                    node = prompt,
                    flowState = savedState,
                    completed = false,
                    proposal = null,
                    minutaId = null,
                    pending = null,
                    evidences = state.evidences.filter { e -> e.messageId < questionIndex + 1 },
                    errorMessage = null
                )
            }
            minutaPersisted = false
            persistProgress()
        }
    }

    // ---- diagnóstico y mapa con IA -----------------------------------------------------------
    private fun diagnosisCard(proposal: TechnicalChatProposal): ChatCard {
        val situation = proposal.asIsFindings.take(3)
        val blocks = listOfNotNull(
            situation.takeIf { it.isNotEmpty() }?.let { ChatBlock("Situación actual:", it) },
            proposal.recommendations.take(2).takeIf { it.isNotEmpty() }?.let { ChatBlock("Propuesta:", listOf(it.joinToString(" ")), inline = true) }
        )
        return ChatCard(
            kind = "DIAGNOSIS",
            title = "Diagnóstico listo",
            badge = proposal.score?.let { "$it/100" },
            blocks = blocks,
            actions = listOf(
                ChatAction("✦ Generar mapa con IA", "GENERATE_MAP", "primary", row = 0),
                ChatAction("Ver propuesta completa", "VIEW_PROPOSAL", "outline", row = 1)
            )
        )
    }

    private fun mapCard(map: NetworkMap): ChatCard = ChatCard(
        kind = "MAP",
        title = "Topología propuesta",
        badge = "+ IA",
        map = map,
        text = map.summary(),
        actions = listOf(
            ChatAction("Editar", "EDIT_MAP", "outline", row = 0, icon = R.drawable.ic_edit),
            ChatAction("Regenerar", "REGENERATE_MAP", "outline", row = 0, icon = R.drawable.ic_refresh),
            ChatAction("Ver análisis completo →", "VIEW_ANALYSIS", "primary", row = 1)
        )
    )

    private fun progressCard(done: Int, answers: Int, evidences: Int): ChatCard {
        val labels = listOf(
            "Leyendo $answers respuestas y $evidences evidencias",
            "Ubicando los equipos del escáner de IP",
            "Validando conexiones y puertos",
            "Dibujando el mapa"
        )
        return ChatCard(
            kind = "PROGRESS",
            title = "Armando la topología",
            steps = labels.mapIndexed { i, label ->
                ChatStep(label, when { i < done -> "done"; i == done -> "active"; else -> "pending" })
            }
        )
    }

    private fun replaceMessage(id: Long, transform: (ChatMessage) -> ChatMessage) {
        _uiState.update { s -> s.copy(messages = s.messages.map { if (it.id == id) transform(it) else it }) }
    }

    /** "Generar mapa con IA" / "Regenerar": arma el mapa a partir del chat con una animación de pasos. */
    fun generateMap() {
        val state = _uiState.value
        if (!state.completed || state.isSending || validEvaluationId == null) return

        val user = ChatMessage("Generar mapa con IA", true, id = state.messages.size.toLong())
        val progressId = user.id + 1
        val answers = state.answeredQuestions
        val photos = state.evidences.count { it.status != "missing" }
        val progress = ChatMessage("", false, id = progressId, card = progressCard(0, answers, photos))
        _uiState.update { it.copy(isSending = true, messages = it.messages + user + progress, errorMessage = null) }

        viewModelScope.launch {
            val result = async { generateMapUseCase(evaluationId) }
            for (done in 1..3) {
                delay(900)
                replaceMessage(progressId) { it.copy(card = progressCard(done, answers, photos)) }
            }
            result.await()
                .onSuccess { map ->
                    replaceMessage(progressId) { it.copy(card = mapCard(map)) }
                    _uiState.update { it.copy(isSending = false, currentMap = map) }
                }
                .onFailure { error ->
                    replaceMessage(progressId) { it.copy(card = null, text = "No pude armar el mapa: ${error.message ?: "error del servidor"}") }
                    _uiState.update { it.copy(isSending = false) }
                }
        }
    }

    /** Recarga el mapa guardado (al volver del editor) y actualiza su tarjeta. */
    fun refreshMapAfterEdit() {
        if (validEvaluationId == null) return
        viewModelScope.launch {
            getMapUseCase(evaluationId).onSuccess { map ->
                if (map == null) return@onSuccess
                _uiState.update { s ->
                    s.copy(
                        currentMap = map,
                        messages = s.messages.map { m -> if (m.card?.kind == "MAP") m.copy(card = mapCard(map)) else m }
                    )
                }
            }
        }
    }

    /** Texto libre tras completar la evaluación: "Pide un cambio al mapa…". */
    fun sendFreeText(text: String) {
        val clean = text.trim()
        val state = _uiState.value
        if (clean.isBlank() || !state.completed || state.isSending || validEvaluationId == null) return

        val user = ChatMessage(clean, true, id = state.messages.size.toLong())
        val map = state.currentMap
        if (map == null) {
            _uiState.update {
                it.copy(
                    messages = it.messages + user + ChatMessage(
                        "Primero genera el mapa con «Generar mapa con IA» y después puedes pedirme cambios.",
                        false, id = user.id + 1
                    )
                )
            }
            return
        }
        _uiState.update { it.copy(isSending = true, messages = it.messages + user) }
        viewModelScope.launch {
            mapCommandUseCase(evaluationId, map, clean)
                .onSuccess { (newMap, reply) ->
                    saveMapUseCase(evaluationId, newMap)
                    _uiState.update { s ->
                        s.copy(
                            isSending = false,
                            currentMap = newMap,
                            messages = s.messages.map { m -> if (m.card?.kind == "MAP") m.copy(card = mapCard(newMap)) else m } +
                                ChatMessage(reply, false, id = s.messages.size.toLong())
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSending = false, errorMessage = error.message ?: "No se pudo aplicar el cambio") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Crea la minuta (BORRADOR) en el gateway al completarse el chat, para que
     * quede visible en el listado de minutas de cualquier técnico/supervisor —
     * incluso si el técnico actual no continúa hasta la propuesta final. Es una
     * operación en segundo plano: si falla, no interrumpe el flujo del chat (el
     * usuario ya tiene su propuesta).
     */
    private fun persistMinuta(proposal: TechnicalChatProposal) {
        if (minutaPersisted) return
        minutaPersisted = true

        viewModelScope.launch {
            val evaluation = evaluationRepository
                .getEvaluation(evaluationId.toString())
                .getOrNull()

            val topologyJson = proposal.topology?.let {
                moshi.adapter(ChatTopology::class.java).toJson(it)
            }

            val contentJson = moshi.adapter(MinutaContentPayload::class.java).toJson(
                MinutaContentPayload(
                    equipment = proposal.equipment,
                    asIsFindings = proposal.asIsFindings,
                    recommendations = proposal.recommendations,
                    score = proposal.score
                )
            )

            createMinutaUseCase(
                evaluationId = evaluationId,
                clientName = evaluation?.establishmentName ?: "Evaluación $evaluationId",
                address = evaluation?.establishmentAddress,
                summary = proposal.summary,
                topologyJson = topologyJson,
                contentJson = contentJson
            ).onSuccess { minuta ->
                _uiState.update { it.copy(minutaId = minuta.id) }
                // La propuesta ya quedó guardada como minuta: desde aquí en
                // adelante la pantalla de Propuesta Técnica es la fuente de
                // verdad, así que ya no hace falta poder retomar el chat.
                chatProgressStore.clear(evaluationId)
            }
            // Si falla, se deja minutaId=null a propósito: el técnico puede
            // completar la evaluación de todas formas; el progreso sigue
            // guardado localmente por si se reintenta luego.
        }
    }
}
