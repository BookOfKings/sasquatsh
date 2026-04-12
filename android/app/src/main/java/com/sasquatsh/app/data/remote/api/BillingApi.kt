package com.sasquatsh.app.data.remote.api

import com.sasquatsh.app.data.remote.dto.BillingInfoDto
import com.sasquatsh.app.data.remote.dto.CheckoutSessionDto
import com.sasquatsh.app.data.remote.dto.PortalSessionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface BillingApi {

    @GET("billing")
    suspend fun getBillingInfo(): Response<BillingInfoDto>

    @POST("stripe-checkout")
    suspend fun createCheckoutSession(@Body body: Map<String, String>): Response<CheckoutSessionDto>

    @POST("stripe-portal")
    suspend fun createPortalSession(): Response<PortalSessionDto>
}
