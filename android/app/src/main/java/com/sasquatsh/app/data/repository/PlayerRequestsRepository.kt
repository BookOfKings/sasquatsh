package com.sasquatsh.app.data.repository

import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.api.PlayerRequestsApi
import com.sasquatsh.app.data.remote.dto.PlayerRequestDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRequestsRepository @Inject constructor(
    private val playerRequestsApi: PlayerRequestsApi,
) {

    suspend fun getAll(): ApiResult<List<PlayerRequestDto>> {
        return try {
            val response = playerRequestsApi.getAll()
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Failed to load requests: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load requests")
        }
    }

    suspend fun getMine(): ApiResult<List<PlayerRequestDto>> {
        return try {
            val response = playerRequestsApi.getMine()
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Failed to load your requests: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load your requests")
        }
    }

    suspend fun create(body: Map<String, Any?>): ApiResult<PlayerRequestDto> {
        return try {
            val response = playerRequestsApi.create(body)
            if (response.isSuccessful) {
                val data = response.body() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(data)
            } else {
                ApiResult.Error("Failed to create request: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to create request")
        }
    }

    suspend fun delete(id: String): ApiResult<Unit> {
        return try {
            val response = playerRequestsApi.delete(id)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error("Failed to delete request: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to delete request")
        }
    }
}
