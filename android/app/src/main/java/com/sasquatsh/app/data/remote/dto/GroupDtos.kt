package com.sasquatsh.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GroupSummaryDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "groupType") val groupType: String?,
    @Json(name = "joinPolicy") val joinPolicy: String?,
    @Json(name = "memberCount") val memberCount: Int?,
    @Json(name = "locationCity") val locationCity: String?,
    @Json(name = "locationState") val locationState: String?,
    @Json(name = "logoUrl") val logoUrl: String?,
)

@JsonClass(generateAdapter = true)
data class GroupDetailDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "groupType") val groupType: String?,
    @Json(name = "joinPolicy") val joinPolicy: String?,
    @Json(name = "memberCount") val memberCount: Int?,
    @Json(name = "locationCity") val locationCity: String?,
    @Json(name = "locationState") val locationState: String?,
    @Json(name = "locationRadiusMiles") val locationRadiusMiles: Int?,
    @Json(name = "logoUrl") val logoUrl: String?,
    @Json(name = "creator") val creator: UserSummaryDto?,
    @Json(name = "members") val members: List<GroupMemberDto>?,
    @Json(name = "userMembership") val userMembership: GroupMemberDto?,
)

@JsonClass(generateAdapter = true)
data class GroupMemberDto(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String?,
    @Json(name = "role") val role: String,
    @Json(name = "displayName") val displayName: String?,
    @Json(name = "username") val username: String?,
    @Json(name = "avatarUrl") val avatarUrl: String?,
    @Json(name = "joinedAt") val joinedAt: String?,
)
