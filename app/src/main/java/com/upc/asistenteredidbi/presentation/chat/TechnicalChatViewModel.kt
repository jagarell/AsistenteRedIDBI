package com.upc.asistenteredidbi.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
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
    val answeredQuestions: Int = 0,
    val totalQuestions: Int = 0,
    val progressPercent: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val completed: Boolean = false,
    val proposal: TechnicalChatProposal? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class TechnicalChatViewModel @Inject constructor(
    private val startTechnicalChatUseCase: StartTechnicalChatUseCase,
    private val answerTechnicalChatUseCase: AnswerTechnicalChatUseCase,
    private val evaluationRepository: EvaluationRepository,
    private val createMinutaUseCase: CreateMinutaUseCase,
    private val moshi: Moshi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val evaluationId: Long =
        savedStateHandle.get<String>("evaluationId")
            ?.toLongOrNull()
            ?: 1L

    private val _uiState =
        MutableStateFlow(TechnicalChatUiState())

    val uiState: StateFlow<TechnicalChatUiState> =
        _uiState.asStateFlow()

    /** Evita crear la minuta más de una vez si la finalización se procesa de nuevo. */
    private var minutaPersisted = false

    init {
        startChat()
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
                        false
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = listOf(firstMessage),
                            currentStep = response.currentStep,
                            currentInputType = response.currentInputType,
                            currentOptions = response.currentOptions,
                            answeredQuestions = response.answeredQuestions,
                            totalQuestions = response.totalQuestions,
                            progressPercent = response.progressPercent,
                            answers = response.answers,
                            completed = response.completed,
                            proposal = response.proposal,
                            errorMessage = null
                        )
                    }
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
            state.completed
        ) {
            return
        }

        val messagesWithUserAnswer =
            state.messages + ChatMessage(cleanAnswer, true)

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
                            false
                        )
                    )
                } else {
                    response.currentQuestion
                        ?.takeIf { it.isNotBlank() }
                        ?.let { question ->
                            updatedMessages.add(
                                ChatMessage(
                                    question,
                                    false
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
                        answeredQuestions = response.answeredQuestions,
                        totalQuestions = response.totalQuestions,
                        progressPercent = response.progressPercent,
                        answers = response.answers,
                        completed = response.completed,
                        proposal = response.proposal,
                        errorMessage = null
                    )
                }

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
            )
            // Silencioso a propósito: si falla, el técnico puede completar la
            // evaluación de todas formas; no hay nada que el usuario deba
            // resolver aquí (no hay reintento automático en esta primera versión).
        }
    }
}