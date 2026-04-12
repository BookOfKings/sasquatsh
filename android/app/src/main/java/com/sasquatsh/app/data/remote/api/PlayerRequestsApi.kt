package com.sasquatsh.app.data.remote.api

import com.sasquatsh.app.data.remote.dto.PlayerRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PlayerRequestsApi {

    @GET("player-requests")
    suspend fun getAll(): Response<List<PlayerRequestDto>>

    @GET("player-requests?type=mine")
    suspend fun getMine(): Response<List<PlayerRequestDto>>

    @POST("player-requests")
    suspend fun create(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<PlayerRequestDto>

    @DELETE("player-requests")
    suspend fun delete(@Query("id") id: String): Response<Unit>
}
