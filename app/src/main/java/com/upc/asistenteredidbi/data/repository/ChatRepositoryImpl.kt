package com.upc.asistenteredidbi.data.repository

import java.io.File
import com.upc.asistenteredidbi.data.mapper.toDomain
import com.upc.asistenteredidbi.data.remote.ChatApiService
import com.upc.asistenteredidbi.data.remote.dto.ChatAmendRequestDto
import com.upc.asistenteredidbi.data.remote.dto.ChatEditRequestDto
import com.upc.asistenteredidbi.data.remote.dto.TechnicalChatAnswerRequestDto
import com.upc.asistenteredidbi.data.remote.toFriendlyMessage
import com.upc.asistenteredidbi.data.util.MultipartUtils
import com.upc.asistenteredidbi.domain.model.TechnicalChatProgress
import com.upc.asistenteredidbi.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApiService
) : ChatRepository {

    override suspend fun startTechnicalChat(
        evaluationId: Long
    ): Result<TechnicalChatProgress> = safeCall {
        api.startTechnicalChat(evaluationId)
            .toDomain()
    }

    override suspend fun answerTechnicalChat(
        evaluationId: Long,
        state: String,
        answer: String
    ): Result<TechnicalChatProgress> = safeCall {
        api.answerTechnicalChat(
            evaluationId = evaluationId,
            request = TechnicalChatAnswerRequestDto(
                evaluationId = evaluationId.toString(),
                state = state,
                answer = answer
            )
        ).toDomain()
    }

    override suspend fun amendTechnicalChat(
        evaluationId: Long,
        state: String,
        evidenceCode: String?,
        evidenceScope: String?,
        fields: Map<String, Any?>,
        clarificationKey: String?,
        clarificationAnswer: String?,
        nodeId: String?,
        deleted: List<String>?,
        restore: Boolean?
    ): Result<TechnicalChatProgress> = safeCall {
        api.amendTechnicalChat(
            evaluationId,
            ChatAmendRequestDto(
                evaluationId = evaluationId.toString(),
                state = state,
                evidenceCode = evidenceCode,
                evidenceScope = evidenceScope,
                fields = fields,
                clarificationKey = clarificationKey,
                clarificationAnswer = clarificationAnswer,
                nodeId = nodeId,
                deleted = deleted,
                restore = restore
            )
        ).toDomain()
    }

    override suspend fun answerTechnicalChatWithPhotos(
        evaluationId: Long,
        state: String,
        photos: List<File>,
        mode: String
    ): Result<TechnicalChatProgress> = safeCall {
        api.answerTechnicalChatWithPhotos(
            evaluationId = evaluationId,
            state = MultipartUtils.textPart(state),
            mode = MultipartUtils.textPart(mode),
            files = photos.map { MultipartUtils.filePart(it, partName = "files") }
        ).toDomain()
    }

    override suspend fun editTechnicalChat(
        evaluationId: Long,
        state: String,
        nodeId: String,
        mode: String
    ): Result<TechnicalChatProgress> = safeCall {
        api.editTechnicalChat(evaluationId, ChatEditRequestDto(evaluationId.toString(), state, nodeId, mode)).toDomain()
    }

    private suspend fun <T> safeCall(
        block: suspend () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (exception: Exception) {
            Result.failure(Exception(exception.toFriendlyMessage(), exception))
        }
    }
}