package com.sasquatsh.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScryfallCardDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "manaCost") val manaCost: String?,
    @Json(name = "cmc") val cmc: Double?,
    @Json(name = "typeLine") val typeLine: String?,
    @Json(name = "oracleText") val oracleText: String?,
    @Json(name = "power") val power: String?,
    @Json(name = "toughness") val toughness: String?,
    @Json(name = "colors") val colors: List<String>?,
    @Json(name = "colorIdentity") val colorIdentity: List<String>?,
    @Json(name = "setName") val setName: String?,
    @Json(name = "set") val set: String?,
    @Json(name = "rarity") val rarity: String?,
    @Json(name = "imageUris") val imageUris: CardImageUrisDto?,
    @Json(name = "prices") val prices: CardPricesDto?,
    @Json(name = "legalities") val legalities: Map<String, String>?,
)

@JsonClass(generateAdapter = true)
data class CardImageUrisDto(
    @Json(name = "small") val small: String?,
    @Json(name = "normal") val normal: String?,
    @Json(name = "large") val large: String?,
    @Json(name = "art_crop") val artCrop: String?,
    @Json(name = "png") val png: String?,
)

@JsonClass(generateAdapter = true)
data class CardPricesDto(
    @Json(name = "usd") val usd: String?,
    @Json(name = "usd_foil") val usdFoil: String?,
)

@JsonClass(generateAdapter = true)
data class MtgDeckDto(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String?,
    @Json(name = "name") val name: String,
    @Json(name = "formatId") val formatId: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "isPublic") val isPublic: Boolean?,
    @Json(name = "cards") val cards: List<MtgDeckCardDto>?,
    @Json(name = "createdAt") val createdAt: String?,
    @Json(name = "updatedAt") val updatedAt: String?,
)

@JsonClass(generateAdapter = true)
data class MtgDeckCardDto(
    @Json(name = "id") val id: String?,
    @Json(name = "scryfallId") val scryfallId: String,
    @Json(name = "name") val name: String,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "board") val board: String?,
    @Json(name = "manaCost") val manaCost: String?,
    @Json(name = "cmc") val cmc: Double?,
    @Json(name = "typeLine") val typeLine: String?,
    @Json(name = "imageUrl") val imageUrl: String?,
)
