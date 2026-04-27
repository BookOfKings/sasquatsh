package com.sasquatsh.app.services

import com.sasquatsh.app.models.Badge
import com.sasquatsh.app.models.BadgesResponse
import com.sasquatsh.app.models.UserBadge
import com.sasquatsh.app.models.PinResponse
import com.sasquatsh.app.services.api.BadgesApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgesService @Inject constructor(
    private val badgesApi: BadgesApi,
    private val moshi: Moshi
) {
    private val badgeListType = Types.newParameterizedType(List::class.java, Badge::class.java)

    suspend fun getAllBadges(): List<Badge> {
        val response = badgesApi.getAllBadges()
        if (!response.isSuccessful) throw Exception("Failed to load badges")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<Badge>>(badgeListType).fromJson(json) ?: emptyList()
    }

    suspend fun getUserBadges(userId: String): List<UserBadge> {
        val response = badgesApi.getUserBadges(userId = userId)
        if (!response.isSuccessful) throw Exception("Failed to load user badges")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        val badgesResponse = moshi.adapter(BadgesResponse::class.java).fromJson(json)
            ?: BadgesResponse(badges = emptyList())
        return badgesResponse.badges
    }

    suspend fun getMyBadges(): BadgesResponse {
        val response = badgesApi.getMyBadges()
        if (!response.isSuccessful) throw Exception("Failed to load earned badges")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(BadgesResponse::class.java).fromJson(json)
            ?: BadgesResponse(badges = emptyList())
    }

    suspend fun computeBadges(): BadgesResponse {
        val response = badgesApi.computeBadges()
        if (!response.isSuccessful) throw Exception("Failed to compute badges")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(BadgesResponse::class.java).fromJson(json)
            ?: BadgesResponse(badges = emptyList())
    }

    suspend fun togglePin(badgeId: Int): PinResponse {
        val response = badgesApi.togglePin(badgeId = badgeId)
        if (!response.isSuccessful) throw Exception("Failed to toggle pin")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(PinResponse::class.java).fromJson(json)
            ?: PinResponse(pinned = false)
    }
}
