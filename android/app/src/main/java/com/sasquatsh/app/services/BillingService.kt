package com.sasquatsh.app.services

import com.sasquatsh.app.models.*
import com.sasquatsh.app.services.api.BillingApi
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingService @Inject constructor(
    private val billingApi: BillingApi,
    private val moshi: Moshi
) {

    suspend fun getSubscriptionInfo(): SubscriptionInfo {
        val response = billingApi.getSubscriptionInfo()
        if (!response.isSuccessful) throw Exception("Failed to load subscription info")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(SubscriptionInfo::class.java).fromJson(json)
            ?: throw Exception("Failed to parse subscription info")
    }

    suspend fun getInvoices(page: Int): InvoicesResponse {
        val response = billingApi.getInvoices(page = page)
        if (!response.isSuccessful) throw Exception("Failed to load invoices")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(InvoicesResponse::class.java).fromJson(json)
            ?: throw Exception("Failed to parse invoices")
    }

    suspend fun cancelSubscription(): CancelResponse {
        val response = billingApi.cancelSubscription()
        if (!response.isSuccessful) throw Exception("Failed to cancel subscription")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(CancelResponse::class.java).fromJson(json)
            ?: throw Exception("Failed to parse response")
    }

    suspend fun reactivateSubscription(): ReactivateResponse {
        val response = billingApi.reactivateSubscription()
        if (!response.isSuccessful) throw Exception("Failed to reactivate subscription")
        val json = moshi.adapter(Any::class.java).toJson(response.body())
        return moshi.adapter(ReactivateResponse::class.java).fromJson(json)
            ?: throw Exception("Failed to parse response")
    }
}
