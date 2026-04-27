package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.AddCollectionGameInput
import com.sasquatsh.app.models.BggSearchResult
import com.sasquatsh.app.models.CollectionGame
import com.sasquatsh.app.services.BggService
import com.sasquatsh.app.services.CollectionsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionUiState(
    val myGames: List<CollectionGame> = emptyList(),
    val topGames: List<CollectionGame> = emptyList(),
    val searchResults: List<BggSearchResult> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val error: String? = null,
    val activeTab: Int = 0,
    val filterText: String = "",
    val pendingAdds: Set<Int> = emptySet(),
    val pendingRemoves: Set<Int> = emptySet()
) {
    val ownedBggIds: Set<Int>
        get() = myGames.mapNotNull { it.bggId }.toSet()

    val filteredGames: List<CollectionGame>
        get() = if (filterText.isEmpty()) myGames
        else myGames.filter { it.gameName.contains(filterText, ignoreCase = true) }
}

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionsService: CollectionsService,
    private val bggService: BggService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun loadCollection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val myGames = collectionsService.getMyCollection()
                val topGames = collectionsService.getTopGames()
                _uiState.update {
                    it.copy(
                        myGames = myGames.sortedBy { g -> g.gameName },
                        topGames = topGames,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun updateActiveTab(tab: Int) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun updateFilterText(text: String) {
        _uiState.update { it.copy(filterText = text) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isEmpty()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            searchBGG(query)
        }
    }

    private suspend fun searchBGG(query: String) {
        _uiState.update { it.copy(isSearching = true) }
        try {
            val results = bggService.searchGames(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.localizedMessage, isSearching = false) }
        }
    }

    fun toggleGame(bggId: Int, game: CollectionGame, isOwned: Boolean) {
        viewModelScope.launch {
            if (isOwned) {
                _uiState.update { it.copy(pendingRemoves = it.pendingRemoves + bggId) }
                try {
                    collectionsService.removeGame(bggId)
                    _uiState.update { state ->
                        state.copy(
                            myGames = state.myGames.filter { it.bggId != bggId },
                            pendingRemoves = state.pendingRemoves - bggId
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.localizedMessage, pendingRemoves = it.pendingRemoves - bggId) }
                }
            } else {
                _uiState.update { it.copy(pendingAdds = it.pendingAdds + bggId) }
                try {
                    val input = AddCollectionGameInput(
                        bggId = bggId,
                        name = game.gameName,
                        thumbnailUrl = game.thumbnailUrl,
                        minPlayers = game.minPlayers,
                        maxPlayers = game.maxPlayers,
                        playingTime = game.playingTime,
                        yearPublished = game.yearPublished,
                        bggRank = game.bggRank,
                        averageRating = game.averageRating
                    )
                    val added = collectionsService.addGame(input)
                    _uiState.update { state ->
                        state.copy(
                            myGames = (state.myGames + added).sortedBy { it.gameName },
                            pendingAdds = state.pendingAdds - bggId
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.localizedMessage, pendingAdds = it.pendingAdds - bggId) }
                }
            }
        }
    }

    fun addFromSearch(result: BggSearchResult) {
        viewModelScope.launch {
            val bggId = result.bggId
            _uiState.update { it.copy(pendingAdds = it.pendingAdds + bggId) }
            try {
                val input = AddCollectionGameInput(
                    bggId = bggId,
                    name = result.name,
                    thumbnailUrl = result.thumbnailUrl,
                    yearPublished = result.yearPublished
                )
                val added = collectionsService.addGame(input)
                _uiState.update { state ->
                    state.copy(
                        myGames = (state.myGames + added).sortedBy { it.gameName },
                        pendingAdds = state.pendingAdds - bggId
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, pendingAdds = it.pendingAdds - bggId) }
            }
        }
    }
}
