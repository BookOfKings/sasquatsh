package com.sasquatsh.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sasquatsh.app.models.AddEventGameInput
import com.sasquatsh.app.models.AppTimezone
import com.sasquatsh.app.models.BggGame
import com.sasquatsh.app.models.BggSearchResult
import com.sasquatsh.app.models.CreateEventInput
import com.sasquatsh.app.models.DifficultyLevel
import com.sasquatsh.app.models.Event
import com.sasquatsh.app.models.EventLocation
import com.sasquatsh.app.models.EventStatus
import com.sasquatsh.app.models.GameCategory
import com.sasquatsh.app.models.GameSystem
import com.sasquatsh.app.models.GroupSummary
import com.sasquatsh.app.models.MemberRole
import com.sasquatsh.app.models.MtgConfigState
import com.sasquatsh.app.models.PokemonConfigState
import com.sasquatsh.app.models.UpdateEventInput
import com.sasquatsh.app.models.Warhammer40kConfigState
import com.sasquatsh.app.models.YugiohConfigState
import com.sasquatsh.app.services.BggService
import com.sasquatsh.app.services.EventsService
import com.sasquatsh.app.services.GroupsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class CreateEditEventUiState(
    val title: String = "",
    val description: String = "",
    val gameSystem: GameSystem = GameSystem.BOARD_GAME,
    val gameCategory: GameCategory? = null,
    val eventDate: Date = Date(),
    val startTime: Date = Date(),
    val durationMinutes: Int = 120,
    val setupMinutes: Int = 15,
    val addressLine1: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val locationDetails: String = "",
    val eventLocationId: String? = null,
    val venueHall: String? = null,
    val venueRoom: String? = null,
    val venueTable: String? = null,
    val timezone: AppTimezone = AppTimezone.EASTERN,
    val hostIsPlaying: Boolean = true,
    val selectedVenue: EventLocation? = null,
    val useVenueMode: Boolean = true,
    val difficultyLevel: DifficultyLevel? = null,
    val maxPlayers: Int = 6,
    val isPublic: Boolean = true,
    val isCharityEvent: Boolean = false,
    val minAge: Int? = null,
    val status: EventStatus = EventStatus.PUBLISHED,
    val groupId: String? = null,
    val mtgConfig: MtgConfigState? = null,
    val pokemonConfig: PokemonConfigState? = null,
    val yugiohConfig: YugiohConfigState? = null,
    val warhammer40kConfig: Warhammer40kConfigState? = null,
    val selectedGames: List<BggGame> = emptyList(),
    val isFetchingGameDetails: Boolean = false,
    val availableGroups: List<GroupSummary> = emptyList(),
    val bggSearchResults: List<BggSearchResult> = emptyList(),
    val isSearchingBGG: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false
) {
    val isBoardGame: Boolean get() = gameSystem == GameSystem.BOARD_GAME

    val derivedGameTitle: String? get() = selectedGames.firstOrNull()?.name

    val isValid: Boolean get() = validationIssues.isEmpty()

    val validationIssues: List<String>
        get() {
            val issues = mutableListOf<String>()
            if (title.trim().isEmpty()) {
                issues.add("Title is required")
            }
            if (isBoardGame && selectedGames.isEmpty() && !isEditing) {
                issues.add("Select at least one game")
            }
            return issues
        }
}

@HiltViewModel
class CreateEditEventViewModel @Inject constructor(
    private val eventsService: EventsService,
    private val groupsService: GroupsService,
    private val bggService: BggService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditEventUiState())
    val uiState: StateFlow<CreateEditEventUiState> = _uiState.asStateFlow()

    private var eventId: String? = null
    private var bggSearchJob: Job? = null

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateGameSystem(gameSystem: GameSystem) {
        _uiState.update { it.copy(gameSystem = gameSystem) }
        gameSystemDidChange()
    }

    fun updateGameCategory(category: GameCategory?) {
        _uiState.update { it.copy(gameCategory = category) }
    }

    fun updateEventDate(date: Date) {
        _uiState.update { it.copy(eventDate = date) }
    }

    fun updateStartTime(time: Date) {
        _uiState.update { it.copy(startTime = time) }
    }

    fun updateDurationMinutes(minutes: Int) {
        _uiState.update { it.copy(durationMinutes = minutes) }
    }

    fun updateSetupMinutes(minutes: Int) {
        _uiState.update { it.copy(setupMinutes = minutes) }
    }

    fun updateAddressLine1(address: String) {
        _uiState.update { it.copy(addressLine1 = address) }
    }

    fun updateCity(city: String) {
        _uiState.update { it.copy(city = city) }
    }

    fun updateState(state: String) {
        _uiState.update { it.copy(state = state) }
    }

    fun updatePostalCode(postalCode: String) {
        _uiState.update { it.copy(postalCode = postalCode) }
    }

    fun updateLocationDetails(details: String) {
        _uiState.update { it.copy(locationDetails = details) }
    }

    fun updateVenueHall(hall: String?) {
        _uiState.update { it.copy(venueHall = hall) }
    }

    fun updateVenueRoom(room: String?) {
        _uiState.update { it.copy(venueRoom = room) }
    }

    fun updateVenueTable(table: String?) {
        _uiState.update { it.copy(venueTable = table) }
    }

    fun updateTimezone(timezone: AppTimezone) {
        _uiState.update { it.copy(timezone = timezone) }
    }

    fun updateHostIsPlaying(playing: Boolean) {
        _uiState.update { it.copy(hostIsPlaying = playing) }
    }

    fun updateDifficultyLevel(level: DifficultyLevel?) {
        _uiState.update { it.copy(difficultyLevel = level) }
    }

    fun updateMaxPlayers(max: Int) {
        _uiState.update { it.copy(maxPlayers = max) }
    }

    fun updateIsPublic(isPublic: Boolean) {
        _uiState.update { it.copy(isPublic = isPublic) }
    }

    fun updateIsCharityEvent(isCharity: Boolean) {
        _uiState.update { it.copy(isCharityEvent = isCharity) }
    }

    fun updateMinAge(age: Int?) {
        _uiState.update { it.copy(minAge = age) }
    }

    fun updateStatus(status: EventStatus) {
        _uiState.update { it.copy(status = status) }
    }

    fun updateGroupId(groupId: String?) {
        _uiState.update { it.copy(groupId = groupId) }
    }

    fun updateMtgConfig(config: MtgConfigState?) {
        _uiState.update { it.copy(mtgConfig = config) }
    }

    fun updatePokemonConfig(config: PokemonConfigState?) {
        _uiState.update { it.copy(pokemonConfig = config) }
    }

    fun updateYugiohConfig(config: YugiohConfigState?) {
        _uiState.update { it.copy(yugiohConfig = config) }
    }

    fun updateWarhammer40kConfig(config: Warhammer40kConfigState?) {
        _uiState.update { it.copy(warhammer40kConfig = config) }
    }

    fun loadAvailableGroups() {
        viewModelScope.launch {
            try {
                val groups = groupsService.getMyGroups()
                val manageable = groups.filter {
                    it.userRole == MemberRole.OWNER || it.userRole == MemberRole.ADMIN
                }
                _uiState.update { it.copy(availableGroups = manageable) }
            } catch (_: Exception) {
                _uiState.update { it.copy(availableGroups = emptyList()) }
            }
        }
    }

    fun selectVenue(venue: EventLocation) {
        _uiState.update {
            it.copy(
                selectedVenue = venue,
                eventLocationId = venue.id,
                city = venue.city,
                state = venue.state,
                useVenueMode = true,
                timezone = venue.timezone?.let { tz ->
                    AppTimezone.fromValue(tz)
                } ?: it.timezone
            )
        }
    }

    fun clearVenue() {
        _uiState.update {
            it.copy(
                selectedVenue = null,
                eventLocationId = null,
                venueHall = null,
                venueRoom = null,
                venueTable = null
            )
        }
    }

    fun switchToCustomAddress() {
        clearVenue()
        _uiState.update { it.copy(useVenueMode = false) }
    }

    fun addGame(searchResult: BggSearchResult) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingGameDetails = true) }
            try {
                val game = bggService.getGameDetails(searchResult.bggId)
                _uiState.update {
                    it.copy(
                        selectedGames = it.selectedGames + game,
                        bggSearchResults = emptyList(),
                        isFetchingGameDetails = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to load game details", isFetchingGameDetails = false)
                }
            }
        }
    }

    fun removeGame(index: Int) {
        _uiState.update {
            if (index in it.selectedGames.indices) {
                it.copy(selectedGames = it.selectedGames.toMutableList().apply { removeAt(index) })
            } else it
        }
    }

    fun setPrimaryGame(index: Int) {
        _uiState.update {
            if (index in it.selectedGames.indices && index != 0) {
                val games = it.selectedGames.toMutableList()
                val game = games.removeAt(index)
                games.add(0, game)
                it.copy(selectedGames = games)
            } else it
        }
    }

    fun loadForEdit(event: Event) {
        eventId = event.id
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)

        _uiState.update {
            it.copy(
                isEditing = true,
                title = event.title,
                description = event.description.orEmpty(),
                gameSystem = event.gameSystem ?: GameSystem.BOARD_GAME,
                gameCategory = event.gameCategory?.let { cat -> GameCategory.fromValue(cat) },
                eventDate = parseDate(event.eventDate) ?: Date(),
                startTime = event.startTime?.let { t -> timeFormatter.parse(t) } ?: Date(),
                durationMinutes = event.durationMinutes ?: 60,
                setupMinutes = event.setupMinutes ?: 0,
                addressLine1 = event.addressLine1.orEmpty(),
                city = event.city.orEmpty(),
                state = event.state.orEmpty(),
                postalCode = event.postalCode.orEmpty(),
                locationDetails = event.locationDetails.orEmpty(),
                eventLocationId = event.eventLocationId,
                venueHall = event.venueHall,
                venueRoom = event.venueRoom,
                venueTable = event.venueTable,
                timezone = event.timezone?.let { tz -> AppTimezone.fromValue(tz) } ?: AppTimezone.EASTERN,
                hostIsPlaying = event.hostIsPlaying ?: true,
                useVenueMode = event.eventLocationId != null,
                difficultyLevel = event.difficultyLevel?.let { d -> DifficultyLevel.fromValue(d) },
                maxPlayers = event.maxPlayers ?: 8,
                isPublic = event.isPublic,
                isCharityEvent = event.isCharityEvent,
                minAge = event.minAge,
                status = EventStatus.fromValue(event.status) ?: EventStatus.PUBLISHED,
                mtgConfig = event.mtgConfig?.let { c -> MtgConfigState.fromConfig(c) },
                pokemonConfig = event.pokemonConfig?.let { c -> PokemonConfigState.fromConfig(c) },
                yugiohConfig = event.yugiohConfig?.let { c -> YugiohConfigState.fromConfig(c) },
                warhammer40kConfig = event.warhammer40kConfig?.let { c -> Warhammer40kConfigState.fromConfig(c) }
            )
        }
    }

    fun save(onSuccess: (Event) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val state = _uiState.value
            val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            try {
                if (state.isEditing && eventId != null) {
                    val input = UpdateEventInput(
                        title = state.title,
                        description = state.description.ifEmpty { null },
                        gameTitle = state.derivedGameTitle,
                        gameCategory = state.gameCategory?.value,
                        gameSystem = state.gameSystem.value,
                        eventDate = dateFormatter.format(state.eventDate),
                        startTime = timeFormatter.format(state.startTime),
                        durationMinutes = state.durationMinutes,
                        setupMinutes = state.setupMinutes,
                        addressLine1 = state.addressLine1.ifEmpty { null },
                        city = state.city.ifEmpty { null },
                        state = state.state.ifEmpty { null },
                        postalCode = state.postalCode.ifEmpty { null },
                        locationDetails = state.locationDetails.ifEmpty { null },
                        eventLocationId = state.eventLocationId,
                        venueHall = state.venueHall,
                        venueRoom = state.venueRoom,
                        venueTable = state.venueTable,
                        timezone = state.timezone.value,
                        hostIsPlaying = state.hostIsPlaying,
                        difficultyLevel = state.difficultyLevel?.value,
                        maxPlayers = state.maxPlayers,
                        isPublic = state.isPublic,
                        isCharityEvent = state.isCharityEvent,
                        minAge = state.minAge,
                        status = state.status.value,
                        mtgConfig = state.mtgConfig?.toInput(),
                        pokemonConfig = state.pokemonConfig?.toInput(),
                        yugiohConfig = state.yugiohConfig?.toInput(),
                        warhammer40kConfig = state.warhammer40kConfig?.toInput()
                    )
                    val event = eventsService.updateEvent(eventId!!, input)
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(event)
                } else {
                    val input = CreateEventInput(
                        title = state.title,
                        description = state.description.ifEmpty { null },
                        gameTitle = state.derivedGameTitle,
                        gameCategory = state.gameCategory?.value,
                        gameSystem = state.gameSystem.value,
                        eventDate = dateFormatter.format(state.eventDate),
                        startTime = timeFormatter.format(state.startTime),
                        durationMinutes = state.durationMinutes,
                        setupMinutes = state.setupMinutes,
                        addressLine1 = state.addressLine1.ifEmpty { null },
                        city = state.city.ifEmpty { null },
                        state = state.state.ifEmpty { null },
                        postalCode = state.postalCode.ifEmpty { null },
                        locationDetails = state.locationDetails.ifEmpty { null },
                        eventLocationId = state.eventLocationId,
                        venueHall = state.venueHall,
                        venueRoom = state.venueRoom,
                        venueTable = state.venueTable,
                        timezone = state.timezone.value,
                        hostIsPlaying = state.hostIsPlaying,
                        difficultyLevel = state.difficultyLevel?.value,
                        maxPlayers = state.maxPlayers,
                        isPublic = state.isPublic,
                        isCharityEvent = state.isCharityEvent,
                        minAge = state.minAge,
                        status = state.status.value,
                        groupId = state.groupId,
                        mtgConfig = state.mtgConfig?.toInput(),
                        pokemonConfig = state.pokemonConfig?.toInput(),
                        yugiohConfig = state.yugiohConfig?.toInput(),
                        warhammer40kConfig = state.warhammer40kConfig?.toInput()
                    )
                    val event = eventsService.createEvent(input)

                    // Add games to the created event
                    for ((index, game) in state.selectedGames.withIndex()) {
                        val gameInput = AddEventGameInput(
                            eventId = event.id,
                            bggId = game.bggId,
                            gameName = game.name,
                            thumbnailUrl = game.thumbnailUrl,
                            minPlayers = game.minPlayers,
                            maxPlayers = game.maxPlayers,
                            playingTime = game.playingTime,
                            isPrimary = index == 0,
                            isAlternative = index != 0
                        )
                        eventsService.addGame(gameInput)
                    }

                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess(event)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    fun searchBGG(query: String) {
        bggSearchJob?.cancel()
        if (query.length < 2) {
            _uiState.update { it.copy(bggSearchResults = emptyList()) }
            return
        }
        bggSearchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isSearchingBGG = true) }
            try {
                val results = bggService.searchGames(query)
                _uiState.update { it.copy(bggSearchResults = results, isSearchingBGG = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(bggSearchResults = emptyList(), isSearchingBGG = false) }
            }
        }
    }

    fun clearBGGSearch() {
        bggSearchJob?.cancel()
        _uiState.update { it.copy(bggSearchResults = emptyList()) }
    }

    private fun gameSystemDidChange() {
        _uiState.update { state ->
            var updated = state.copy(
                mtgConfig = null,
                pokemonConfig = null,
                yugiohConfig = null,
                warhammer40kConfig = null
            )
            when (state.gameSystem) {
                GameSystem.BOARD_GAME -> { /* no config */ }
                GameSystem.MTG -> updated = updated.copy(
                    mtgConfig = MtgConfigState(),
                    durationMinutes = 180,
                    maxPlayers = 8
                )
                GameSystem.POKEMON_TCG -> updated = updated.copy(
                    pokemonConfig = PokemonConfigState(),
                    hostIsPlaying = false,
                    maxPlayers = 16,
                    durationMinutes = 240
                )
                GameSystem.YUGIOH -> updated = updated.copy(
                    yugiohConfig = YugiohConfigState(),
                    hostIsPlaying = false,
                    maxPlayers = 16,
                    durationMinutes = 240
                )
                GameSystem.WARHAMMER_40K -> updated = updated.copy(
                    warhammer40kConfig = Warhammer40kConfigState(),
                    hostIsPlaying = false,
                    maxPlayers = 8,
                    durationMinutes = 180,
                    setupMinutes = 30
                )
            }
            updated
        }
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString)
        } catch (_: Exception) {
            null
        }
    }
}
