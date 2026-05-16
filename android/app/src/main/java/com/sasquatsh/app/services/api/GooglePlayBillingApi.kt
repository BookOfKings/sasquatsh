package com.sasquatsh.app.services.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GooglePlayBillingApi {

    @POST("google-play-webhook")
    suspend fun verifyPurchase(
        @Query("action") action: String = "verify",
        @Body body: Map<String, String?>
    ): Response<Any>
}
