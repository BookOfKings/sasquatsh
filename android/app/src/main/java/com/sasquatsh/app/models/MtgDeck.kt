package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScryfallCard(
    @Json(name = "scryfallId") val scryfallId: String,
    @Json(name = "oracleId") val oracleId: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "manaCost") val manaCost: String? = null,
    @Json(name = "cmc") val cmc: Double? = null,
    @Json(name = "typeLine") val typeLine: String? = null,
    @Json(name = "oracleText") val oracleText: String? = null,
    @Json(name = "power") val power: String? = null,
    @Json(name = "toughness") val toughness: String? = null,
    @Json(name = "loyalty") val loyalty: String? = null,
    @Json(name = "colors") val colors: List<String>? = null,
    @Json(name = "colorIdentity") val colorIdentity: List<String>? = null,
    @Json(name = "keywords") val keywords: List<String>? = null,
    @Json(name = "legalities") val legalities: Map<String, String>? = null,
    @Json(name = "setCode") val setCode: String? = null,
    @Json(name = "rarity") val rarity: String? = null,
    @Json(name = "imageUris") val imageUris: ScryfallImageUris? = null,
    @Json(name = "prices") val prices: Map<String, String?>? = null,
    @Json(name = "isDoubleFaced") val isDoubleFaced: Boolean? = null,
    @Json(name = "cardFaces") val cardFaces: List<ScryfallCardFace>? = null
) {
    val id: String get() = scryfallId

    val smallImageUrl: String? get() = imageUris?.small ?: cardFaces?.firstOrNull()?.imageUris?.small
    val normalImageUrl: String? get() = imageUris?.normal ?: cardFaces?.firstOrNull()?.imageUris?.normal
    val largeImageUrl: String? get() = imageUris?.large ?: cardFaces?.firstOrNull()?.imageUris?.large

    val isCreature: Boolean get() = typeLine?.contains("Creature") == true
    val isLand: Boolean get() = typeLine?.contains("Land") == true
    val isInstant: Boolean get() = typeLine?.contains("Instant") == true
    val isSorcery: Boolean get() = typeLine?.contains("Sorcery") == true
    val isEnchantment: Boolean get() = typeLine?.contains("Enchantment") == true
    val isArtifact: Boolean get() = typeLine?.contains("Artifact") == true
    val isPlaneswalker: Boolean get() = typeLine?.contains("Planeswalker") == true

    val typeCategory: String
        get() = when {
            isCreature -> "Creatures"
            isInstant -> "Instants"
            isSorcery -> "Sorceries"
            isEnchantment -> "Enchantments"
            isArtifact -> "Artifacts"
            isPlaneswalker -> "Planeswalkers"
            isLand -> "Lands"
            else -> "Other"
        }
}

@JsonClass(generateAdapter = true)
data class ScryfallImageUris(
    @Json(name = "small") val small: String? = null,
    @Json(name = "normal") val normal: String? = null,
    @Json(name = "large") val large: String? = null,
    @Json(name = "artCrop") val artCrop: String? = null,
    @Json(name = "png") val png: String? = null
)

@JsonClass(generateAdapter = true)
data class ScryfallCardFace(
    @Json(name = "name") val name: String? = null,
    @Json(name = "manaCost") val manaCost: String? = null,
    @Json(name = "typeLine") val typeLine: String? = null,
    @Json(name = "oracleText") val oracleText: String? = null,
    @Json(name = "imageUris") val imageUris: ScryfallImageUris? = null
)

@JsonClass(generateAdapter = true)
data class MtgDeck(
    @Json(name = "id") val id: String,
    @Json(name = "ownerUserId") val ownerUserId: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "formatId") val formatId: String? = null,
    @Json(name = "commanderScryfallId") val commanderScryfallId: String? = null,
    @Json(name = "partnerCommanderScryfallId") val partnerCommanderScryfallId: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "powerLevel") val powerLevel: Int? = null,
    @Json(name = "isPublic") val isPublic: Boolean? = null,
    @Json(name = "cards") val cards: List<MtgDeckCard>? = null,
    @Json(name = "cardCount") val cardCount: Int? = null,
    @Json(name = "commander") val commander: ScryfallCard? = null,
    @Json(name = "partnerCommander") val partnerCommander: ScryfallCard? = null
)

@JsonClass(generateAdapter = true)
data class MtgDeckCard(
    @Json(name = "id") val id: String,
    @Json(name = "deckId") val deckId: String? = null,
    @Json(name = "scryfallId") val scryfallId: String,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "board") val board: String,
    @Json(name = "card") val card: ScryfallCard? = null
)

data class CreateDeckInput(
    @Json(name = "name") val name: String,
    @Json(name = "formatId") val formatId: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "powerLevel") val powerLevel: Int? = null,
    @Json(name = "isPublic") val isPublic: Boolean? = null
)

data class UpdateDeckInput(
    @Json(name = "name") val name: String? = null,
    @Json(name = "formatId") val formatId: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "powerLevel") val powerLevel: Int? = null,
    @Json(name = "isPublic") val isPublic: Boolean? = null,
    @Json(name = "commanderScryfallId") val commanderScryfallId: String? = null,
    @Json(name = "partnerCommanderScryfallId") val partnerCommanderScryfallId: String? = null
)

data class DeckCardInput(
    @Json(name = "scryfallId") val scryfallId: String,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "board") val board: String
)

data class ImportDeckInput(
    @Json(name = "name") val name: String? = null,
    @Json(name = "formatId") val formatId: String? = null,
    @Json(name = "deckList") val deckList: String? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class DeckResponse(
    @Json(name = "deck") val deck: MtgDeck? = null
)

@JsonClass(generateAdapter = true)
data class DecksResponse(
    @Json(name = "decks") val decks: List<MtgDeck>? = null
)
