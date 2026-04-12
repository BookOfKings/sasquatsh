package com.sasquatsh.app.ui.mtg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.data.remote.ApiResult
import com.sasquatsh.app.data.remote.dto.MtgDeckDto
import com.sasquatsh.app.data.repository.MtgDeckRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeckListUiState(
    val decks: List<MtgDeckDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DeckListViewModel @Inject constructor(
    private val repository: MtgDeckRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeckListUiState())
    val uiState: StateFlow<DeckListUiState> = _uiState

    init {
        loadDecks()
    }

    fun loadDecks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getMyDecks()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(isLoading = false, decks = result.data)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun deleteDeck(deckId: String) {
        viewModelScope.launch {
            when (repository.deleteDeck(deckId)) {
                is ApiResult.Success -> loadDecks()
                is ApiResult.Error -> {} // Silently fail, could show snackbar
            }
        }
    }
}
