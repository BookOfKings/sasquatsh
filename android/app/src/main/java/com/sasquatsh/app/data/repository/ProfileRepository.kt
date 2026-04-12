package com.sasquatsh.app.data.repository

import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.api.ProfileApi
import com.sasquatsh.app.data.remote.dto.ProfileDto
import com.sasquatsh.app.data.remote.dto.UpdateProfileRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileApi: ProfileApi,
) {

    suspend fun getProfile(): ApiResult<ProfileDto> {
        return try {
            val response = profileApi.getProfile()
            if (response.isSuccessful) {
                val body = response.body() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(body)
            } else {
                ApiResult.Error("Failed to load profile: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load profile")
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequest): ApiResult<ProfileDto> {
        return try {
            val response = profileApi.updateProfile(request)
            if (response.isSuccessful) {
                val body = response.body() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(body)
            } else {
                ApiResult.Error("Failed to update profile: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to update profile")
        }
    }
}
