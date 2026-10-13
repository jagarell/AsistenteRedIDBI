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
import com.upc.asistenteredidbi.domain.usecase.EditTechnicalChatUseCase
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
    val currentMap: NetworkMap? = null,
    /** Lectura que la IA hizo de la foto y el técnico está validando (editable). */
    val reviewDraft: Map<String, Any?> = emptyMap(),
    /** Lecturas eliminadas con la X roja. */
    val reviewDeleted: Set<String> = emptySet(),
    /** Modo edición de una evaluación terminada: estado de antes de los cambios (para descartarlos). */
    val editSnapshot: String? = null,
    val editProposal: TechnicalChatProposal? = null,
    val editMessages: List<ChatMessage> = emptyList(),
    val editAnswers: Map<String, String> = emptyMap()
)

@HiltViewModel
class TechnicalChatViewModel @Inject constructor(
    private val startTechnicalChatUseCase: StartTechnicalChatUseCase,
    private val answerTechnicalChatUseCase: AnswerTechnicalChatUseCase,
    private val answerWithPhotosUseCase: AnswerTechnicalChatWithPhotosUseCase,
    private val amendUseCase: AmendTechnicalChatUseCase,
    private val editUseCase: EditTechnicalChatUseCase,
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

    /** Si se abre desde la propuesta para corregir una lectura, la pregunta que se vuelve a hacer. */
    private val reopenNodeId: String? = savedStateHandle.get<String>("reopenNodeId")

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
                    reopenNodeId?.let { reopenNode(it) }
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
     * Mensaje del bot para el nodo que pide la respuesta (pregunta, evidencia o resumen).
     * Null si el flujo terminó. Las fotos que ya están pidiéndose no repiten la tarjeta.
     */
    private fun botMessage(
        id: Long,
        progress: TechnicalChatProgress,
        evidences: List<ChatEvidenceItem>,
        greeting: String = "",
        previousNodeId: String? = null,
        draft: Map<String, Any?> = emptyMap(),
        deleted: Set<String> = emptySet()
    ): ChatMessage? {
        val node = progress.node ?: return null
        return when {
            node.kind == "evidence" -> {
                val ev = node.evidence
                when (ev?.status) {
                    "REVIEW" -> ChatMessage(
                        text = "", isFromUser = false, id = id, prompt = node,
                        card = EvidencePresentation.readingCard(node, draft, deleted)
                    )
                    "IL", "NC", "WRONG" -> ChatMessage(
                        text = "", isFromUser = false, id = id, prompt = node,
                        card = ChatCard(kind = "ERROR", title = "", text = ev.message)
                    )
                    else -> ChatMessage(
                        text = "", isFromUser = false, id = id, prompt = node,
                        card = EvidencePresentation.promptCard(node)
                    )
                }
            }

            node.kind == "summary" -> ChatMessage(
                text = "", isFromUser = false, id = id, prompt = node,
                card = summaryCard(node, evidences)
            )

            node.inputType == TechnicalChatInputType.READ_CARD -> ChatMessage(
                text = node.text, isFromUser = false, id = id, prompt = node,
                card = EvidencePresentation.routerCard(node, draft)
            )

            else -> ChatMessage(text = greeting + node.text, isFromUser = false, id = id, prompt = node)
        }
    }

    private fun noticeMessages(notices: List<com.upc.asistenteredidbi.domain.model.ChatNotice>, startId: Long): List<ChatMessage> =
        notices.mapIndexed { i, n ->
            ChatMessage(
                text = "", isFromUser = false, id = startId + i,
                card = ChatCard(kind = "NOTICE", title = n.tag, text = n.text, tone = n.tone)
            )
        }

    private fun summaryCard(node: ChatNodePrompt, evidences: List<ChatEvidenceItem>): ChatCard {
        val ready = evidences.count { it.status != "missing" }
        val missing = evidences.filter { it.status == "missing" }
        val observed = evidences.filter { it.status == "warn" }
        val warnings = buildList {
            if (missing.isNotEmpty()) {
                add("Falta: ${missing.joinToString(", ") { it.title }}. Quedan pendientes: súbelas con «Resolver» cuando puedas (no van en el PDF).")
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

    /** Lo que se ve en la burbuja del técnico para una respuesta ya serializada. */
    fun sendAnswer(value: String, display: String = value) {
        val cleanValue = value.trim()
        val state = _uiState.value
        val node = state.node

        if (
            node == null ||
            (cleanValue.isBlank() && node.required && node.inputType != TechnicalChatInputType.ALERT &&
                node.inputType != TechnicalChatInputType.LINKS) ||
            state.isLoading ||
            state.isSending ||
            (state.completed && !state.editing()) ||
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
                TechnicalChatInputType.ALERT, TechnicalChatInputType.SUMMARY, TechnicalChatInputType.EVIDENCE
            ) && cleanValue != "@SKIP" && cleanValue != "@WRONG",
            answerNodeId = node.nodeId
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

    private fun TechnicalChatUiState.editing() = editSnapshot != null

    /** Agrega los avisos y el siguiente mensaje del bot (o el cierre) y adopta el estado de la respuesta. */
    private suspend fun applyResponse(base: List<ChatMessage>, response: TechnicalChatProgress) {
        val messages = base.toMutableList()
        val evidences = _uiState.value.evidences
        val previousNode = _uiState.value.node

        messages += noticeMessages(response.notices, messages.size.toLong())
        var draft: Map<String, Any?> = emptyMap()
        if (response.node?.evidence?.status == "REVIEW") draft = response.node.evidence.data
        if (response.node?.inputType == TechnicalChatInputType.READ_CARD) {
            @Suppress("UNCHECKED_CAST")
            draft = (com.upc.asistenteredidbi.data.mapper.AnyJson.fromJson(response.node.valueJson) as? Map<String, Any?>).orEmpty()
        }

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
            botMessage(messages.size.toLong(), response, evidences, previousNodeId = previousNode?.nodeId, draft = draft)?.let { message ->
                messages.add(message)
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
                reviewDraft = draft,
                reviewDeleted = emptySet(),
                errorMessage = null
            )
        }
        persistProgress()

        if (response.completed) {
            response.proposal?.let { persistMinuta(it) }
        }
    }

    // ---- evidencias ------------------------------------------------------------
    /** Sube 1..N fotos para el nodo EVIDENCE actual. `mode`: tomar (cámara) o subir (galería). */
    fun sendPhotos(uris: List<Uri>, mode: String = "subir") {
        val state = _uiState.value
        val node = state.node
        if (
            node == null || node.inputType != TechnicalChatInputType.EVIDENCE ||
            uris.isEmpty() || state.isLoading || state.isSending ||
            (state.completed && !state.editing()) || validEvaluationId == null
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

            answerWithPhotosUseCase(evaluationId, flowState, files, mode)
                .onSuccess { response ->
                    if (response.validationError != null) {
                        _uiState.update {
                            it.copy(isSending = false, messages = state.messages, errorMessage = response.validationError)
                        }
                        return@onSuccess
                    }
                    // Etiqueta del router: la miniatura local pasa a ser la versión con credenciales ocultas.
                    localFiles.replaceWithProcessed(files, response.processedImages)
                    val evidence = response.lastEvidence
                    val item = evidence?.let {
                        ChatEvidenceItem(
                            code = it.code,
                            scope = it.scope,
                            title = EvidencePresentation.title(it.code) + it.scope.takeIf { s -> s.isNotBlank() }?.let { s -> " · $s" }.orEmpty(),
                            section = EvidencePresentation.section(it.code),
                            summary = EvidencePresentation.summary(it),
                            paths = files.map { f -> f.absolutePath },
                            status = "ok",
                            messageId = photoMessage.id
                        )
                    }
                    // Si ya había una foto de la misma evidencia, esta la reemplaza.
                    _uiState.update { ui ->
                        ui.copy(evidences = ui.evidences.filterNot { e -> item != null && e.code == item.code && e.scope == item.scope } + listOfNotNull(item))
                    }
                    applyResponse(withPhoto, response.copy(processedImages = emptyList()))
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

    /** "Saltar evidencia" / "Continuar sin esta lectura": queda pendiente en la minuta. */
    fun skipEvidence() {
        val state = _uiState.value
        val node = state.node ?: return
        if (node.inputType != TechnicalChatInputType.EVIDENCE || !node.skippable) return
        if (state.isSending || state.isLoading) return
        val wrong = node.evidence?.status == "WRONG"
        val code = node.evidenceCode.orEmpty()
        val missing = ChatEvidenceItem(
            code = code, scope = node.scope,
            title = EvidencePresentation.title(code) + node.scope.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
            section = EvidencePresentation.section(code),
            summary = "Falta esta evidencia", paths = emptyList(),
            status = "missing", messageId = state.messages.size.toLong()
        )
        _uiState.update { it.copy(evidences = it.evidences.filterNot { e -> e.code == missing.code && e.scope == missing.scope } + missing) }
        sendAnswer("@SKIP", if (wrong) "Continuar sin esta lectura" else "Saltar evidencia")
    }

    // ---- lectura de la IA: Correcto / Editar / X roja / No es correcto ---------------
    /** "Correcto": confirma lo que se ve en la tarjeta (con lo editado y sin lo eliminado). */
    fun confirmReading() {
        val state = _uiState.value
        val node = state.node ?: return
        if (node.inputType == TechnicalChatInputType.READ_CARD) {
            sendAnswer(com.upc.asistenteredidbi.data.mapper.AnyJson.toJson(state.reviewDraft), "Correcto")
            return
        }
        if (node.evidence?.status != "REVIEW") return
        val data = linkedMapOf<String, Any?>()
        node.evidence.fields.forEach { f -> if (f.key !in state.reviewDeleted) data[f.key] = state.reviewDraft[f.key] }
        if (state.reviewDeleted.isNotEmpty()) data["_del"] = state.reviewDeleted.toList()
        sendAnswer(com.upc.asistenteredidbi.data.mapper.AnyJson.toJson(data), "Correcto")
    }

    fun wrongReading() = sendAnswer("@WRONG", "No es correcto")

    /** Cambios hechos en "Editar datos". */
    fun updateReading(changes: Map<String, Any?>) {
        val state = _uiState.value
        val node = state.node ?: return
        val draft = state.reviewDraft + changes
        refreshReadingCard(node, draft, state.reviewDeleted)
    }

    /** X roja: la lectura no aparece en el chat, la minuta ni el anexo. */
    fun deleteReading(key: String) {
        val state = _uiState.value
        val node = state.node ?: return
        refreshReadingCard(node, state.reviewDraft, state.reviewDeleted + key)
    }

    fun restoreReadings() {
        val state = _uiState.value
        val node = state.node ?: return
        refreshReadingCard(node, node.evidence?.data.orEmpty() + state.reviewDraft.filterKeys { it !in state.reviewDeleted }, emptySet(), restore = true)
    }

    private fun refreshReadingCard(node: ChatNodePrompt, draft: Map<String, Any?>, deleted: Set<String>, restore: Boolean = false) {
        val effective = if (restore) node.evidence?.data.orEmpty().filterKeys { it in node.evidence?.fields.orEmpty().map { f -> f.key } } + draft else draft
        _uiState.update { s ->
            val index = s.messages.indexOfLast { it.card?.kind == "RESULT" && it.prompt?.nodeId == node.nodeId }
            val card = if (node.inputType == TechnicalChatInputType.READ_CARD) EvidencePresentation.routerCard(node, effective)
            else EvidencePresentation.readingCard(node, effective, deleted)
            s.copy(
                reviewDraft = effective,
                reviewDeleted = deleted,
                messages = if (index < 0) s.messages else s.messages.mapIndexed { i, m ->
                    if (i == index) m.copy(card = card) else m
                }
            )
        }
    }

    /** "Corregir" una lectura ya confirmada (desde la propuesta o el resumen): se guarda en el chat y se recalcula. */
    fun correctEvidence(fields: Map<String, Any?>, nodeId: String, deleted: List<String>? = null, restore: Boolean = false) {
        val state = _uiState.value
        val flowState = state.flowState ?: return
        if (state.isSending) return
        _uiState.update { it.copy(isSending = true, errorMessage = null) }
        viewModelScope.launch {
            amendUseCase(
                evaluationId = evaluationId, state = flowState, fields = fields,
                nodeId = nodeId, deleted = deleted, restore = restore
            ).onSuccess { response ->
                _uiState.update {
                    it.copy(
                        isSending = false, flowState = response.state, node = response.node, completed = response.completed,
                        proposal = response.proposal ?: it.proposal, answers = response.answers
                    )
                }
                persistProgress()
            }.onFailure { error ->
                _uiState.update { it.copy(isSending = false, errorMessage = error.message ?: "No se pudo corregir") }
            }
        }
    }

    /**
     * "Repetir" desde la galería: vuelve a pedir esa evidencia. Con la evaluación terminada solo se vuelve a
     * preguntar esa foto; en curso, se borra lo respondido desde ahí.
     */
    fun retakeEvidence(messageId: Long) {
        val state = _uiState.value
        val item = state.evidences.firstOrNull { it.messageId == messageId } ?: return
        val nodeId = if (item.scope.isBlank()) item.code else "${item.code}#${item.scope}"
        reopenNode(nodeId)
    }

    /** "Editar respuesta": tocar la burbuja del técnico vuelve a esa pregunta. */
    fun editAnswer(messageId: Long) {
        val state = _uiState.value
        if (state.isSending || state.isLoading) return
        val nodeId = state.messages.firstOrNull { it.id == messageId }?.answerNodeId ?: return
        reopenNode(nodeId)
    }

    private fun reopenNode(nodeId: String) {
        val state = _uiState.value
        val flowState = state.flowState ?: return
        if (state.isSending || state.isLoading) return
        val finished = state.completed || state.editSnapshot != null
        _uiState.update { it.copy(isSending = true, errorMessage = null) }
        viewModelScope.launch {
            editUseCase(evaluationId, flowState, nodeId, if (finished) "reopen" else "rewind")
                .onSuccess { response ->
                    if (response.validationError != null) {
                        _uiState.update { it.copy(isSending = false, errorMessage = response.validationError) }
                        return@onSuccess
                    }
                    val node = response.node
                    val messages: List<ChatMessage>
                    if (finished) {
                        // Evaluación terminada: se agrega la pregunta al final y el resto de respuestas se conserva.
                        messages = state.messages
                    } else {
                        // En curso: se vuelve a esa pregunta y se descarta lo que le siguió.
                        val questionIndex = state.messages.indexOfLast { it.prompt?.nodeId == nodeId && !it.isFromUser }
                            .takeIf { it >= 0 } ?: state.messages.size - 1
                        messages = state.messages.subList(0, questionIndex)
                    }
                    val evidences = if (finished) state.evidences else state.evidences.filter { it.messageId < messages.size }
                    val first = finished && state.editSnapshot == null
                    _uiState.update {
                        it.copy(
                            isSending = false, messages = messages, evidences = evidences, flowState = response.state,
                            completed = false, proposal = null, minutaId = if (finished) it.minutaId else null,
                            editSnapshot = state.editSnapshot ?: if (finished) flowState else null,
                            editProposal = if (first) state.proposal else it.editProposal,
                            editMessages = if (first) state.messages else it.editMessages,
                            editAnswers = if (first) state.answers else it.editAnswers,
                            answers = response.answers
                        )
                    }
                    minutaPersisted = finished && minutaPersisted
                    applyResponse(_uiState.value.messages, response.copy(notices = emptyList()))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSending = false, errorMessage = error.message ?: "No se pudo editar") }
                }
        }
    }

    /** Modo edición: descarta los cambios y vuelve a lo que estaba antes. */
    fun discardEdits() {
        val state = _uiState.value
        val snapshot = state.editSnapshot ?: return
        _uiState.update {
            it.copy(
                flowState = snapshot, node = null, completed = true, proposal = it.editProposal, messages = it.editMessages,
                answers = it.editAnswers, editSnapshot = null, editProposal = null, editMessages = emptyList(),
                reviewDraft = emptyMap(), reviewDeleted = emptySet()
            )
        }
        persistProgress()
    }

    fun finishEditing() {
        _uiState.update { it.copy(editSnapshot = null, editProposal = null, editMessages = emptyList()) }
        persistProgress()
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
                // El chat queda guardado aunque la minuta ya exista: "Continuar" lo
                // retoma tal como se dejó (diagnóstico, mapa y cambios incluidos).
                // Solo "Nueva Evaluación" lo descarta.
                persistProgress()
            }
            // Si falla, se deja minutaId=null a propósito: el técnico puede
            // completar la evaluación de todas formas; el progreso sigue
            // guardado localmente por si se reintenta luego.
        }
    }
}
