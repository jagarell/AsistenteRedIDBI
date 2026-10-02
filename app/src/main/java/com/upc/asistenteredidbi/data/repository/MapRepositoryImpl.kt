package com.upc.asistenteredidbi.data.repository

import com.upc.asistenteredidbi.data.remote.MapApiService
import com.upc.asistenteredidbi.data.remote.MapCommandRequestDto
import com.upc.asistenteredidbi.data.remote.toFriendlyMessage
import com.upc.asistenteredidbi.domain.model.NetworkMap
import com.upc.asistenteredidbi.domain.repository.MapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MapRepositoryImpl @Inject constructor(
    private val api: MapApiService
) : MapRepository {

    override suspend fun getMap(evaluationId: Long): Result<NetworkMap?> =
        safeCall { api.getMap(evaluationId).map }

    override suspend fun saveMap(evaluationId: Long, map: NetworkMap): Result<NetworkMap> =
        safeCall { api.saveMap(evaluationId, map) }

    override suspend fun generateMap(evaluationId: Long): Result<NetworkMap> =
        safeCall { api.generateMap(evaluationId) }

    override suspend fun command(
        evaluationId: Long,
        map: NetworkMap,
        command: String
    ): Result<Pair<NetworkMap, String>> = safeCall {
        val response = api.mapCommand(evaluationId, MapCommandRequestDto(map, command))
        response.map to response.reply
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(block())
            } catch (exception: Exception) {
                Result.failure(Exception(exception.toFriendlyMessage(), exception))
            }
        }
}
