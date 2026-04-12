package com.sasquatsh.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BillingInfoDto(
    @Json(name = "subscriptionTier") val subscriptionTier: String?,
    @Json(name = "subscriptionOverrideTier") val subscriptionOverrideTier: String?,
    @Json(name = "stripeCustomerId") val stripeCustomerId: String?,
    @Json(name = "currentPeriodEnd") val currentPeriodEnd: String?,
    @Json(name = "cancelAtPeriodEnd") val cancelAtPeriodEnd: Boolean?,
    @Json(name = "invoices") val invoices: List<InvoiceDto>?,
)

@JsonClass(generateAdapter = true)
data class InvoiceDto(
    @Json(name = "id") val id: String,
    @Json(name = "amount") val amount: Int?,
    @Json(name = "currency") val currency: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "created") val created: Long?,
    @Json(name = "hostedInvoiceUrl") val hostedInvoiceUrl: String?,
)

@JsonClass(generateAdapter = true)
data class CheckoutSessionDto(
    @Json(name = "url") val url: String,
)

@JsonClass(generateAdapter = true)
data class PortalSessionDto(
    @Json(name = "url") val url: String,
)
