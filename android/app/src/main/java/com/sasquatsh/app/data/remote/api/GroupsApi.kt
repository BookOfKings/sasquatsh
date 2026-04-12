package com.sasquatsh.app.data.remote.api

import com.sasquatsh.app.data.remote.dto.GroupDetailDto
import com.sasquatsh.app.data.remote.dto.GroupSummaryDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface GroupsApi {

    @GET("groups")
    suspend fun getPublicGroups(): Response<List<GroupSummaryDto>>

    @GET("groups?type=mine")
    suspend fun getMyGroups(): Response<List<GroupSummaryDto>>

    @GET("groups")
    suspend fun getGroup(@Query("slug") slug: String): Response<GroupDetailDto>

    @GET("groups")
    suspend fun getGroupById(
        @Query("id") groupId: String,
        @Query("include") include: String = "members",
    ): Response<GroupDetailDto>

    @POST("groups")
    suspend fun createGroup(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<GroupDetailDto>

    @PUT("groups")
    suspend fun updateGroup(
        @Query("id") groupId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Response<GroupDetailDto>

    @POST("groups")
    suspend fun joinGroup(
        @Query("id") groupId: String,
        @Query("action") action: String = "join",
    ): Response<Unit>

    @POST("groups")
    suspend fun requestToJoin(
        @Query("id") groupId: String,
        @Query("action") action: String = "request",
    ): Response<Unit>

    @DELETE("groups")
    suspend fun leaveGroup(
        @Query("id") groupId: String,
        @Query("action") action: String = "leave",
    ): Response<Unit>
}
