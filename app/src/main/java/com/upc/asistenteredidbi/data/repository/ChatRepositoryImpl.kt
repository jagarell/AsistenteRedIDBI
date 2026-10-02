package com.upc.asistenteredidbi.data.repository

import android.content.Context
import android.net.Uri
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.upc.asistenteredidbi.data.mapper.toDomain
import com.upc.asistenteredidbi.data.remote.ChatApiService
import com.upc.asistenteredidbi.data.remote.dto.TechnicalChatAnswerRequestDto
import com.upc.asistenteredidbi.data.remote.toFriendlyMessage
import com.upc.asistenteredidbi.data.util.MultipartUtils
import com.upc.asistenteredidbi.domain.model.TechnicalChatProgress
import com.upc.asistenteredidbi.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApiService,
    private val moshi: Moshi,
    @ApplicationContext private val context: Context
) : ChatRepository {

    override suspend fun startTechnicalChat(
        evaluationId: Long
    ): Result<TechnicalChatProgress> = safeCall {
        api.startTechnicalChat(evaluationId)
            .toDomain()
    }

    override suspend fun answerTechnicalChat(
        evaluationId: Long,
        currentStep: Int,
        answer: String,
        answers: Map<String, String>
    ): Result<TechnicalChatProgress> = safeCall {

        api.answerTechnicalChat(
            evaluationId = evaluationId,
            request = TechnicalChatAnswerRequestDto(
                evaluationId = evaluationId.toString(),
                currentStep = currentStep,
                answer = answer,
                answers = answers
            )
        ).toDomain()
    }

    override suspend fun answerTechnicalChatWithPhoto(
        evaluationId: Long,
        currentStep: Int,
        answers: Map<String, String>,
        photoUri: Uri
    ): Result<TechnicalChatProgress> = safeCall {
        val tempFile = MultipartUtils.uriToTempFile(context, photoUri, prefix = "chat_photo_")
        val answersJson = moshi.adapter<Map<String, String>>(
            Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        ).toJson(answers)

        val result = api.answerTechnicalChatWithPhoto(
            evaluationId = evaluationId,
            currentStep = MultipartUtils.textPart(currentStep.toString()),
            answersJson = MultipartUtils.textPart(answersJson),
            file = MultipartUtils.filePart(tempFile)
        )
        tempFile.delete()
        result.toDomain()
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