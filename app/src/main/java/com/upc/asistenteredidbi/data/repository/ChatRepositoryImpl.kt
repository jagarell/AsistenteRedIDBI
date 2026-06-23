package com.upc.asistenteredidbi.data.repository

import android.content.Context
import android.net.Uri
import com.upc.asistenteredidbi.data.mapper.toDomain
import com.upc.asistenteredidbi.data.remote.ChatApiService
import com.upc.asistenteredidbi.data.remote.dto.ConfirmClosingSummaryRequestDto
import com.upc.asistenteredidbi.data.remote.dto.SaveChatResponseRequestDto
import com.upc.asistenteredidbi.data.util.MultipartUtils
import com.upc.asistenteredidbi.domain.model.ChatClosingSummary
import com.upc.asistenteredidbi.domain.model.ChatProgress
import com.upc.asistenteredidbi.domain.model.ChatResponseAnswer
import com.upc.asistenteredidbi.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApiService,
    @ApplicationContext private val context: Context
) : ChatRepository {

    override suspend fun getChatProgress(evaluationId: String): Result<ChatProgress> = safeCall {
        api.getChatProgress(evaluationId).toDomain()
    }

    override suspend fun saveChatResponse(evaluationId: String, nodeKey: String, value: String): Result<ChatProgress> = safeCall {
        api.saveChatResponse(evaluationId, SaveChatResponseRequestDto(nodeKey, value)).toDomain()
    }

    override suspend fun saveChatPhotoResponse(evaluationId: String, nodeKey: String, imageUri: Uri): Result<ChatProgress> = safeCall {
        val tempFile = MultipartUtils.uriToTempFile(context, imageUri, prefix = "chat_$nodeKey")
        val result = api.saveChatPhotoResponse(evaluationId, MultipartUtils.textPart(nodeKey), MultipartUtils.filePart(tempFile))
        tempFile.delete()
        result.toDomain()
    }

    override suspend fun listChatResponses(evaluationId: String): Result<List<ChatResponseAnswer>> = safeCall {
        api.listChatResponses(evaluationId).map { it.toDomain() }
    }

    override suspend fun getClosingSummary(evaluationId: String): Result<ChatClosingSummary> = safeCall {
        api.getClosingSummary(evaluationId).toDomain()
    }

    override suspend fun confirmClosingSummary(evaluationId: String, additionalNotes: String?): Result<ChatProgress> = safeCall {
        api.confirmClosingSummary(evaluationId, ConfirmClosingSummaryRequestDto(additionalNotes)).toDomain()
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
