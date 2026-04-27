package com.sasquatsh.app.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubscriptionInfo(
    @Json(name = "subscription") val subscription: SubscriptionDetail,
    @Json(name = "paymentMethod") val paymentMethod: PaymentMethod? = null,
    @Json(name = "hasStripeAccount") val hasStripeAccount: Boolean = false,
    @Json(name = "hasActiveSubscription") val hasActiveSubscription: Boolean = false,
    @Json(name = "subscriptionSource") val subscriptionSource: String? = null,
    @Json(name = "hasAppleSubscription") val hasAppleSubscription: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class SubscriptionDetail(
    @Json(name = "tier") val tier: SubscriptionTier,
    @Json(name = "effectiveTier") val effectiveTier: SubscriptionTier,
    @Json(name = "status") val status: SubscriptionStatus? = null,
    @Json(name = "expiresAt") val expiresAt: String? = null,
    @Json(name = "hasOverride") val hasOverride: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class PaymentMethod(
    @Json(name = "brand") val brand: String,
    @Json(name = "last4") val last4: String,
    @Json(name = "expMonth") val expMonth: Int,
    @Json(name = "expYear") val expYear: Int
)

@JsonClass(generateAdapter = true)
data class Invoice(
    @Json(name = "id") val id: String,
    @Json(name = "stripeInvoiceId") val stripeInvoiceId: String? = null,
    @Json(name = "amountCents") val amountCents: Int,
    @Json(name = "taxCents") val taxCents: Int? = null,
    @Json(name = "currency") val currency: String,
    @Json(name = "status") val status: InvoiceStatus,
    @Json(name = "invoiceDate") val invoiceDate: String,
    @Json(name = "periodStart") val periodStart: String? = null,
    @Json(name = "periodEnd") val periodEnd: String? = null,
    @Json(name = "hostedInvoiceUrl") val hostedInvoiceUrl: String? = null,
    @Json(name = "invoicePdfUrl") val invoicePdfUrl: String? = null,
    @Json(name = "paymentMethodBrand") val paymentMethodBrand: String? = null,
    @Json(name = "paymentMethodLast4") val paymentMethodLast4: String? = null
)

@JsonClass(generateAdapter = true)
data class InvoicesResponse(
    @Json(name = "invoices") val invoices: List<Invoice>,
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
    @Json(name = "pageSize") val pageSize: Int,
    @Json(name = "hasMore") val hasMore: Boolean
)

@JsonClass(generateAdapter = true)
data class CancelResponse(
    @Json(name = "message") val message: String,
    @Json(name = "cancelAt") val cancelAt: String? = null,
    @Json(name = "manageUrl") val manageUrl: String? = null,
    @Json(name = "source") val source: String? = null
)

@JsonClass(generateAdapter = true)
data class ReactivateResponse(
    @Json(name = "message") val message: String
)
