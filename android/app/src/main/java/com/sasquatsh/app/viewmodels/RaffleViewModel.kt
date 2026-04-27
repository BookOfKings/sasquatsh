package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.Raffle
import com.sasquatsh.app.models.RaffleEntryType
import com.sasquatsh.app.services.RaffleService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RaffleUiState(
    val raffle: Raffle? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val mailInName: String = "",
    val mailInAddress: String = "",
    val isSubmittingMailIn: Boolean = false,
    val mailInSuccess: Boolean = false
) {
    val entriesByType: List<Pair<RaffleEntryType, Int>>
        get() {
            val entries = raffle?.userEntries ?: return emptyList()
            return RaffleEntryType.values().mapNotNull { entryType ->
                val count = entries
                    .filter { it.entryType == entryType.value }
                    .sumOf { it.entryCount }
                if (count > 0) entryType to count else null
            }
        }
}

@HiltViewModel
class RaffleViewModel @Inject constructor(
    private val raffleService: RaffleService
) : ViewModel() {

    private val _uiState = MutableStateFlow(RaffleUiState())
    val uiState: StateFlow<RaffleUiState> = _uiState.asStateFlow()

    fun loadActiveRaffle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val raffle = try {
                raffleService.getActiveRaffle()
            } catch (_: Exception) {
                null
            }
            _uiState.update { it.copy(raffle = raffle, isLoading = false) }
        }
    }

    fun updateMailInName(name: String) {
        _uiState.update { it.copy(mailInName = name) }
    }

    fun updateMailInAddress(address: String) {
        _uiState.update { it.copy(mailInAddress = address) }
    }

    fun submitMailInEntry() {
        val raffle = _uiState.value.raffle ?: return
        val name = _uiState.value.mailInName.trim()
        val address = _uiState.value.mailInAddress.trim()

        if (name.isEmpty() || address.isEmpty()) {
            _uiState.update { it.copy(error = "Name and address are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingMailIn = true, error = null) }
            try {
                raffleService.submitMailInEntry(raffle.id, name, address)
                _uiState.update {
                    it.copy(
                        mailInSuccess = true,
                        mailInName = "",
                        mailInAddress = "",
                        isSubmittingMailIn = false
                    )
                }
                loadActiveRaffle()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.localizedMessage, isSubmittingMailIn = false)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
