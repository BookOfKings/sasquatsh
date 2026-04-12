package com.sasquatsh.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.dto.ProfileDto
import com.sasquatsh.app.data.remote.dto.UpdateProfileRequest
import com.sasquatsh.app.data.repository.AuthRepository
import com.sasquatsh.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: ProfileDto? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    // Editable fields
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val homeCity: String = "",
    val homeState: String = "",
    val favoriteGames: List<String> = emptyList(),
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = profileRepository.getProfile()) {
                is ApiResult.Success -> {
                    val p = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = p,
                            displayName = p.displayName ?: "",
                            username = p.username ?: "",
                            bio = p.bio ?: "",
                            homeCity = p.homeCity ?: "",
                            homeState = p.homeState ?: "",
                            favoriteGames = p.favoriteGames ?: emptyList(),
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun updateDisplayName(value: String) {
        _uiState.update { it.copy(displayName = value) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun updateBio(value: String) {
        _uiState.update { it.copy(bio = value) }
    }

    fun updateHomeCity(value: String) {
        _uiState.update { it.copy(homeCity = value) }
    }

    fun updateHomeState(value: String) {
        _uiState.update { it.copy(homeState = value) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }
            val request = UpdateProfileRequest(
                displayName = state.displayName.ifBlank { null },
                username = state.username.ifBlank { null },
                bio = state.bio.ifBlank { null },
                homeCity = state.homeCity.ifBlank { null },
                homeState = state.homeState.ifBlank { null },
                favoriteGames = state.favoriteGames.ifEmpty { null },
            )
            when (val result = profileRepository.updateProfile(request)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isSaving = false, saveSuccess = true, profile = result.data)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = result.message) }
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.deleteAccount()) {
                is ApiResult.Success -> {
                    // Auth state listener will handle navigation
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
