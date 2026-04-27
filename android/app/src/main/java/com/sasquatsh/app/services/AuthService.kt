package com.sasquatsh.app.services

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.sasquatsh.app.config.AppConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    suspend fun login(email: String, password: String): FirebaseUser {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: throw ApiException("Login failed: no user returned")
    }

    suspend fun signup(email: String, password: String): FirebaseUser {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        return result.user ?: throw ApiException("Signup failed: no user returned")
    }

    suspend fun signInWithGoogle(activityContext: Context): FirebaseUser {
        val credentialManager = CredentialManager.create(activityContext)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(AppConfig.googleWebClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(activityContext, request)
        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(
                googleIdTokenCredential.idToken, null
            )
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            return authResult.user ?: throw ApiException("Google sign-in failed: no user returned")
        } else {
            throw ApiException("Unexpected credential type")
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        return try {
            firebaseAuth.currentUser?.getIdToken(forceRefresh)?.await()?.token
        } catch (_: Exception) {
            null
        }
    }

    suspend fun resetPassword(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    suspend fun changePassword(newPassword: String) {
        val user = firebaseAuth.currentUser
            ?: throw ApiException("Not signed in")
        user.updatePassword(newPassword).await()
    }

    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        firebaseAuth.addAuthStateListener(listener)
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        firebaseAuth.removeAuthStateListener(listener)
    }

    suspend fun signInWithCredential(credential: AuthCredential): FirebaseUser {
        val result = firebaseAuth.signInWithCredential(credential).await()
        return result.user ?: throw ApiException("Sign-in failed: no user returned")
    }

    fun getWebClientId(): String {
        return AppConfig.googleWebClientId
    }

    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    suspend fun getFcmToken(): String {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (_: Exception) {
            ""
        }
    }
}
