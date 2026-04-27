package com.sasquatsh.app.services

import com.sasquatsh.app.models.*
import com.sasquatsh.app.services.api.ProfileApi
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileService @Inject constructor(
    private val profileApi: ProfileApi,
    private val moshi: Moshi
) {

    suspend fun getMyProfile(): UserProfile {
        val response = profileApi.getMyProfile()
        if (!response.isSuccessful) throw Exception("Failed to load profile")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(UserProfile::class.java).fromJson(json)
            ?: throw Exception("Failed to parse profile")
    }

    suspend fun updateProfile(input: UpdateProfileInput): UserProfile {
        val response = profileApi.updateProfile(input)
        if (!response.isSuccessful) throw Exception("Failed to update profile")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(UserProfile::class.java).fromJson(json)
            ?: throw Exception("Failed to parse profile")
    }

    suspend fun blockUser(userId: String): BlockActionResponse {
        val response = profileApi.blockUser(userId = userId)
        if (!response.isSuccessful) throw Exception("Failed to block user")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(BlockActionResponse::class.java).fromJson(json)
            ?: throw Exception("Failed to parse response")
    }

    suspend fun unblockUser(userId: String): BlockActionResponse {
        val response = profileApi.unblockUser(userId = userId)
        if (!response.isSuccessful) throw Exception("Failed to unblock user")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(BlockActionResponse::class.java).fromJson(json)
            ?: throw Exception("Failed to parse response")
    }

    suspend fun checkUsername(username: String): UsernameCheckResponse {
        val response = profileApi.checkUsername(username)
        if (!response.isSuccessful) throw Exception("Failed to check username")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(UsernameCheckResponse::class.java).fromJson(json)
            ?: throw Exception("Failed to parse response")
    }

    suspend fun uploadAvatar(
        imageData: ByteArray,
        fileName: String,
        mimeType: String
    ): AvatarUploadResponse {
        val requestBody = imageData.toRequestBody(mimeType.toMediaType())
        val part = MultipartBody.Part.createFormData("avatar", fileName, requestBody)
        val response = profileApi.uploadAvatar(avatar = part)
        if (!response.isSuccessful) throw Exception("Failed to upload avatar")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(AvatarUploadResponse::class.java).fromJson(json)
            ?: throw Exception("Failed to parse response")
    }

    suspend fun deleteAvatar(): AvatarDeleteResponse {
        val response = profileApi.deleteAvatar()
        if (!response.isSuccessful) throw Exception("Failed to delete avatar")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(AvatarDeleteResponse::class.java).fromJson(json)
            ?: throw Exception("Failed to parse response")
    }

    suspend fun deleteAccount() {
        val response = profileApi.deleteAccount()
        if (!response.isSuccessful) throw Exception("Failed to delete account")
    }
}
