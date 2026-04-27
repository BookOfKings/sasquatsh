package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.CreateGroupInput
import com.sasquatsh.app.models.GameGroup
import com.sasquatsh.app.models.GroupType
import com.sasquatsh.app.models.JoinPolicy
import com.sasquatsh.app.models.UpdateGroupInput
import com.sasquatsh.app.services.GroupsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateEditGroupUiState(
    val name: String = "",
    val description: String = "",
    val groupType: GroupType = GroupType.GEOGRAPHIC,
    val locationCity: String = "",
    val locationState: String = "",
    val locationRadiusMiles: Int? = null,
    val joinPolicy: JoinPolicy = JoinPolicy.OPEN,
    val isLoading: Boolean = false,
    val isUploadingLogo: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false,
    val currentLogoUrl: String? = null
) {
    val isValid: Boolean get() = name.trim().isNotEmpty()
}

@HiltViewModel
class CreateEditGroupViewModel @Inject constructor(
    private val groupsService: GroupsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditGroupUiState())
    val uiState: StateFlow<CreateEditGroupUiState> = _uiState.asStateFlow()

    private var groupId: String? = null

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateGroupType(type: GroupType) {
        _uiState.update { it.copy(groupType = type) }
    }

    fun updateLocationCity(city: String) {
        _uiState.update { it.copy(locationCity = city) }
    }

    fun updateLocationState(state: String) {
        _uiState.update { it.copy(locationState = state) }
    }

    fun updateLocationRadiusMiles(radius: Int?) {
        _uiState.update { it.copy(locationRadiusMiles = radius) }
    }

    fun updateJoinPolicy(policy: JoinPolicy) {
        _uiState.update { it.copy(joinPolicy = policy) }
    }

    fun loadForEdit(group: GameGroup) {
        groupId = group.id
        _uiState.update {
            it.copy(
                isEditing = true,
                name = group.name,
                description = group.description.orEmpty(),
                groupType = group.groupType,
                locationCity = group.locationCity.orEmpty(),
                locationState = group.locationState.orEmpty(),
                locationRadiusMiles = group.locationRadiusMiles,
                joinPolicy = group.joinPolicy,
                currentLogoUrl = group.logoUrl
            )
        }
    }

    fun save(onSuccess: (GameGroup) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val state = _uiState.value

            try {
                if (state.isEditing && groupId != null) {
                    val input = UpdateGroupInput(
                        name = state.name,
                        description = state.description.ifEmpty { null },
                        groupType = state.groupType,
                        locationCity = state.locationCity.ifEmpty { null },
                        locationState = state.locationState.ifEmpty { null },
                        locationRadiusMiles = state.locationRadiusMiles,
                        joinPolicy = state.joinPolicy
                    )
                    val group = groupsService.updateGroup(groupId!!, input)
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(group)
                } else {
                    val input = CreateGroupInput(
                        name = state.name,
                        description = state.description.ifEmpty { null },
                        groupType = state.groupType,
                        locationCity = state.locationCity.ifEmpty { null },
                        locationState = state.locationState.ifEmpty { null },
                        locationRadiusMiles = state.locationRadiusMiles,
                        joinPolicy = state.joinPolicy
                    )
                    val group = groupsService.createGroup(input)
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(group)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun uploadLogo(imageData: ByteArray) {
        if (groupId == null) {
            _uiState.update { it.copy(error = "Group ID not set") }
            return
        }
        if (imageData.isEmpty()) {
            _uiState.update { it.copy(error = "No image data") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingLogo = true, error = null) }
            try {
                val url = groupsService.uploadLogo(
                    groupId = groupId!!,
                    imageData = imageData,
                    fileName = "logo.jpg",
                    mimeType = "image/jpeg"
                )
                _uiState.update { it.copy(currentLogoUrl = url, isUploadingLogo = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Upload failed: ${e.localizedMessage}", isUploadingLogo = false)
                }
            }
        }
    }

    fun removeLogo() {
        if (groupId == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                val input = UpdateGroupInput(logoUrl = "")
                groupsService.updateGroup(groupId!!, input)
                _uiState.update { it.copy(currentLogoUrl = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }
}
