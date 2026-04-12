package com.sasquatsh.app.data.remote.api

import com.sasquatsh.app.data.remote.dto.ProfileDto
import com.sasquatsh.app.data.remote.dto.UpdateProfileRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface ProfileApi {

    @GET("profile")
    suspend fun getProfile(): Response<ProfileDto>

    @PUT("profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Response<ProfileDto>
}
