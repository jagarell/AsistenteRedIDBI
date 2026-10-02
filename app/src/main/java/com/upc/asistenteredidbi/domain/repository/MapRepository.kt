package com.upc.asistenteredidbi.domain.repository

import com.upc.asistenteredidbi.domain.model.NetworkMap

interface MapRepository {
    /** El mapa guardado de la evaluación, o null si todavía no hay. */
    suspend fun getMap(evaluationId: Long): Result<NetworkMap?>

    suspend fun saveMap(evaluationId: Long, map: NetworkMap): Result<NetworkMap>

    /** Arma el mapa automático a partir del chat y lo guarda. */
    suspend fun generateMap(evaluationId: Long): Result<NetworkMap>

    /** Aplica una orden en lenguaje natural al mapa; devuelve el mapa nuevo y la respuesta del asistente. */
    suspend fun command(evaluationId: Long, map: NetworkMap, command: String): Result<Pair<NetworkMap, String>>
}
