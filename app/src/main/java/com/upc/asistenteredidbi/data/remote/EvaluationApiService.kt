package com.upc.asistenteredidbi.data.remote

import com.upc.asistenteredidbi.data.remote.dto.AnalysisResponseDto
import com.upc.asistenteredidbi.data.remote.dto.AnalyzeAnswersRequestDto
import com.upc.asistenteredidbi.data.remote.dto.EvaluationDto
import com.upc.asistenteredidbi.data.remote.dto.EvaluationListItemDto
import com.upc.asistenteredidbi.data.remote.dto.StartEvaluationRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EvaluationApiService {

    @POST("api/evaluations")
    suspend fun startEvaluation(@Body request: StartEvaluationRequestDto): EvaluationDto

    /** Ruta muerta (api/v1) — el listado de Historial usa datos mock hoy, ver HistorialFragment. */
    @GET("api/v1/evaluations")
    suspend fun listEvaluations(
        @Query("client_name") clientName: String? = null,
        @Query("status") status: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): List<EvaluationListItemDto>

    @GET("api/evaluations/{evaluationId}")
    suspend fun getEvaluation(@Path("evaluationId") evaluationId: String): EvaluationDto

    @POST("api/evaluations/{evaluationId}/analysis")
    suspend fun analyzeEvaluation(
        @Path("evaluationId") evaluationId: Long,
        @Body request: AnalyzeAnswersRequestDto
    ): AnalysisResponseDto

}
