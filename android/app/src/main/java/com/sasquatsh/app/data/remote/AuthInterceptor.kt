package com.sasquatsh.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.sasquatsh.app.util.Constants
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val builder = original.newBuilder()
            .header("Authorization", "Bearer ${Constants.SUPABASE_ANON_KEY}")
            .header("Content-Type", "application/json")

        val user = firebaseAuth.currentUser
        if (user != null) {
            val token = runBlocking {
                try {
                    user.getIdToken(false).await().token
                } catch (e: Exception) {
                    null
                }
            }
            if (token != null) {
                builder.header("X-Firebase-Token", token)
            }
        }

        val response = chain.proceed(builder.build())

        // If 401, try refreshing the token once
        if (response.code == 401 && user != null) {
            response.close()
            val freshToken = runBlocking {
                try {
                    user.getIdToken(true).await().token
                } catch (e: Exception) {
                    null
                }
            }
            if (freshToken != null) {
                val retryRequest = original.newBuilder()
                    .header("Authorization", "Bearer ${Constants.SUPABASE_ANON_KEY}")
                    .header("Content-Type", "application/json")
                    .header("X-Firebase-Token", freshToken)
                    .build()
                return chain.proceed(retryRequest)
            }
        }

        return response
    }
}
