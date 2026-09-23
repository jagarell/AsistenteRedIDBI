package com.upc.asistenteredidbi.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.upc.asistenteredidbi.data.session.ChatProgressSnapshot
import com.upc.asistenteredidbi.data.session.ChatProgressStore
import com.upc.asistenteredidbi.domain.model.ChatTopology
import com.upc.asistenteredidbi.domain.model.MinutaContentPayload
import com.upc.asistenteredidbi.domain.model.TechnicalChatInputType
import com.upc.asistenteredidbi.domain.model.TechnicalChatProposal
import com.upc.asistenteredidbi.domain.repository.EvaluationRepository
import com.upc.asistenteredidbi.domain.usecase.AnswerTechnicalChatUseCase
import com.upc.asistenteredidbi.domain.usecase.CreateMinutaUseCase
import com.upc.asistenteredidbi.domain.usecase.StartTechnicalChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TechnicalChatUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val currentStep: Int = 0,
    val currentInputType: TechnicalChatInputType = TechnicalChatInputType.TEXT,
    val currentOptions: List<String> = emptyList(),
    val currentUnit: String? = null,
    val answeredQuestions: Int = 0,
    val totalQuestions: Int = 0,
    val progressPercent: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val completed: Boolean = false,
    val proposal: TechnicalChatProposal? = null,
    val minutaId: Long? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class TechnicalChatViewModel @Inject constructor(
    private val startTechnicalChatUseCase: StartTechnicalChatUseCase,
    private val answerTechnicalChatUseCase: AnswerTechnicalChatUseCase,
    private val evaluationRepository: EvaluationRepository,
    private val createMinutaUseCase: CreateMinutaUseCase,
    private val chatProgressStore: ChatProgressStore,
    private val moshi: Moshi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Null si el argumento de navegación no trae un evaluationId real y parseable —
     *  en vez de asumir la evaluación 1, se falla explícito (ver [init]). */
    private val validEvaluationId: Long? =
        savedStateHandle.get<String>("evaluationId")?.toLongOrNull()

    val evaluationId: Long = validEvaluationId ?: -1L

    private val _uiState =
        MutableStateFlow(TechnicalChatUiState())

    val uiState: StateFlow<TechnicalChatUiState> =
        _uiState.asStateFlow()

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
                            currentStep = saved.currentStep,
                            currentInputType = saved.currentInputType,
                            currentOptions = saved.currentOptions,
                            currentUnit = saved.currentUnit,
                            answeredQuestions = saved.answeredQuestions,
                            totalQuestions = saved.totalQuestions,
                            progressPercent = saved.progressPercent,
                            answers = saved.answers,
                            completed = saved.completed,
                            proposal = saved.proposal,
                            minutaId = saved.minutaId
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
            chatProgressStore.save(
                evaluationId,
                ChatProgressSnapshot(
                    messages = state.messages,
                    currentStep = state.currentStep,
                    currentInputType = state.currentInputType,
                    currentOptions = state.currentOptions,
                    currentUnit = state.currentUnit,
                    answeredQuestions = state.answeredQuestions,
                    totalQuestions = state.totalQuestions,
                    progressPercent = state.progressPercent,
                    answers = state.answers,
                    completed = state.completed,
                    proposal = state.proposal,
                    minutaId = state.minutaId
                )
            )
        }
    }

    private fun startChat() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            startTechnicalChatUseCase(evaluationId)
                .onSuccess { response ->
                    val question = response.currentQuestion.orEmpty()

                    val firstMessage = ChatMessage(
                        "¡Hola! Soy tu Asistente de Red con IA.\n\n$question",
                        false,
                        id = 0L,
                        stepIndex = response.currentStep,
                        inputType = response.currentInputType,
                        options = response.currentOptions,
                        unit = response.currentUnit,
                        answersSnapshot = response.answers
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = listOf(firstMessage),
                            currentStep = response.currentStep,
                            currentInputType = response.currentInputType,
                            currentOptions = response.currentOptions,
                            currentUnit = response.currentUnit,
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
                            errorMessage = error.message
                                ?: "No se pudo iniciar el chat"
                        )
                    }
                }
        }
    }

    fun sendAnswer(answer: String) {
        val cleanAnswer = answer.trim()
        val state = _uiState.value

        if (
            cleanAnswer.isBlank() ||
            state.isLoading ||
            state.isSending ||
            state.completed ||
            validEvaluationId == null
        ) {
            return
        }

        val messagesWithUserAnswer =
            state.messages + ChatMessage(
                cleanAnswer,
                true,
                id = state.messages.size.toLong(),
                isEditable = true
            )

        _uiState.update {
            it.copy(
                isSending = true,
                messages = messagesWithUserAnswer,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            answerTechnicalChatUseCase(
                evaluationId = evaluationId,
                currentStep = state.currentStep,
                answer = cleanAnswer,
                answers = state.answers
            ).onSuccess { response ->

                val updatedMessages =
                    messagesWithUserAnswer.toMutableList()

                if (response.completed) {
                    updatedMessages.add(
                        ChatMessage(
                            "Evaluación completada. He procesado tus respuestas y generado una propuesta técnica preliminar.",
                            false,
                            id = updatedMessages.size.toLong()
                        )
                    )
                } else {
                    response.currentQuestion
                        ?.takeIf { it.isNotBlank() }
                        ?.let { question ->
                            updatedMessages.add(
                                ChatMessage(
                                    question,
                                    false,
                                    id = updatedMessages.size.toLong(),
                                    stepIndex = response.currentStep,
                                    inputType = response.currentInputType,
                                    options = response.currentOptions,
                                    unit = response.currentUnit,
                                    answersSnapshot = response.answers
                                )
                            )
                        }
                }

                _uiState.update {
                    it.copy(
                        isSending = false,
                        messages = updatedMessages,
                        currentStep = response.currentStep,
                        currentInputType = response.currentInputType,
                        currentOptions = response.currentOptions,
                        currentUnit = response.currentUnit,
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

                if (response.completed) {
                    response.proposal?.let { persistMinuta(it) }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = error.message
                            ?: "No se pudo enviar la respuesta"
                    )
                }
            }
        }
    }

    /**
     * "Editar respuesta": el motor de chat es sin estado (recibe el mapa
     * completo de respuestas + currentStep y calcula la siguiente pregunta),
     * así que reabrir una pregunta anterior es truncar localmente hasta esa
     * pregunta y dejar que el técnico la responda de nuevo por el mismo
     * flujo de [sendAnswer] — las respuestas dadas después se descartan.
     */
    fun editAnswer(messageId: Long) {
        val state = _uiState.value
        if (state.isSending || state.isLoading) return

        val index = state.messages.indexOfFirst { it.id == messageId }
        if (index <= 0) return

        val question = state.messages[index - 1]
        val step = question.stepIndex
        val inputType = question.inputType
        if (step == null || inputType == null) return

        _uiState.update {
            it.copy(
                messages = state.messages.subList(0, index),
                currentStep = step,
                currentInputType = inputType,
                currentOptions = question.options,
                currentUnit = question.unit,
                answers = question.answersSnapshot,
                completed = false,
                proposal = null,
                minutaId = null,
                errorMessage = null
            )
        }
        minutaPersisted = false
        persistProgress()
    }

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    /**
     * Crea la minuta (BORRADOR) en el gateway al completarse el chat de 20
     * nodos, para que quede visible en el listado de minutas de cualquier
     * técnico/supervisor — incluso si el técnico actual no continúa hasta la
     * propuesta final. Es una operación en segundo plano: si falla, no
     * interrumpe el flujo del chat (el usuario ya tiene su propuesta).
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
            // completar la evaluación de todas formas; no hay reintento
            // automático en esta primera versión (la minuta simplemente no
            // quedará disponible para editar/validar más adelante). El
            // progreso sigue guardado localmente por si se reintenta luego.
        }
    }
}