package com.sasquatsh.app.services

import com.sasquatsh.app.models.GameInvitation
import com.sasquatsh.app.models.InvitationPreview
import com.sasquatsh.app.services.api.GroupsApi
import com.sasquatsh.app.services.api.InvitationsApi
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

data class AcceptResult(
    val message: String,
    val eventId: String? = null,
    val groupId: String? = null
)

@Singleton
class InvitationsService @Inject constructor(
    private val invitationsApi: InvitationsApi,
    private val groupsApi: GroupsApi,
    private val moshi: Moshi
) {
    suspend fun getGameInvitation(code: String): GameInvitation {
        val response = invitationsApi.getGameInvitation(code)
        if (!response.isSuccessful) throw Exception("Failed to load invitation")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(GameInvitation::class.java).fromJson(json)
            ?: throw Exception("Invalid invitation response")
    }

    suspend fun acceptGameInvitation(code: String): AcceptResult {
        val response = invitationsApi.acceptGameInvitation(code)
        if (!response.isSuccessful) throw Exception("Failed to accept invitation")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        val map = moshi.adapter(Map::class.java).fromJson(json)
        return AcceptResult(
            message = map?.get("message") as? String ?: "Invitation accepted",
            eventId = map?.get("eventId") as? String
        )
    }

    suspend fun getGroupInvitePreview(code: String): InvitationPreview {
        val response = groupsApi.previewInvite(code = code)
        if (!response.isSuccessful) throw Exception("Failed to load group invitation")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(InvitationPreview::class.java).fromJson(json)
            ?: throw Exception("Invalid invitation response")
    }

    suspend fun acceptGroupInvite(code: String): AcceptResult {
        val response = groupsApi.acceptInvite(code = code)
        if (!response.isSuccessful) throw Exception("Failed to accept group invitation")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        val map = moshi.adapter(Map::class.java).fromJson(json)
        return AcceptResult(
            message = map?.get("message") as? String ?: "Joined group",
            groupId = map?.get("groupId") as? String
        )
    }
}
