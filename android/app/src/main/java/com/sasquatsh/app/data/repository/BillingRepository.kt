package com.sasquatsh.app.data.repository

import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.api.BillingApi
import com.sasquatsh.app.data.remote.dto.BillingInfoDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    private val billingApi: BillingApi,
) {
    suspend fun getBillingInfo(): ApiResult<BillingInfoDto> {
        return try {
            val response = billingApi.getBillingInfo()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Failed to load billing info: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load billing info")
        }
    }

    suspend fun createCheckoutSession(priceId: String): ApiResult<String> {
        return try {
            val response = billingApi.createCheckoutSession(mapOf("priceId" to priceId))
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!.url)
            } else {
                ApiResult.Error("Failed to create checkout: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to create checkout")
        }
    }

    suspend fun createPortalSession(): ApiResult<String> {
        return try {
            val response = billingApi.createPortalSession()
            if (response.isSuccessful) {
                ApiResult.Success(response.body()!!.url)
            } else {
                ApiResult.Error("Failed to open billing portal: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to open billing portal")
        }
    }
}
