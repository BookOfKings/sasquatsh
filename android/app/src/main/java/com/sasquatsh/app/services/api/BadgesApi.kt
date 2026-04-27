package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface BadgesApi {

    // GET badges
    @GET("badges")
    suspend fun getAllBadges(): Response<Any>

    // GET badges?action=my-badges
    @GET("badges")
    suspend fun getMyBadges(
        @Query("action") action: String = "my-badges"
    ): Response<Any>

    // POST badges?action=compute
    @POST("badges")
    suspend fun computeBadges(
        @Query("action") action: String = "compute"
    ): Response<Any>

    // PUT badges?action=pin&badgeId=...
    @PUT("badges")
    suspend fun togglePin(
        @Query("action") action: String = "pin",
        @Query("badgeId") badgeId: Int
    ): Response<Any>
}
