package com.upc.asistenteredidbi.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.upc.asistenteredidbi.domain.model.ChatNodePrompt
import com.upc.asistenteredidbi.domain.model.TechnicalChatProgress
import com.upc.asistenteredidbi.domain.model.TechnicalChatProposal
import com.upc.asistenteredidbi.presentation.chat.ChatEvidenceItem
import com.upc.asistenteredidbi.presentation.chat.ChatMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.chatProgressDataStore by preferencesDataStore(name = "technical_chat_progress")

/** Todo lo necesario para redibujar el chat exactamente donde se dejó. */
data class ChatProgressSnapshot(
    val messages: List<ChatMessage>,
    /** Estado opaco del flujo (ver app/chat/flow_engine.py) tal como lo devolvió el motor. */
    val state: String?,
    /** Nodo que toca responder ahora; null si el flujo ya terminó. */
    val node: ChatNodePrompt?,
    val answeredQuestions: Int,
    val totalQuestions: Int,
    val progressPercent: Int,
    val answers: Map<String, String>,
    val completed: Boolean,
    val proposal: TechnicalChatProposal?,
    val minutaId: Long?,
    /** Respuesta del motor a una evidencia que el técnico aún no confirmó ("Sí, es correcto"). */
    val pending: TechnicalChatProgress? = null,
    /** Evidencias subidas u omitidas hasta ahora (resumen y galería). */
    val evidences: List<ChatEvidenceItem> = emptyList()
)

/**
 * Guarda el progreso del chat técnico (flujo de nodos) localmente en el dispositivo
 * mientras la propuesta no termina de generarse. El motor de FastAPI/gateway
 * es sin estado (`/chat/start` siempre arranca en el nodo 0), así que si no
 * se guarda nada aquí, salir del fragmento o matar el proceso reinicia la
 * evaluación desde cero. Se limpia al completar el chat y persistir la
 * minuta — desde ahí la pantalla de Propuesta Técnica toma el control.
 */
@Singleton
class ChatProgressStore @Inject constructor(
    @ApplicationContext private val context: Context,
    moshi: Moshi
) {
    private val adapter = moshi.adapter(ChatProgressSnapshot::class.java)

    suspend fun load(evaluationId: Long): ChatProgressSnapshot? {
        val json = context.chatProgressDataStore.data.first()[keyFor(evaluationId)] ?: return null
        return runCatching { adapter.fromJson(json) }.getOrNull()
    }

    suspend fun save(evaluationId: Long, snapshot: ChatProgressSnapshot) {
        context.chatProgressDataStore.edit {
            it[keyFor(evaluationId)] = adapter.toJson(snapshot)
            it[LAST_EVALUATION_KEY] = evaluationId.toString()
        }
    }

    suspend fun clear(evaluationId: Long) {
        context.chatProgressDataStore.edit {
            it.remove(keyFor(evaluationId))
            if (it[LAST_EVALUATION_KEY] == evaluationId.toString()) it.remove(LAST_EVALUATION_KEY)
        }
    }

    /**
     * Evaluación cuyo chat se puede retomar con "Continuar": la última con progreso
     * guardado. Solo "Nueva Evaluación" la descarta (ver [clear]).
     */
    suspend fun resumableEvaluationId(): Long? {
        val id = context.chatProgressDataStore.data.first()[LAST_EVALUATION_KEY]?.toLongOrNull() ?: return null
        return id.takeIf { load(it) != null }
    }

    private fun keyFor(evaluationId: Long) = stringPreferencesKey("chat_progress_$evaluationId")

    private companion object {
        val LAST_EVALUATION_KEY = stringPreferencesKey("last_evaluation_id")
    }
}
