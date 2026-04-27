package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.MtgDeck
import com.sasquatsh.app.services.MtgDeckService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeckListUiState(
    val decks: List<MtgDeck> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterFormat: String? = null
) {
    val filteredDecks: List<MtgDeck>
        get() = if (filterFormat != null) {
            decks.filter { it.formatId == filterFormat }
        } else {
            decks
        }
}

@HiltViewModel
class DeckListViewModel @Inject constructor(
    private val mtgDeckService: MtgDeckService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeckListUiState())
    val uiState: StateFlow<DeckListUiState> = _uiState.asStateFlow()

    fun loadDecks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val decks = mtgDeckService.getMyDecks()
                _uiState.update { it.copy(decks = decks, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun deleteDeck(id: String) {
        viewModelScope.launch {
            try {
                mtgDeckService.deleteDeck(id)
                _uiState.update { state ->
                    state.copy(decks = state.decks.filter { it.id != id })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun updateFilterFormat(format: String?) {
        _uiState.update { it.copy(filterFormat = format) }
    }
}
