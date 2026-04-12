package com.sasquatsh.app.ui.mtg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.dto.MtgDeckDto
import com.sasquatsh.app.data.remote.dto.ScryfallCardDto
import com.sasquatsh.app.data.repository.MtgDeckRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeckBuilderUiState(
    val deck: MtgDeckDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchResults: List<ScryfallCardDto> = emptyList(),
    val isSearching: Boolean = false,
)

@HiltViewModel
class DeckBuilderViewModel @Inject constructor(
    private val repository: MtgDeckRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeckBuilderUiState())
    val uiState: StateFlow<DeckBuilderUiState> = _uiState

    private var searchJob: Job? = null

    fun loadDeck(deckId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.getDeck(deckId)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, deck = result.data)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun searchCards(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _uiState.value = _uiState.value.copy(isSearching = true)
            when (val result = repository.searchCards(query)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(isSearching = false, searchResults = result.data)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isSearching = false, searchResults = emptyList())
            }
        }
    }

    fun addCard(card: ScryfallCardDto) {
        val deck = _uiState.value.deck ?: return
        val deckId = deck.id

        viewModelScope.launch {
            val updates = mapOf(
                "action" to "add_card",
                "scryfallId" to card.id,
                "name" to card.name,
                "quantity" to 1,
                "board" to "main",
                "manaCost" to card.manaCost,
                "cmc" to card.cmc,
                "typeLine" to card.typeLine,
                "imageUrl" to card.imageUris?.small,
            )
            when (val result = repository.updateDeck(deckId, updates)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(deck = result.data)
                is ApiResult.Error -> {} // Could show error
            }
        }
    }
}
