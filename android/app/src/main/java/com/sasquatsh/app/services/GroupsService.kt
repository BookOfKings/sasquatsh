package com.sasquatsh.app.services

import com.sasquatsh.app.models.*
import com.sasquatsh.app.services.api.GroupsApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupsService @Inject constructor(
    private val groupsApi: GroupsApi,
    private val moshi: Moshi
) {

    private val groupSummaryListType = Types.newParameterizedType(List::class.java, GroupSummary::class.java)
    private val groupMemberListType = Types.newParameterizedType(List::class.java, GroupMember::class.java)
    private val joinRequestListType = Types.newParameterizedType(List::class.java, JoinRequest::class.java)
    private val groupInvitationListType = Types.newParameterizedType(List::class.java, GroupInvitation::class.java)
    private val pendingInvitationListType = Types.newParameterizedType(List::class.java, PendingGroupInvitation::class.java)

    suspend fun getPublicGroups(filter: GroupSearchFilter): List<GroupSummary> {
        val response = groupsApi.getPublicGroups(
            search = filter.search,
            groupType = filter.groupType?.value,
            city = filter.city,
            state = filter.state
        )
        if (!response.isSuccessful) throw Exception("Failed to load groups")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<GroupSummary>>(groupSummaryListType).fromJson(json) ?: emptyList()
    }

    suspend fun getMyGroups(): List<GroupSummary> {
        val response = groupsApi.getMyGroups()
        if (!response.isSuccessful) throw Exception("Failed to load my groups")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<GroupSummary>>(groupSummaryListType).fromJson(json) ?: emptyList()
    }

    suspend fun getGroup(id: String): GameGroup {
        val response = groupsApi.getGroup(id)
        if (!response.isSuccessful) throw Exception("Failed to load group")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(GameGroup::class.java).fromJson(json)
            ?: throw Exception("Failed to parse group")
    }

    suspend fun createGroup(input: CreateGroupInput): GameGroup {
        val response = groupsApi.createGroup(input)
        if (!response.isSuccessful) throw Exception("Failed to create group")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(GameGroup::class.java).fromJson(json)
            ?: throw Exception("Failed to parse group")
    }

    suspend fun updateGroup(id: String, input: UpdateGroupInput): GameGroup {
        val response = groupsApi.updateGroup(id, input)
        if (!response.isSuccessful) throw Exception("Failed to update group")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(GameGroup::class.java).fromJson(json)
            ?: throw Exception("Failed to parse group")
    }

    suspend fun deleteGroup(id: String) {
        val response = groupsApi.deleteGroup(id)
        if (!response.isSuccessful) throw Exception("Failed to delete group")
    }

    suspend fun joinGroup(id: String) {
        val response = groupsApi.joinGroup(id)
        if (!response.isSuccessful) throw Exception("Failed to join group")
    }

    suspend fun leaveGroup(id: String) {
        val response = groupsApi.leaveGroup(id)
        if (!response.isSuccessful) throw Exception("Failed to leave group")
    }

    suspend fun requestToJoin(id: String, message: String?) {
        val body = if (message != null) mapOf("message" to message) else null
        val response = groupsApi.requestToJoin(groupId = id, body = body)
        if (!response.isSuccessful) throw Exception("Failed to send join request")
    }

    suspend fun getMembers(groupId: String): List<GroupMember> {
        val response = groupsApi.getMembers(groupId)
        if (!response.isSuccessful) throw Exception("Failed to load members")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<GroupMember>>(groupMemberListType).fromJson(json) ?: emptyList()
    }

    suspend fun removeMember(groupId: String, userId: String) {
        val response = groupsApi.removeMember(groupId = groupId, userId = userId)
        if (!response.isSuccessful) throw Exception("Failed to remove member")
    }

    suspend fun changeRole(groupId: String, userId: String, role: MemberRole) {
        val body = mapOf("role" to role.value)
        val response = groupsApi.changeRole(groupId = groupId, userId = userId, body = body)
        if (!response.isSuccessful) throw Exception("Failed to change role")
    }

    suspend fun transferOwnership(groupId: String, userId: String) {
        val response = groupsApi.transferOwnership(groupId = groupId, newOwnerId = userId)
        if (!response.isSuccessful) throw Exception("Failed to transfer ownership")
    }

    suspend fun getJoinRequests(groupId: String): List<JoinRequest> {
        val response = groupsApi.getJoinRequests(groupId)
        if (!response.isSuccessful) throw Exception("Failed to load join requests")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<JoinRequest>>(joinRequestListType).fromJson(json) ?: emptyList()
    }

    suspend fun approveRequest(groupId: String, userId: String) {
        val response = groupsApi.approveRequest(groupId = groupId, userId = userId)
        if (!response.isSuccessful) throw Exception("Failed to approve request")
    }

    suspend fun rejectRequest(groupId: String, userId: String) {
        val response = groupsApi.rejectRequest(groupId = groupId, userId = userId)
        if (!response.isSuccessful) throw Exception("Failed to reject request")
    }

    suspend fun createInvitation(groupId: String, input: CreateInvitationInput?): GroupInvitation {
        val response = groupsApi.createInvitation(groupId = groupId, input = input)
        if (!response.isSuccessful) throw Exception("Failed to create invitation")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(GroupInvitation::class.java).fromJson(json)
            ?: throw Exception("Failed to parse invitation")
    }

    suspend fun getInvitations(groupId: String): List<GroupInvitation> {
        val response = groupsApi.getInvitations(groupId)
        if (!response.isSuccessful) throw Exception("Failed to load invitations")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<GroupInvitation>>(groupInvitationListType).fromJson(json) ?: emptyList()
    }

    suspend fun revokeInvitation(groupId: String, inviteId: String) {
        val response = groupsApi.revokeInvitation(groupId = groupId, inviteId = inviteId)
        if (!response.isSuccessful) throw Exception("Failed to revoke invitation")
    }

    suspend fun getMyPendingInvitations(): List<PendingGroupInvitation> {
        val response = groupsApi.getMyPendingInvitations()
        if (!response.isSuccessful) throw Exception("Failed to load invitations")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<PendingGroupInvitation>>(pendingInvitationListType).fromJson(json) ?: emptyList()
    }

    suspend fun respondToInvitation(invitationId: String, accept: Boolean) {
        val body = mapOf("response" to if (accept) "accept" else "decline")
        val response = groupsApi.respondToInvitation(invitationId = invitationId, body = body)
        if (!response.isSuccessful) throw Exception("Failed to respond to invitation")
    }

    suspend fun uploadLogo(
        groupId: String,
        imageData: ByteArray,
        fileName: String,
        mimeType: String
    ): String {
        val requestBody = imageData.toRequestBody(mimeType.toMediaType())
        val part = MultipartBody.Part.createFormData("logo", fileName, requestBody)
        val response = groupsApi.uploadLogo(groupId = groupId, logo = part)
        if (!response.isSuccessful) throw Exception("Failed to upload logo")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        val map = moshi.adapter(Map::class.java).fromJson(json)
        return map?.get("logoUrl") as? String ?: ""
    }
}
