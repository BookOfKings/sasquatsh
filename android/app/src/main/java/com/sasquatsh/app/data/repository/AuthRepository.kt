package com.sasquatsh.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.api.AuthApi
import com.sasquatsh.app.data.remote.dto.AuthSyncRequest
import com.sasquatsh.app.domain.model.SubscriptionTier
import com.sasquatsh.app.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val authApi: AuthApi,
) {
    val currentFirebaseUser: FirebaseUser? get() = firebaseAuth.currentUser

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun loginWithEmail(email: String, password: String): ApiResult<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val fbUser = result.user ?: return ApiResult.Error("Login failed")
            syncUser(fbUser, "email")
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Login failed")
        }
    }

    suspend fun signupWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): ApiResult<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val fbUser = result.user ?: return ApiResult.Error("Signup failed")
            syncUser(fbUser, "email", displayName)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Signup failed")
        }
    }

    suspend fun loginWithGoogle(idToken: String): ApiResult<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val fbUser = result.user ?: return ApiResult.Error("Google login failed")
            syncUser(
                fbUser,
                "google",
                fbUser.displayName,
                fbUser.photoUrl?.toString(),
            )
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Google login failed")
        }
    }

    suspend fun sendPasswordResetEmail(email: String): ApiResult<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to send reset email")
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    suspend fun deleteAccount(): ApiResult<Unit> {
        return try {
            firebaseAuth.currentUser?.delete()?.await()
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to delete account")
        }
    }

    private suspend fun syncUser(
        fbUser: FirebaseUser,
        provider: String,
        displayName: String? = null,
        avatarUrl: String? = null,
    ): ApiResult<User> {
        return try {
            val response = authApi.syncAuth(
                AuthSyncRequest(
                    email = fbUser.email ?: "",
                    displayName = displayName ?: fbUser.displayName,
                    avatarUrl = avatarUrl ?: fbUser.photoUrl?.toString(),
                    authProvider = provider,
                )
            )
            if (response.isSuccessful) {
                val body = response.body() ?: return ApiResult.Error("Empty response")
                ApiResult.Success(
                    User(
                        id = body.id,
                        firebaseUid = body.firebaseUid,
                        email = body.email,
                        displayName = body.displayName,
                        username = body.username,
                        avatarUrl = body.avatarUrl,
                        subscriptionTier = SubscriptionTier.fromValue(body.subscriptionTier),
                        isAdmin = body.isAdmin ?: false,
                        isFoundingMember = body.isFoundingMember ?: false,
                    )
                )
            } else {
                ApiResult.Error("Auth sync failed: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Auth sync failed")
        }
    }
}
