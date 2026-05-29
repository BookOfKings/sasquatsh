package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.AddCollectionGameInput
import com.sasquatsh.app.models.BggCollectionGame
import com.sasquatsh.app.models.BggSearchResult
import com.sasquatsh.app.models.CollectionGame
import com.sasquatsh.app.services.BggService
import com.sasquatsh.app.services.CollectionsService
import com.sasquatsh.app.services.GameUpcService
import com.sasquatsh.app.services.ProfileService
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
    val pendingRemoves: Set<Int> = emptySet(),
    val bggUsername: String = "",
    val bggGames: List<BggCollectionGame> = emptyList(),
    val isFetchingBgg: Boolean = false,
    val isImporting: Boolean = false,
    val bggError: String? = null,
    val bggImportResult: String? = null
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
    val bggService: BggService,
    val gameUpcService: GameUpcService,
    private val profileService: ProfileService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun loadCollection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val myGames = collectionsService.getMyCollection()
                _uiState.update {
                    it.copy(
                        myGames = myGames.sortedBy { g -> g.gameName },
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
            // Load top games separately so a failure doesn't block my collection
            try {
                val topGames = collectionsService.getTopGames()
                _uiState.update { it.copy(topGames = topGames) }
            } catch (_: Exception) {
                // Top games are non-critical
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

    fun addFromInput(input: AddCollectionGameInput) {
        viewModelScope.launch {
            val bggId = input.bggId
            _uiState.update { it.copy(pendingAdds = it.pendingAdds + bggId) }
            try {
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

    // --- BGG Import ---

    fun updateBggUsername(username: String) {
        _uiState.update { it.copy(bggUsername = username) }
    }

    fun prefillBggUsername() {
        viewModelScope.launch {
            try {
                val profile = profileService.getMyProfile()
                profile.bggUsername?.let { username ->
                    if (username.isNotEmpty() && _uiState.value.bggUsername.isEmpty()) {
                        _uiState.update { it.copy(bggUsername = username) }
                    }
                }
            } catch (_: Exception) {
                // Non-critical
            }
        }
    }

    fun fetchBggCollection() {
        val username = _uiState.value.bggUsername.trim()
        if (username.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingBgg = true, bggError = null, bggImportResult = null, bggGames = emptyList()) }
            try {
                val response = bggService.fetchCollection(username)
                if (response.games.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isFetchingBgg = false,
                            bggError = "No games found for '$username'. Check the username and make sure the collection is public on BGG."
                        )
                    }
                } else {
                    _uiState.update { it.copy(bggGames = response.games, isFetchingBgg = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(bggError = e.localizedMessage, isFetchingBgg = false) }
            }
        }
    }

    fun importAddNewOnly() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, bggError = null, bggImportResult = null) }
            try {
                val ownedIds = _uiState.value.ownedBggIds
                val newGames = _uiState.value.bggGames.filter { !ownedIds.contains(it.bggId) }
                val added = addBggGamesToCollection(newGames)
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        bggImportResult = "Added $added new game${if (added == 1) "" else "s"} to your collection"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, bggError = "Import failed: ${e.localizedMessage}") }
            }
        }
    }

    fun importFullSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, bggError = null, bggImportResult = null) }
            try {
                val ownedIds = _uiState.value.ownedBggIds
                val bggIds = _uiState.value.bggGames.map { it.bggId }.toSet()

                val newGames = _uiState.value.bggGames.filter { !ownedIds.contains(it.bggId) }
                val added = addBggGamesToCollection(newGames)

                // Remove games not on BGG
                val toRemove = _uiState.value.myGames.filter { game ->
                    val bggId = game.bggId ?: return@filter false
                    !bggIds.contains(bggId)
                }
                var removed = 0
                var updatedGames = _uiState.value.myGames
                for (game in toRemove) {
                    val bggId = game.bggId ?: continue
                    try {
                        collectionsService.removeGame(bggId)
                        updatedGames = updatedGames.filter { it.bggId != bggId }
                        removed++
                    } catch (_: Exception) { }
                }
                _uiState.update { it.copy(myGames = updatedGames) }

                _uiState.update {
                    it.copy(
                        isImporting = false,
                        bggImportResult = "Synced: $added added, $removed removed"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, bggError = "Import failed: ${e.localizedMessage}") }
            }
        }
    }

    private suspend fun addBggGamesToCollection(games: List<BggCollectionGame>): Int {
        if (games.isEmpty()) return 0
        var added = 0
        val addedGames = mutableListOf<CollectionGame>()
        for (game in games) {
            try {
                val input = AddCollectionGameInput(
                    bggId = game.bggId,
                    name = game.name,
                    thumbnailUrl = game.thumbnailUrl,
                    imageUrl = game.imageUrl,
                    yearPublished = game.yearPublished
                )
                val result = collectionsService.addGame(input)
                addedGames.add(result)
                added++
            } catch (_: Exception) { }
        }
        // Single state update + sort after all additions
        _uiState.update { state ->
            state.copy(myGames = (state.myGames + addedGames).sortedBy { it.gameName })
        }
        return added
    }
}
