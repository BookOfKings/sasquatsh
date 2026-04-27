package com.sasquatsh.app.viewmodels

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.sasquatsh.app.models.User
import com.sasquatsh.app.services.AuthService
import com.sasquatsh.app.services.ApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val error: String? = null
) {
    val isAuthenticated: Boolean get() = user != null
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val apiClient: ApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        initialize()
    }

    private fun initialize() {
        _uiState.update { it.copy(isLoading = true) }

        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            viewModelScope.launch {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    syncUser()
                } else {
                    _uiState.update { it.copy(user = null) }
                }
                _uiState.update { it.copy(isInitialized = true, isLoading = false) }
            }
        }
        authService.addAuthStateListener(authStateListener!!)
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                authService.login(email, password)
                syncUser()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun signup(email: String, password: String, displayName: String, username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val firebaseUser = authService.signup(email, password)
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()

                val body = mapOf("username" to username)
                apiClient.post<User>("auth-sync", body, authenticated = true)
                syncUser()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun handleGoogleSignInResult(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                authService.signInWithCredential(credential)
                syncUser()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(authService.getWebClientId())
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, gso)
    }

    fun logout() {
        try {
            authService.logout()
            _uiState.update { it.copy(user = null) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.localizedMessage) }
        }
    }

    fun refreshUser() {
        viewModelScope.launch {
            syncUser()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun syncUser() {
        try {
            val fcmToken = authService.getFcmToken()
            val body = mapOf("fcmToken" to fcmToken)
            val syncedUser = apiClient.post<User>("auth-sync", body, authenticated = true)
            _uiState.update { it.copy(user = syncedUser, error = null) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.localizedMessage) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { authService.removeAuthStateListener(it) }
    }
}
