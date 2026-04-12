package com.sasquatsh.app.data.repository

import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.api.GroupsApi
import com.sasquatsh.app.data.remote.dto.GroupDetailDto
import com.sasquatsh.app.data.remote.dto.GroupSummaryDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupsRepository @Inject constructor(
    private val groupsApi: GroupsApi,
) {

    suspend fun getPublicGroups(): ApiResult<List<GroupSummaryDto>> {
        return try {
            val response = groupsApi.getPublicGroups()
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Failed to load groups: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load groups")
        }
    }

    suspend fun getMyGroups(): ApiResult<List<GroupSummaryDto>> {
        return try {
            val response = groupsApi.getMyGroups()
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: emptyList())
            } else {
                ApiResult.Error("Failed to load your groups: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load your groups")
        }
    }

    suspend fun getGroup(slug: String): ApiResult<GroupDetailDto> {
        return try {
            val response = groupsApi.getGroup(slug)
            if (response.isSuccessful) {
                val body = response.body() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(body)
            } else {
                ApiResult.Error("Failed to load group: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load group")
        }
    }

    suspend fun joinGroup(id: String): ApiResult<Unit> {
        return try {
            val response = groupsApi.joinGroup(id)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error("Failed to join group: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to join group")
        }
    }

    suspend fun leaveGroup(id: String): ApiResult<Unit> {
        return try {
            val response = groupsApi.leaveGroup(id)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error("Failed to leave group: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to leave group")
        }
    }

    suspend fun requestToJoin(id: String): ApiResult<Unit> {
        return try {
            val response = groupsApi.requestToJoin(id)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error("Failed to request to join: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to request to join")
        }
    }
}
