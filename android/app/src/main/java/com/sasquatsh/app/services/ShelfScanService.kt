package com.sasquatsh.app.services

import com.sasquatsh.app.models.ShelfScanQuota
import com.sasquatsh.app.models.ShelfScanResult
import com.sasquatsh.app.services.api.ShelfScanApi
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShelfScanService @Inject constructor(
    private val shelfScanApi: ShelfScanApi,
    private val moshi: Moshi
) {
    suspend fun getRemainingScans(): ShelfScanQuota {
        val response = shelfScanApi.getRemainingScans()
        if (!response.isSuccessful) throw Exception("Failed to get scan quota")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(ShelfScanQuota::class.java).fromJson(json)
            ?: ShelfScanQuota()
    }

    suspend fun scanImage(base64Image: String): ShelfScanResult {
        val body = mapOf("image" to base64Image)
        val response = shelfScanApi.scanImage(body)
        if (!response.isSuccessful) throw Exception("Failed to scan shelf image")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(ShelfScanResult::class.java).fromJson(json)
            ?: throw Exception("Failed to parse scan result")
    }
}
