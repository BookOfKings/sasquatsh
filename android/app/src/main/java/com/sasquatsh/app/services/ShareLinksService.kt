package com.sasquatsh.app.services

import com.sasquatsh.app.services.api.ShareLinksApi
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

data class ShareLinkPreview(
    val code: String,
    val type: String? = null,
    val targetName: String? = null,
    val createdBy: String? = null,
    val expiresAt: String? = null
)

data class ShareLinkAcceptResult(
    val message: String,
    val eventId: String? = null,
    val groupId: String? = null
)

data class ShareLinkCreateResult(
    val code: String,
    val url: String? = null
)

@Singleton
class ShareLinksService @Inject constructor(
    private val shareLinksApi: ShareLinksApi,
    private val moshi: Moshi
) {

    suspend fun previewLink(code: String): ShareLinkPreview {
        val response = shareLinksApi.preview(code)
        if (!response.isSuccessful) throw Exception("Failed to preview link")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        val map = moshi.adapter(Map::class.java).fromJson(json)
        return ShareLinkPreview(
            code = map?.get("code") as? String ?: code,
            type = map?.get("type") as? String,
            targetName = map?.get("targetName") as? String,
            createdBy = map?.get("createdBy") as? String,
            expiresAt = map?.get("expiresAt") as? String
        )
    }

    suspend fun acceptLink(code: String): ShareLinkAcceptResult {
        val response = shareLinksApi.accept(code)
        if (!response.isSuccessful) throw Exception("Failed to accept link")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        val map = moshi.adapter(Map::class.java).fromJson(json)
        return ShareLinkAcceptResult(
            message = map?.get("message") as? String ?: "Link accepted",
            eventId = map?.get("eventId") as? String,
            groupId = map?.get("groupId") as? String
        )
    }

    suspend fun createLink(input: Any): ShareLinkCreateResult {
        val response = shareLinksApi.create(input = input)
        if (!response.isSuccessful) throw Exception("Failed to create share link")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        val map = moshi.adapter(Map::class.java).fromJson(json)
        return ShareLinkCreateResult(
            code = map?.get("code") as? String ?: "",
            url = map?.get("url") as? String
        )
    }
}
