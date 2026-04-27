package com.sasquatsh.app.services.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ProfileApi {

    // GET profile
    @GET("profile")
    suspend fun getMyProfile(): Response<Any>

    // GET profile?id=...
    @GET("profile")
    suspend fun getPublicProfile(
        @Query("id") userId: String
    ): Response<Any>

    // PUT profile
    @PUT("profile")
    suspend fun updateProfile(
        @Body input: Any
    ): Response<Any>

    // POST profile?action=block&userId=...
    @POST("profile")
    suspend fun blockUser(
        @Query("action") action: String = "block",
        @Query("userId") userId: String
    ): Response<Any>

    // POST profile?action=unblock&userId=...
    @POST("profile")
    suspend fun unblockUser(
        @Query("action") action: String = "unblock",
        @Query("userId") userId: String
    ): Response<Any>

    // GET check-username?username=...
    @GET("check-username")
    suspend fun checkUsername(
        @Query("username") username: String
    ): Response<Any>

    // POST profile?action=upload-avatar (multipart)
    @Multipart
    @POST("profile")
    suspend fun uploadAvatar(
        @Query("action") action: String = "upload-avatar",
        @Part avatar: MultipartBody.Part
    ): Response<Any>

    // POST profile?action=delete-avatar
    @POST("profile")
    suspend fun deleteAvatar(
        @Query("action") action: String = "delete-avatar"
    ): Response<Any>

    // DELETE profile?action=delete-account
    @DELETE("profile")
    suspend fun deleteAccount(
        @Query("action") action: String = "delete-account"
    ): Response<Unit>

    // GET profile?action=search&q=...
    @GET("profile")
    suspend fun searchUsers(
        @Query("action") action: String = "search",
        @Query("q") query: String
    ): Response<List<Any>>
}
