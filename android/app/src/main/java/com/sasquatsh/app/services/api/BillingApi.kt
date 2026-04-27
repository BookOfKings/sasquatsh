package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.*

interface BillingApi {

    // GET billing
    @GET("billing")
    suspend fun getSubscriptionInfo(): Response<Any>

    // GET billing?include=invoices&page=...
    @GET("billing")
    suspend fun getInvoices(
        @Query("include") include: String = "invoices",
        @Query("page") page: Int
    ): Response<Any>

    // GET billing?invoiceId=...
    @GET("billing")
    suspend fun getInvoice(
        @Query("invoiceId") id: String
    ): Response<Any>

    // POST billing?action=cancel
    @POST("billing")
    suspend fun cancelSubscription(
        @Query("action") action: String = "cancel"
    ): Response<Any>

    // POST billing?action=reactivate
    @POST("billing")
    suspend fun reactivateSubscription(
        @Query("action") action: String = "reactivate"
    ): Response<Any>
}
