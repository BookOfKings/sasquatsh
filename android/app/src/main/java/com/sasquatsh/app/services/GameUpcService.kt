package com.sasquatsh.app.services

import com.sasquatsh.app.models.UpcLookupResult
import com.sasquatsh.app.services.api.GameUpcApi
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameUpcService @Inject constructor(
    private val gameUpcApi: GameUpcApi,
    private val moshi: Moshi
) {
    suspend fun lookupUpc(upc: String, search: String? = null): UpcLookupResult {
        val response = gameUpcApi.lookupUpc(upc, search)
        if (!response.isSuccessful) throw Exception("Failed to lookup UPC")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(UpcLookupResult::class.java).fromJson(json)
            ?: throw Exception("Failed to parse UPC result")
    }

    suspend fun voteMatch(upc: String, bggId: Int) {
        val response = gameUpcApi.voteMatch(upc, bggId)
        if (!response.isSuccessful) throw Exception("Failed to vote match")
    }
}
