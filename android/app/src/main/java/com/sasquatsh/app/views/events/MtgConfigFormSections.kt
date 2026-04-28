package com.sasquatsh.app.views.events

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sasquatsh.app.views.shared.D20SpinnerView
import com.sasquatsh.app.models.MtgConfigState
import com.sasquatsh.app.models.ScryfallCard
import com.sasquatsh.app.services.ScryfallService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── MTG Format Category ───

enum class MtgFormatCategory(val displayName: String) {
    CONSTRUCTED("Constructed"),
    COMMANDER("Commander"),
    LIMITED("Limited"),
    CASUAL("Casual");

    data class Format(val id: String, val name: String)

    val formats: List<Format>
        get() = when (this) {
            CONSTRUCTED -> listOf(
                Format("standard", "Standard"), Format("modern", "Modern"),
                Format("pioneer", "Pioneer"), Format("legacy", "Legacy"),
                Format("vintage", "Vintage"), Format("pauper", "Pauper")
            )
            COMMANDER -> listOf(
                Format("commander", "Commander"), Format("oathbreaker", "Oathbreaker"),
                Format("brawl", "Brawl")
            )
            LIMITED -> listOf(
                Format("draft", "Draft"), Format("sealed", "Sealed"),
                Format("cube", "Cube")
            )
            CASUAL -> listOf(
                Format("casual", "Casual"), Format("custom", "Custom")
            )
        }

    companion object {
        fun categoryFor(formatId: String?): MtgFormatCategory {
            if (formatId == null) return CASUAL
            return entries.firstOrNull { cat -> cat.formats.any { it.id == formatId } } ?: CASUAL
        }
    }
}

// ─── Helper extensions on MtgConfigState ───

private val MtgConfigState.isCommanderFamily: Boolean
    get() = formatId in listOf("commander", "oathbreaker", "brawl")

private val MtgConfigState.isLimitedFormat: Boolean
    get() = formatId in listOf("draft", "sealed", "cube")

private val MtgConfigState.showPowerLevel: Boolean
    get() = isCommanderFamily || formatId == "casual"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MtgConfigFormSections(
    config: MtgConfigState,
    onConfigChange: (MtgConfigState) -> Unit,
    scryfallService: ScryfallService? = null
) {
    val scope = rememberCoroutineScope()

    // Local state for banned card search
    var bannedCardSearch by remember { mutableStateOf("") }
    var bannedCardResults by remember { mutableStateOf<List<ScryfallCard>>(emptyList()) }
    var isSearchingBanned by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var bannedCardImages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Derived format category
    var formatCategory by remember(config.formatId) {
        mutableStateOf(MtgFormatCategory.categoryFor(config.formatId))
    }

    // ─── Format Section ───
    SectionHeader("MTG Format")

    // Category segmented button
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        MtgFormatCategory.entries.forEachIndexed { index, cat ->
            SegmentedButton(
                selected = formatCategory == cat,
                onClick = {
                    formatCategory = cat
                    val first = cat.formats.firstOrNull()
                    onConfigChange(config.copy(formatId = first?.id))
                },
                shape = SegmentedButtonDefaults.itemShape(index, MtgFormatCategory.entries.size)
            ) {
                Text(cat.displayName, style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // Format dropdown
    var formatExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = formatExpanded, onExpandedChange = { formatExpanded = it }) {
        OutlinedTextField(
            value = formatCategory.formats.firstOrNull { it.id == config.formatId }?.name ?: "Select Format",
            onValueChange = {},
            readOnly = true,
            label = { Text("Format") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(formatExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
            formatCategory.formats.forEach { format ->
                DropdownMenuItem(
                    text = { Text(format.name) },
                    onClick = {
                        onConfigChange(config.copy(formatId = format.id))
                        formatExpanded = false
                    }
                )
            }
        }
    }

    // Custom format name
    AnimatedVisibility(visible = config.formatId == "custom") {
        OutlinedTextField(
            value = config.customFormatName ?: "",
            onValueChange = { onConfigChange(config.copy(customFormatName = it)) },
            label = { Text("Custom Format Name") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }

    // ─── Power Level Section ───
    AnimatedVisibility(visible = config.showPowerLevel) {
        Column {
            Spacer(Modifier.height(16.dp))
            SectionHeader("Power Level")

            var powerExpanded by remember { mutableStateOf(false) }
            val powerOptions = listOf(
                "casual" to "Casual (1-4)", "mid" to "Mid (5-6)",
                "high" to "High (7-8)", "cedh" to "cEDH (9-10)", "custom" to "Custom"
            )
            ExposedDropdownMenuBox(expanded = powerExpanded, onExpandedChange = { powerExpanded = it }) {
                OutlinedTextField(
                    value = powerOptions.firstOrNull { it.first == (config.powerLevelRange ?: "mid") }?.second ?: "Mid (5-6)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Power Level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(powerExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = powerExpanded, onDismissRequest = { powerExpanded = false }) {
                    powerOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onConfigChange(config.copy(powerLevelRange = value))
                                powerExpanded = false
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = config.powerLevelRange == "custom") {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    val minVal = (config.powerLevelMin ?: 1).toFloat()
                    Text("Min: ${minVal.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = minVal,
                        onValueChange = { onConfigChange(config.copy(powerLevelMin = it.toInt())) },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                    val maxVal = (config.powerLevelMax ?: 10).toFloat()
                    Text("Max: ${maxVal.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = maxVal,
                        onValueChange = { onConfigChange(config.copy(powerLevelMax = it.toInt())) },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                }
            }
        }
    }

    // ─── Event Structure Section ───
    Spacer(Modifier.height(16.dp))
    SectionHeader("Event Structure")

    var eventTypeExpanded by remember { mutableStateOf(false) }
    val eventTypes = listOf(
        "casual" to "Casual", "pods" to "Pods", "swiss" to "Swiss",
        "single_elim" to "Single Elimination", "double_elim" to "Double Elimination",
        "round_robin" to "Round Robin"
    )
    ExposedDropdownMenuBox(expanded = eventTypeExpanded, onExpandedChange = { eventTypeExpanded = it }) {
        OutlinedTextField(
            value = eventTypes.firstOrNull { it.first == (config.eventType ?: "casual") }?.second ?: "Casual",
            onValueChange = {},
            readOnly = true,
            label = { Text("Event Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(eventTypeExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = eventTypeExpanded, onDismissRequest = { eventTypeExpanded = false }) {
            eventTypes.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onConfigChange(config.copy(eventType = value))
                        eventTypeExpanded = false
                    }
                )
            }
        }
    }

    AnimatedVisibility(visible = config.eventType != null && config.eventType != "casual") {
        Column {
            Spacer(Modifier.height(8.dp))

            // Play Mode
            var playModeExpanded by remember { mutableStateOf(false) }
            val playModes = listOf(
                "open_play" to "Open Play", "assigned_pods" to "Assigned Pods",
                "tournament_pairings" to "Tournament Pairings"
            )
            ExposedDropdownMenuBox(expanded = playModeExpanded, onExpandedChange = { playModeExpanded = it }) {
                OutlinedTextField(
                    value = playModes.firstOrNull { it.first == (config.playMode ?: "open_play") }?.second ?: "Open Play",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Play Mode") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(playModeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = playModeExpanded, onDismissRequest = { playModeExpanded = false }) {
                    playModes.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onConfigChange(config.copy(playMode = value))
                                playModeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Match Style segmented
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val matchStyles = listOf("bo1" to "Best of 1", "bo3" to "Best of 3")
                matchStyles.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = (config.matchStyle ?: "bo1") == value,
                        onClick = { onConfigChange(config.copy(matchStyle = value)) },
                        shape = SegmentedButtonDefaults.itemShape(index, matchStyles.size)
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Round time slider
            val roundTime = (config.roundTimeMinutes ?: 50).toFloat()
            Text("Round Time: ${roundTime.toInt()} min", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = roundTime,
                onValueChange = { onConfigChange(config.copy(roundTimeMinutes = it.toInt())) },
                valueRange = 10f..180f,
                steps = 33
            )

            // Rounds count
            OutlinedTextField(
                value = config.roundsCount?.toString() ?: "",
                onValueChange = { onConfigChange(config.copy(roundsCount = it.toIntOrNull())) },
                label = { Text("Rounds") },
                placeholder = { Text("Auto") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Pods size
    AnimatedVisibility(visible = config.eventType == "pods") {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            val podSize = (config.podsSize ?: 4).toFloat()
            Text("Pod Size: ${podSize.toInt()}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = podSize,
                onValueChange = { onConfigChange(config.copy(podsSize = it.toInt())) },
                valueRange = 2f..8f,
                steps = 5
            )
        }
    }

    // Top cut
    AnimatedVisibility(
        visible = config.eventType in listOf("swiss", "single_elim", "double_elim")
    ) {
        OutlinedTextField(
            value = config.topCut?.toString() ?: "",
            onValueChange = { onConfigChange(config.copy(topCut = it.toIntOrNull())) },
            label = { Text("Top Cut") },
            placeholder = { Text("None") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }

    // ─── Deck Rules Section ───
    Spacer(Modifier.height(16.dp))
    SectionHeader("Deck Rules")

    SwitchRow("Allow Proxies", config.allowProxies == true) {
        onConfigChange(config.copy(allowProxies = it))
    }

    AnimatedVisibility(visible = config.allowProxies == true) {
        OutlinedTextField(
            value = config.proxyLimit?.toString() ?: "",
            onValueChange = { onConfigChange(config.copy(proxyLimit = it.toIntOrNull())) },
            label = { Text("Proxy Limit") },
            placeholder = { Text("Unlimited") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }

    // Banned cards search
    Spacer(Modifier.height(8.dp))
    Text("Banned Cards", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(4.dp))

    OutlinedTextField(
        value = bannedCardSearch,
        onValueChange = { query ->
            bannedCardSearch = query
            searchJob?.cancel()
            if (query.length < 2) {
                bannedCardResults = emptyList()
                return@OutlinedTextField
            }
            searchJob = scope.launch {
                delay(300)
                isSearchingBanned = true
                try {
                    bannedCardResults = scryfallService?.searchCards(query) ?: emptyList()
                } catch (_: Exception) {
                    bannedCardResults = emptyList()
                }
                isSearchingBanned = false
            }
        },
        placeholder = { Text("Search cards to ban...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (isSearchingBanned) {
                D20SpinnerView(size = 20.dp, modifier = Modifier.size(20.dp))
            } else if (bannedCardSearch.isNotEmpty()) {
                IconButton(onClick = {
                    bannedCardSearch = ""
                    bannedCardResults = emptyList()
                }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    // Search results
    bannedCardResults.take(8).forEach { card ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val name = card.name
                    val currentBanned = config.bannedCards?.toMutableList() ?: mutableListOf()
                    if (!currentBanned.contains(name)) {
                        currentBanned.add(name)
                        onConfigChange(config.copy(bannedCards = currentBanned))
                        card.smallImageUrl?.let { url ->
                            bannedCardImages = bannedCardImages + (name to url)
                        }
                    }
                    bannedCardSearch = ""
                    bannedCardResults = emptyList()
                }
                .padding(vertical = 4.dp)
        ) {
            card.smallImageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = card.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(28.dp, 40.dp).clip(RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(card.name, style = MaterialTheme.typography.bodySmall)
                card.typeLine?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.Add, contentDescription = "Ban card",
                tint = MaterialTheme.colorScheme.error)
        }
    }

    // Banned list
    config.bannedCards?.forEachIndexed { index, card ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
        ) {
            val url = bannedCardImages[card]
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = card,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(22.dp, 30.dp).clip(RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(card, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val updated = config.bannedCards?.toMutableList() ?: return@IconButton
                updated.removeAt(index)
                onConfigChange(config.copy(bannedCards = updated))
            }) {
                Icon(Icons.Default.Close, contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp))
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    SwitchRow("Require Deck Registration", config.requireDeckRegistration == true) {
        onConfigChange(config.copy(requireDeckRegistration = it))
    }

    // Deck submission deadline would need a date picker - simplified to text field
    AnimatedVisibility(visible = config.requireDeckRegistration == true) {
        OutlinedTextField(
            value = config.deckSubmissionDeadline ?: "",
            onValueChange = { onConfigChange(config.copy(deckSubmissionDeadline = it)) },
            label = { Text("Submission Deadline") },
            placeholder = { Text("YYYY-MM-DD HH:MM") },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }

    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = config.houseRulesNotes ?: "",
        onValueChange = { onConfigChange(config.copy(houseRulesNotes = it)) },
        label = { Text("House Rules") },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth()
    )

    // ─── Draft / Sealed Section ───
    AnimatedVisibility(visible = config.isLimitedFormat) {
        Column {
            Spacer(Modifier.height(16.dp))
            SectionHeader("Draft / Sealed")

            val packs = (config.packsPerPlayer ?: 3).toFloat()
            Text("Packs Per Player: ${packs.toInt()}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = packs,
                onValueChange = { onConfigChange(config.copy(packsPerPlayer = it.toInt())) },
                valueRange = 1f..6f,
                steps = 4
            )

            var draftStyleExpanded by remember { mutableStateOf(false) }
            val draftStyles = listOf(
                "standard" to "Standard", "rochester" to "Rochester",
                "winston" to "Winston", "grid" to "Grid"
            )
            ExposedDropdownMenuBox(expanded = draftStyleExpanded, onExpandedChange = { draftStyleExpanded = it }) {
                OutlinedTextField(
                    value = draftStyles.firstOrNull { it.first == (config.draftStyle ?: "standard") }?.second ?: "Standard",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Draft Style") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(draftStyleExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = draftStyleExpanded, onDismissRequest = { draftStyleExpanded = false }) {
                    draftStyles.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onConfigChange(config.copy(draftStyle = value))
                                draftStyleExpanded = false
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = config.formatId == "cube") {
                OutlinedTextField(
                    value = config.cubeId ?: "",
                    onValueChange = { onConfigChange(config.copy(cubeId = it)) },
                    label = { Text("Cube ID / Link") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }

    // ─── Prizes & Entry Section ───
    Spacer(Modifier.height(16.dp))
    SectionHeader("Prizes & Entry")

    SwitchRow("Has Prizes", config.hasPrizes == true) {
        onConfigChange(config.copy(hasPrizes = it))
    }

    AnimatedVisibility(visible = config.hasPrizes == true) {
        OutlinedTextField(
            value = config.prizeStructure ?: "",
            onValueChange = { onConfigChange(config.copy(prizeStructure = it)) },
            label = { Text("Prize Structure") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }

    Spacer(Modifier.height(8.dp))
    EntryFeeRow(
        entryFee = config.entryFee?.toString() ?: "",
        currency = config.entryFeeCurrency ?: "USD",
        onFeeChange = { onConfigChange(config.copy(entryFee = it.toDoubleOrNull())) },
        onCurrencyChange = { onConfigChange(config.copy(entryFeeCurrency = it)) }
    )

    Spacer(Modifier.height(8.dp))
    SwitchRow("Allow Spectators", config.allowSpectators != false) {
        onConfigChange(config.copy(allowSpectators = it))
    }
}

// ─── Shared UI helpers ───

@Composable
internal fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
internal fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntryFeeRow(
    entryFee: String,
    currency: String,
    onFeeChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = entryFee,
            onValueChange = onFeeChange,
            label = { Text("Entry Fee") },
            placeholder = { Text("Free") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )

        var currencyExpanded by remember { mutableStateOf(false) }
        val currencies = listOf("USD", "EUR", "GBP", "CAD")
        ExposedDropdownMenuBox(
            expanded = currencyExpanded,
            onExpandedChange = { currencyExpanded = it },
            modifier = Modifier.width(100.dp)
        ) {
            OutlinedTextField(
                value = currency,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(currencyExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = currencyExpanded, onDismissRequest = { currencyExpanded = false }) {
                currencies.forEach { cur ->
                    DropdownMenuItem(
                        text = { Text(cur) },
                        onClick = {
                            onCurrencyChange(cur)
                            currencyExpanded = false
                        }
                    )
                }
            }
        }
    }
}
