package com.sasquatsh.app.services

import com.sasquatsh.app.models.*
import com.sasquatsh.app.services.api.MtgDeckApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MtgDeckService @Inject constructor(
    private val mtgDeckApi: MtgDeckApi,
    private val moshi: Moshi
) {
    private val deckListType = Types.newParameterizedType(List::class.java, MtgDeck::class.java)

    @Suppress("UNCHECKED_CAST")
    suspend fun getMyDecks(): List<MtgDeck> {
        val response = mtgDeckApi.getMyDecks()
        if (!response.isSuccessful) throw Exception("Failed to load decks")
        val body = response.body() as? List<Any?> ?: run {
            // Response is wrapped: { "decks": [...] }
            val map = response.body() as? Map<String, Any?> ?: return emptyList()
            val decks = map["decks"] ?: return emptyList()
            val json = moshi.adapter(Any::class.java).toJson(decks)
            return moshi.adapter<List<MtgDeck>>(deckListType).fromJson(json) ?: emptyList()
        }
        val json = moshi.adapter(Any::class.java).toJson(body)
        return moshi.adapter<List<MtgDeck>>(deckListType).fromJson(json) ?: emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun getDeck(id: String): MtgDeck {
        val response = mtgDeckApi.getDeck(id)
        if (!response.isSuccessful) throw Exception("Failed to load deck")
        val body = response.body() as? Map<String, Any?> ?: throw Exception("Invalid response")
        val deckMap = (body as MutableMap<String, Any?>).toMutableMap()

        // Transform cards: nested card data from cache uses snake_case
        val rawCards = deckMap["cards"] as? List<Map<String, Any?>> ?: emptyList()
        val transformedCards = rawCards.map { card ->
            val cardData = card.toMutableMap()
            val cachedCard = cardData["card"] as? Map<String, Any?>
            if (cachedCard != null) {
                // Transform snake_case cache fields to camelCase for ScryfallCard
                val transformed = mutableMapOf<String, Any?>()
                transformed["scryfallId"] = cachedCard["scryfall_id"] ?: cachedCard["scryfallId"]
                transformed["oracleId"] = cachedCard["oracle_id"] ?: cachedCard["oracleId"]
                transformed["name"] = cachedCard["name"]
                transformed["manaCost"] = cachedCard["mana_cost"] ?: cachedCard["manaCost"]
                transformed["cmc"] = cachedCard["cmc"]
                transformed["typeLine"] = cachedCard["type_line"] ?: cachedCard["typeLine"]
                transformed["oracleText"] = cachedCard["oracle_text"] ?: cachedCard["oracleText"]
                transformed["power"] = cachedCard["power"]
                transformed["toughness"] = cachedCard["toughness"]
                transformed["loyalty"] = cachedCard["loyalty"]
                transformed["colors"] = cachedCard["colors"]
                transformed["colorIdentity"] = cachedCard["color_identity"] ?: cachedCard["colorIdentity"]
                transformed["keywords"] = cachedCard["keywords"]
                transformed["legalities"] = cachedCard["legalities"]
                transformed["setCode"] = cachedCard["set_code"] ?: cachedCard["setCode"]
                transformed["rarity"] = cachedCard["rarity"]
                transformed["prices"] = cachedCard["prices"]
                transformed["isDoubleFaced"] = cachedCard["is_double_faced"] ?: cachedCard["isDoubleFaced"]
                transformed["cardFaces"] = cachedCard["card_faces"] ?: cachedCard["cardFaces"]
                // Build imageUris from flat fields if not already nested
                val existingUris = cachedCard["imageUris"] as? Map<String, Any?>
                transformed["imageUris"] = existingUris ?: mapOf(
                    "small" to cachedCard["image_uri_small"],
                    "normal" to cachedCard["image_uri_normal"],
                    "large" to cachedCard["image_uri_large"],
                    "artCrop" to cachedCard["image_uri_art_crop"],
                    "png" to cachedCard["image_uri_png"]
                )
                cardData["card"] = transformed
            }
            cardData
        }
        deckMap["cards"] = transformedCards

        val json = moshi.adapter(Any::class.java).toJson(deckMap)
        return moshi.adapter(MtgDeck::class.java).fromJson(json) ?: throw Exception("Failed to parse deck")
    }

    suspend fun createDeckWithCards(
        name: String,
        formatId: String?,
        description: String?,
        powerLevel: Int?,
        isPublic: Boolean?,
        cards: List<Map<String, Any>>
    ): MtgDeck {
        val body = mutableMapOf<String, Any?>(
            "name" to name,
            "formatId" to formatId,
            "description" to description,
            "powerLevel" to powerLevel,
            "isPublic" to isPublic,
            "cards" to cards
        )
        val response = mtgDeckApi.createDeck(body)
        if (!response.isSuccessful) throw Exception("Failed to create deck")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(DeckResponse::class.java).fromJson(json)?.deck
            ?: throw Exception("Invalid response")
    }

    suspend fun createDeck(input: CreateDeckInput): MtgDeck {
        val body = moshi.adapter(CreateDeckInput::class.java).toJsonValue(input) ?: throw Exception("Serialization failed")
        val response = mtgDeckApi.createDeck(body)
        if (!response.isSuccessful) throw Exception("Failed to create deck")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(DeckResponse::class.java).fromJson(json)?.deck
            ?: throw Exception("Invalid response")
    }

    suspend fun updateDeck(id: String, input: UpdateDeckInput): MtgDeck {
        val body = moshi.adapter(UpdateDeckInput::class.java).toJsonValue(input) ?: throw Exception("Serialization failed")
        val response = mtgDeckApi.updateDeck(id, body)
        if (!response.isSuccessful) throw Exception("Failed to update deck")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(DeckResponse::class.java).fromJson(json)?.deck
            ?: throw Exception("Invalid response")
    }

    suspend fun deleteDeck(id: String) {
        val response = mtgDeckApi.deleteDeck(id)
        if (!response.isSuccessful) throw Exception("Failed to delete deck")
    }

    suspend fun addCard(deckId: String, card: DeckCardInput) {
        val body = moshi.adapter(DeckCardInput::class.java).toJsonValue(card) ?: throw Exception("Serialization failed")
        val response = mtgDeckApi.addCard(deckId, body)
        if (!response.isSuccessful) throw Exception("Failed to add card")
    }

    suspend fun importDeck(input: ImportDeckInput): MtgDeck {
        val body = moshi.adapter(ImportDeckInput::class.java).toJsonValue(input) ?: throw Exception("Serialization failed")
        val response = mtgDeckApi.importDeck(input = body)
        if (!response.isSuccessful) throw Exception("Failed to import deck")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(DeckResponse::class.java).fromJson(json)?.deck
            ?: throw Exception("Invalid response")
    }
}
