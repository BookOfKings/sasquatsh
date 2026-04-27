package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.CreateDeckInput
import com.sasquatsh.app.models.DeckCardInput
import com.sasquatsh.app.models.MtgDeck
import com.sasquatsh.app.models.MtgDeckCard
import com.sasquatsh.app.models.ScryfallCard
import com.sasquatsh.app.models.UpdateDeckInput
import com.sasquatsh.app.services.MtgDeckService
import com.sasquatsh.app.services.ScryfallService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DeckBuilderUiState(
    val deckName: String = "",
    val formatId: String? = null,
    val description: String = "",
    val powerLevel: Int? = null,
    val isPublic: Boolean = false,
    val commanderScryfallId: String? = null,
    val searchQuery: String = "",
    val searchResults: List<ScryfallCard> = emptyList(),
    val isSearching: Boolean = false,
    val cards: List<MtgDeckCard> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false
) {
    val isValid: Boolean get() = deckName.trim().isNotEmpty()

    val totalCards: Int
        get() = cards.filter { it.board == "main" }.sumOf { it.quantity }

    val creatureCount: Int
        get() = cards.filter { it.board == "main" && it.card?.isCreature == true }.sumOf { it.quantity }

    val landCount: Int
        get() = cards.filter { it.board == "main" && it.card?.isLand == true }.sumOf { it.quantity }

    val spellCount: Int
        get() = totalCards - creatureCount - landCount

    val avgCMC: Double
        get() {
            val nonLands = cards.filter { it.board == "main" && it.card?.isLand != true }
            val total = nonLands.sumOf { (it.card?.cmc ?: 0.0) * it.quantity }
            val count = nonLands.sumOf { it.quantity }
            return if (count > 0) total / count else 0.0
        }

    val mainDeckCards: List<MtgDeckCard>
        get() = cards.filter { it.board == "main" }

    val sideboardCards: List<MtgDeckCard>
        get() = cards.filter { it.board == "sideboard" }

    val cardsByType: List<Pair<String, List<MtgDeckCard>>>
        get() {
            val types = listOf(
                "Creatures", "Instants", "Sorceries", "Enchantments",
                "Artifacts", "Planeswalkers", "Lands", "Other"
            )
            return types.mapNotNull { type ->
                val matching = mainDeckCards.filter { it.card?.typeCategory == type }
                if (matching.isNotEmpty()) type to matching else null
            }
        }
}

@HiltViewModel
class DeckBuilderViewModel @Inject constructor(
    private val mtgDeckService: MtgDeckService,
    private val scryfallService: ScryfallService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeckBuilderUiState())
    val uiState: StateFlow<DeckBuilderUiState> = _uiState.asStateFlow()

    private var deckId: String? = null
    private var searchJob: Job? = null

    fun updateDeckName(name: String) {
        _uiState.update { it.copy(deckName = name) }
    }

    fun updateFormatId(formatId: String?) {
        _uiState.update { it.copy(formatId = formatId) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updatePowerLevel(level: Int?) {
        _uiState.update { it.copy(powerLevel = level) }
    }

    fun updateIsPublic(isPublic: Boolean) {
        _uiState.update { it.copy(isPublic = isPublic) }
    }

    fun updateCommanderScryfallId(id: String?) {
        _uiState.update { it.copy(commanderScryfallId = id) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchCards(query)
    }

    // Card Search

    fun searchCards(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isSearching = true) }
            try {
                val results = scryfallService.searchCards(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }

    // Deck Management

    fun addCard(card: ScryfallCard, board: String = "main") {
        _uiState.update { state ->
            val existingIndex = state.cards.indexOfFirst {
                it.scryfallId == card.scryfallId && it.board == board
            }
            if (existingIndex >= 0) {
                val existing = state.cards[existingIndex]
                val updated = existing.copy(quantity = existing.quantity + 1)
                val newCards = state.cards.toMutableList().apply { set(existingIndex, updated) }
                state.copy(cards = newCards)
            } else {
                val newCard = MtgDeckCard(
                    id = UUID.randomUUID().toString(),
                    deckId = deckId,
                    scryfallId = card.scryfallId,
                    quantity = 1,
                    board = board,
                    card = card
                )
                state.copy(cards = state.cards + newCard)
            }
        }
    }

    fun removeCard(index: Int) {
        _uiState.update { state ->
            if (index in state.cards.indices) {
                state.copy(cards = state.cards.toMutableList().apply { removeAt(index) })
            } else state
        }
    }

    fun updateQuantity(index: Int, quantity: Int) {
        _uiState.update { state ->
            if (index !in state.cards.indices) return@update state
            if (quantity <= 0) {
                state.copy(cards = state.cards.toMutableList().apply { removeAt(index) })
            } else {
                val card = state.cards[index]
                val updated = card.copy(quantity = quantity)
                val newCards = state.cards.toMutableList().apply { set(index, updated) }
                state.copy(cards = newCards)
            }
        }
    }

    // Load / Save

    fun loadForEdit(deck: MtgDeck) {
        deckId = deck.id
        _uiState.update {
            it.copy(
                isEditing = true,
                deckName = deck.name,
                formatId = deck.formatId,
                description = deck.description.orEmpty(),
                powerLevel = deck.powerLevel,
                isPublic = deck.isPublic ?: false,
                commanderScryfallId = deck.commanderScryfallId,
                cards = deck.cards ?: emptyList()
            )
        }
    }

    fun saveDeck(onSuccess: (MtgDeck) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val state = _uiState.value

            try {
                if (state.isEditing && deckId != null) {
                    val input = UpdateDeckInput(
                        name = state.deckName,
                        formatId = state.formatId,
                        description = state.description.ifEmpty { null },
                        powerLevel = state.powerLevel,
                        isPublic = state.isPublic,
                        commanderScryfallId = state.commanderScryfallId
                    )
                    val deck = mtgDeckService.updateDeck(deckId!!, input)
                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess(deck)
                } else {
                    val input = CreateDeckInput(
                        name = state.deckName,
                        formatId = state.formatId,
                        description = state.description.ifEmpty { null },
                        powerLevel = state.powerLevel,
                        isPublic = state.isPublic
                    )
                    val deck = mtgDeckService.createDeck(input)
                    deckId = deck.id
                    _uiState.update { it.copy(isEditing = true) }

                    // Add cards to the newly created deck
                    for (card in state.cards) {
                        val cardInput = DeckCardInput(
                            scryfallId = card.scryfallId,
                            quantity = card.quantity,
                            board = card.board
                        )
                        mtgDeckService.addCard(deck.id, cardInput)
                    }

                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess(deck)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isSaving = false) }
            }
        }
    }
}
