package com.sasquatsh.app.services

import com.sasquatsh.app.config.AppConfig
import com.squareup.moshi.Moshi
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    val okHttpClient: OkHttpClient,
    val moshi: Moshi
) {

    suspend inline fun <reified T> post(
        endpoint: String,
        body: Any? = null,
        authenticated: Boolean = false
    ): T = withContext(Dispatchers.IO) {
        val url = "${AppConfig.supabaseFunctionsUrl}/$endpoint"
        val jsonBody = if (body != null) {
            moshi.adapter(Any::class.java).toJson(body)
        } else {
            "{}"
        }
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            throw ApiException(
                parseErrorMessage(errorBody) ?: "Request failed with code ${response.code}",
                response.code
            )
        }

        val responseBody = response.body?.string()
            ?: throw ApiException("Empty response body")

        moshi.adapter(T::class.java).fromJson(responseBody)
            ?: throw ApiException("Failed to parse response")
    }

    suspend inline fun <reified T> get(
        endpoint: String,
        queryParams: Map<String, String> = emptyMap()
    ): T = withContext(Dispatchers.IO) {
        val urlBuilder = "${AppConfig.supabaseFunctionsUrl}/$endpoint".toHttpUrl()
            .newBuilder()

        queryParams.forEach { (key, value) ->
            urlBuilder.addQueryParameter(key, value)
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            throw ApiException(
                parseErrorMessage(errorBody) ?: "Request failed with code ${response.code}",
                response.code
            )
        }

        val responseBody = response.body?.string()
            ?: throw ApiException("Empty response body")

        moshi.adapter(T::class.java).fromJson(responseBody)
            ?: throw ApiException("Failed to parse response")
    }

    fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val map = moshi.adapter(Map::class.java).fromJson(errorBody)
            map?.get("error") as? String ?: map?.get("message") as? String
        } catch (_: Exception) {
            errorBody
        }
    }
}
