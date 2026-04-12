package com.sasquatsh.app.data.remote.api

import com.sasquatsh.app.data.remote.dto.PlanningSessionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PlanningApi {

    @GET("planning")
    suspend fun getGroupPlanningSessions(@Query("groupId") groupId: String): Response<List<PlanningSessionDto>>

    @GET("planning")
    suspend fun getPlanningSession(@Query("id") sessionId: String): Response<PlanningSessionDto>

    @POST("planning")
    suspend fun createPlanningSession(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<PlanningSessionDto>

    @POST("planning")
    suspend fun respondToPlanningSession(
        @Query("id") sessionId: String,
        @Query("action") action: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<PlanningSessionDto>
}
