package com.upc.asistenteredidbi.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.upc.asistenteredidbi.domain.model.TechnicalChatInputType
import com.upc.asistenteredidbi.domain.model.TechnicalChatProposal
import com.upc.asistenteredidbi.presentation.chat.ChatMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.chatProgressDataStore by preferencesDataStore(name = "technical_chat_progress")

/** Todo lo necesario para redibujar el chat exactamente donde se dejó. */
data class ChatProgressSnapshot(
    val messages: List<ChatMessage>,
    val currentStep: Int,
    val currentInputType: TechnicalChatInputType,
    val currentOptions: List<String>,
    val currentUnit: String? = null,
    val answeredQuestions: Int,
    val totalQuestions: Int,
    val progressPercent: Int,
    val answers: Map<String, String>,
    val completed: Boolean,
    val proposal: TechnicalChatProposal?,
    val minutaId: Long?
)

/**
 * Guarda el progreso del chat técnico (20 nodos) localmente en el dispositivo
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
        }
    }

    suspend fun clear(evaluationId: Long) {
        context.chatProgressDataStore.edit { it.remove(keyFor(evaluationId)) }
    }

    private fun keyFor(evaluationId: Long) = stringPreferencesKey("chat_progress_$evaluationId")
}
