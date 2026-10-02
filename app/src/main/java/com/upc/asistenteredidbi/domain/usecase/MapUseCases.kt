package com.upc.asistenteredidbi.domain.usecase

import com.upc.asistenteredidbi.domain.model.NetworkMap
import com.upc.asistenteredidbi.domain.repository.MapRepository
import javax.inject.Inject

class GetMapUseCase @Inject constructor(private val repository: MapRepository) {
    suspend operator fun invoke(evaluationId: Long) = repository.getMap(evaluationId)
}

class SaveMapUseCase @Inject constructor(private val repository: MapRepository) {
    suspend operator fun invoke(evaluationId: Long, map: NetworkMap) = repository.saveMap(evaluationId, map)
}

class GenerateMapUseCase @Inject constructor(private val repository: MapRepository) {
    suspend operator fun invoke(evaluationId: Long) = repository.generateMap(evaluationId)
}

class MapCommandUseCase @Inject constructor(private val repository: MapRepository) {
    suspend operator fun invoke(evaluationId: Long, map: NetworkMap, command: String) =
        repository.command(evaluationId, map, command)
}
