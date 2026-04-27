package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.BlockedUser
import com.sasquatsh.app.models.UpdateProfileInput
import com.sasquatsh.app.models.UserProfile
import com.sasquatsh.app.models.UsernameCheckResponse
import com.sasquatsh.app.services.ProfileService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val blockedUsers: List<BlockedUser> = emptyList(),
    val isLoading: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileService: ProfileService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val profile = profileService.getMyProfile()
                _uiState.update { it.copy(profile = profile, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun updateProfile(input: UpdateProfileInput) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val profile = profileService.updateProfile(input)
                _uiState.update {
                    it.copy(
                        profile = profile,
                        successMessage = "Profile updated!",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val result = profileService.blockUser(userId)
                _uiState.update { state ->
                    val updatedProfile = state.profile?.copy(blockedUserIds = result.blockedUserIds)
                    state.copy(
                        profile = updatedProfile,
                        successMessage = result.message
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val result = profileService.unblockUser(userId)
                _uiState.update { state ->
                    val updatedProfile = state.profile?.copy(blockedUserIds = result.blockedUserIds)
                    state.copy(
                        profile = updatedProfile,
                        blockedUsers = state.blockedUsers.filter { it.id != userId },
                        successMessage = result.message
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun uploadAvatar(imageData: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, error = null) }
            try {
                val result = profileService.uploadAvatar(
                    imageData = imageData,
                    fileName = "avatar.jpg",
                    mimeType = "image/jpeg"
                )
                _uiState.update {
                    it.copy(
                        profile = result.user,
                        successMessage = result.message,
                        isUploadingAvatar = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.localizedMessage, isUploadingAvatar = false)
                }
            }
        }
    }

    fun deleteAvatar() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val result = profileService.deleteAvatar()
                _uiState.update {
                    it.copy(
                        profile = result.user,
                        successMessage = result.message
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    suspend fun checkUsername(username: String): UsernameCheckResponse? {
        return try {
            profileService.checkUsername(username)
        } catch (_: Exception) {
            null
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
