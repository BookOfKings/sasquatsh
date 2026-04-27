package com.sasquatsh.app.services

import com.sasquatsh.app.models.Raffle
import com.sasquatsh.app.models.RaffleResponse
import com.sasquatsh.app.services.api.RaffleApi
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RaffleService @Inject constructor(
    private val raffleApi: RaffleApi,
    private val moshi: Moshi
) {
    suspend fun getActiveRaffle(): Raffle? {
        val response = raffleApi.getActiveRaffle()
        if (!response.isSuccessful) throw Exception("Failed to load raffle")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(RaffleResponse::class.java).fromJson(json)?.raffle
    }

    suspend fun submitMailInEntry(raffleId: String, name: String, address: String) {
        val body = mapOf("raffleId" to raffleId, "name" to name, "address" to address)
        val response = raffleApi.submitMailInEntry(input = body)
        if (!response.isSuccessful) throw Exception("Failed to submit mail-in entry")
    }
}
