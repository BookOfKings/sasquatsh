package com.sasquatsh.app.services.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface GroupsApi {

    // GET groups (+ filter params)
    @GET("groups")
    suspend fun getPublicGroups(
        @Query("search") search: String? = null,
        @Query("type") groupType: String? = null,
        @Query("city") city: String? = null,
        @Query("state") state: String? = null
    ): Response<List<Any>>

    // GET groups?mine=true
    @GET("groups")
    suspend fun getMyGroups(
        @Query("mine") mine: String = "true"
    ): Response<List<Any>>

    // GET groups?id=...
    @GET("groups")
    suspend fun getGroup(
        @Query("id") id: String
    ): Response<Any>

    // GET groups?slug=...
    @GET("groups")
    suspend fun getGroupBySlug(
        @Query("slug") slug: String
    ): Response<Any>

    // POST groups
    @POST("groups")
    suspend fun createGroup(
        @Body input: Any
    ): Response<Any>

    // PUT groups?id=...
    @PUT("groups")
    suspend fun updateGroup(
        @Query("id") id: String,
        @Body input: Any
    ): Response<Any>

    // DELETE groups?id=...
    @DELETE("groups")
    suspend fun deleteGroup(
        @Query("id") id: String
    ): Response<Unit>

    // POST groups?id=...&action=join
    @POST("groups")
    suspend fun joinGroup(
        @Query("id") id: String,
        @Query("action") action: String = "join"
    ): Response<Unit>

    // DELETE groups?id=...&action=leave
    @DELETE("groups")
    suspend fun leaveGroup(
        @Query("id") id: String,
        @Query("action") action: String = "leave"
    ): Response<Unit>

    // POST groups?id=...&action=request
    @POST("groups")
    suspend fun requestToJoin(
        @Query("id") groupId: String,
        @Query("action") action: String = "request",
        @Body body: Map<String, String>? = null
    ): Response<Unit>

    // GET groups?id=...&include=members
    @GET("groups")
    suspend fun getMembers(
        @Query("id") groupId: String,
        @Query("include") include: String = "members"
    ): Response<List<Any>>

    // DELETE groups?id=...&action=remove&userId=...
    @DELETE("groups")
    suspend fun removeMember(
        @Query("id") groupId: String,
        @Query("action") action: String = "remove",
        @Query("userId") userId: String
    ): Response<Unit>

    // PUT groups?id=...&action=role&userId=...
    @PUT("groups")
    suspend fun changeRole(
        @Query("id") groupId: String,
        @Query("action") action: String = "role",
        @Query("userId") userId: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    // PUT groups?id=...&action=transfer&userId=...
    @PUT("groups")
    suspend fun transferOwnership(
        @Query("id") groupId: String,
        @Query("action") action: String = "transfer",
        @Query("userId") newOwnerId: String
    ): Response<Unit>

    // GET groups?id=...&include=requests
    @GET("groups")
    suspend fun getJoinRequests(
        @Query("id") groupId: String,
        @Query("include") include: String = "requests"
    ): Response<List<Any>>

    // POST groups?id=...&action=approve&userId=...
    @POST("groups")
    suspend fun approveRequest(
        @Query("id") groupId: String,
        @Query("action") action: String = "approve",
        @Query("userId") userId: String
    ): Response<Unit>

    // POST groups?id=...&action=reject&userId=...
    @POST("groups")
    suspend fun rejectRequest(
        @Query("id") groupId: String,
        @Query("action") action: String = "reject",
        @Query("userId") userId: String
    ): Response<Unit>

    // POST groups?id=...&action=invite
    @POST("groups")
    suspend fun createInvitation(
        @Query("id") groupId: String,
        @Query("action") action: String = "invite",
        @Body input: Any? = null
    ): Response<Any>

    // GET groups?id=...&include=invitations
    @GET("groups")
    suspend fun getInvitations(
        @Query("id") groupId: String,
        @Query("include") include: String = "invitations"
    ): Response<List<Any>>

    // DELETE groups?id=...&action=revoke-invite&inviteId=...
    @DELETE("groups")
    suspend fun revokeInvitation(
        @Query("id") groupId: String,
        @Query("action") action: String = "revoke-invite",
        @Query("inviteId") inviteId: String
    ): Response<Unit>

    // GET groups?action=my-invitations
    @GET("groups")
    suspend fun getMyPendingInvitations(
        @Query("action") action: String = "my-invitations"
    ): Response<List<Any>>

    // POST groups?action=respond-invite&inviteId=...
    @POST("groups")
    suspend fun respondToInvitation(
        @Query("action") action: String = "respond-invite",
        @Query("inviteId") invitationId: String,
        @Body body: Map<String, String>
    ): Response<Any>

    // GET groups?action=preview-invite&code=...
    @GET("groups")
    suspend fun previewInvite(
        @Query("action") action: String = "preview-invite",
        @Query("code") code: String
    ): Response<Any>

    // POST groups?action=accept-invite&code=...
    @POST("groups")
    suspend fun acceptInvite(
        @Query("action") action: String = "accept-invite",
        @Query("code") code: String
    ): Response<Any>

    // POST groups?id=...&action=upload-logo (multipart)
    @Multipart
    @POST("groups")
    suspend fun uploadLogo(
        @Query("id") groupId: String,
        @Query("action") action: String = "upload-logo",
        @Part logo: MultipartBody.Part
    ): Response<Any>
}
