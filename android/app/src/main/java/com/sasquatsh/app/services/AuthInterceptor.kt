package com.sasquatsh.app.services

import com.sasquatsh.app.config.AppConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = getFirebaseToken()

        val request = originalRequest.newBuilder().apply {
            header("apikey", AppConfig.supabaseAnonKey)
            header("Content-Type", "application/json")
            if (token != null) {
                header("X-Firebase-Token", token)
                header("Authorization", "Bearer ${AppConfig.supabaseAnonKey}")
            }
        }.build()

        val response = chain.proceed(request)

        // Retry once on 401 with a fresh token
        if (response.code == 401 && token != null) {
            response.close()
            val freshToken = refreshFirebaseToken()
            if (freshToken != null) {
                val retryRequest = originalRequest.newBuilder().apply {
                    header("apikey", AppConfig.supabaseAnonKey)
                    header("Content-Type", "application/json")
                    header("X-Firebase-Token", freshToken)
                    header("Authorization", "Bearer ${AppConfig.supabaseAnonKey}")
                }.build()
                return chain.proceed(retryRequest)
            }
        }

        return response
    }

    private fun getFirebaseToken(): String? {
        return try {
            val user = firebaseAuth.currentUser ?: return null
            runBlocking {
                user.getIdToken(false).await().token
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun refreshFirebaseToken(): String? {
        return try {
            val user = firebaseAuth.currentUser ?: return null
            runBlocking {
                user.getIdToken(true).await().token
            }
        } catch (_: Exception) {
            null
        }
    }
}
