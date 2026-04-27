package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface InvitationsApi {

    // GET invitations?code=...
    @GET("invitations")
    suspend fun getGameInvitation(
        @Query("code") code: String
    ): Response<Any>

    // POST invitations?code=...&action=accept
    @POST("invitations")
    suspend fun acceptGameInvitation(
        @Query("code") code: String,
        @Query("action") action: String = "accept"
    ): Response<Any>

    // POST invitations
    @POST("invitations")
    suspend fun createGameInvitation(
        @Body input: Any
    ): Response<Any>

    // GET invitations?eventId=...
    @GET("invitations")
    suspend fun getEventInvitations(
        @Query("eventId") eventId: String
    ): Response<List<Any>>

    // DELETE invitations?id=...
    @DELETE("invitations")
    suspend fun revokeGameInvitation(
        @Query("id") id: String
    ): Response<Unit>
}
