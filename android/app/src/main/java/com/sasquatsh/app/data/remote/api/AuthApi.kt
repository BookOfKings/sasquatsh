package com.sasquatsh.app.data.remote.api

import com.sasquatsh.app.data.remote.dto.AuthSyncRequest
import com.sasquatsh.app.data.remote.dto.AuthSyncResponse
import com.sasquatsh.app.data.remote.dto.CheckUsernameResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {

    @POST("auth-sync")
    suspend fun syncAuth(@Body body: AuthSyncRequest): Response<AuthSyncResponse>

    @GET("check-username")
    suspend fun checkUsername(@Query("username") username: String): Response<CheckUsernameResponse>
}
