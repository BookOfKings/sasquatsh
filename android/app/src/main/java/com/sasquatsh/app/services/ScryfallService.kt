package com.sasquatsh.app.services

import com.sasquatsh.app.models.ScryfallCard
import com.sasquatsh.app.services.api.ScryfallApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScryfallService @Inject constructor(
    private val scryfallApi: ScryfallApi,
    private val moshi: Moshi
) {
    private val cardListType = Types.newParameterizedType(List::class.java, ScryfallCard::class.java)

    suspend fun searchCards(query: String): List<ScryfallCard> {
        val response = scryfallApi.searchCards(query)
        if (!response.isSuccessful) throw Exception("Failed to search cards")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter<List<ScryfallCard>>(cardListType).fromJson(json) ?: emptyList()
    }
}
