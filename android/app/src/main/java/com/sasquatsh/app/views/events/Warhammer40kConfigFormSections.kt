package com.sasquatsh.app.views.events

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sasquatsh.app.models.Warhammer40kConfigState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Warhammer40kConfigFormSections(
    config: Warhammer40kConfigState,
    onConfigChange: (Warhammer40kConfigState) -> Unit
) {
    // ─── Game Setup Section ───
    SectionHeader("Game Setup")

    // Game Type
    var gameTypeExpanded by remember { mutableStateOf(false) }
    val gameTypes = listOf(
        "matched" to "Matched Play", "narrative" to "Narrative",
        "crusade" to "Crusade", "open" to "Open Play"
    )
    ExposedDropdownMenuBox(expanded = gameTypeExpanded, onExpandedChange = { gameTypeExpanded = it }) {
        OutlinedTextField(
            value = gameTypes.firstOrNull { it.first == (config.gameType ?: "matched") }?.second ?: "Matched Play",
            onValueChange = {},
            readOnly = true,
            label = { Text("Game Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(gameTypeExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = gameTypeExpanded, onDismissRequest = { gameTypeExpanded = false }) {
            gameTypes.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onConfigChange(config.copy(gameType = value))
                        gameTypeExpanded = false
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // Points Limit - quick-select chips
    Text("Points Limit", style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(500, 1000, 1500, 2000, 2500, 3000).forEach { pts ->
            FilterChip(
                selected = config.pointsLimit == pts,
                onClick = { onConfigChange(config.copy(pointsLimit = pts)) },
                label = { Text("$pts") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }

    Spacer(Modifier.height(4.dp))
    val pointsVal = (config.pointsLimit ?: 2000).toFloat()
    Text("Points: ${pointsVal.toInt()}", style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = pointsVal,
        onValueChange = { onConfigChange(config.copy(pointsLimit = (it / 100).toInt() * 100)) },
        valueRange = 100f..5000f
    )

    Spacer(Modifier.height(8.dp))

    // Player Mode segmented
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        val playerModes = listOf("1v1" to "1v1", "2v2" to "2v2", "group" to "Group")
        playerModes.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = (config.playerMode ?: "1v1") == value,
                onClick = { onConfigChange(config.copy(playerMode = value)) },
                shape = SegmentedButtonDefaults.itemShape(index, playerModes.size)
            ) {
                Text(label)
            }
        }
    }

    // ─── Mission Section ───
    Spacer(Modifier.height(16.dp))
    SectionHeader("Mission")

    var missionPackExpanded by remember { mutableStateOf(false) }
    val missionPacks = listOf(
        "" to "None", "leviathan" to "Leviathan", "pariah_nexus" to "Pariah Nexus",
        "chapter_approved" to "Chapter Approved", "crusade" to "Crusade", "custom" to "Custom"
    )
    ExposedDropdownMenuBox(expanded = missionPackExpanded, onExpandedChange = { missionPackExpanded = it }) {
        OutlinedTextField(
            value = missionPacks.firstOrNull { it.first == (config.missionPack ?: "") }?.second ?: "None",
            onValueChange = {},
            readOnly = true,
            label = { Text("Mission Pack") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(missionPackExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = missionPackExpanded, onDismissRequest = { missionPackExpanded = false }) {
            missionPacks.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onConfigChange(config.copy(missionPack = value))
                        missionPackExpanded = false
                    }
                )
            }
        }
    }

    AnimatedVisibility(visible = !config.missionPack.isNullOrEmpty()) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            var missionSelExpanded by remember { mutableStateOf(false) }
            val missionSelections = listOf("random" to "Random", "pre_selected" to "Pre-Selected")
            ExposedDropdownMenuBox(expanded = missionSelExpanded, onExpandedChange = { missionSelExpanded = it }) {
                OutlinedTextField(
                    value = missionSelections.firstOrNull { it.first == (config.missionSelection ?: "random") }?.second ?: "Random",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Mission Selection") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(missionSelExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = missionSelExpanded, onDismissRequest = { missionSelExpanded = false }) {
                    missionSelections.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onConfigChange(config.copy(missionSelection = value))
                                missionSelExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = config.missionNotes ?: "",
        onValueChange = { onConfigChange(config.copy(missionNotes = it)) },
        label = { Text("Mission Notes") },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth()
    )

    // ─── Army Rules Section ───
    Spacer(Modifier.height(16.dp))
    SectionHeader("Army Rules")

    SwitchRow("Battle Ready Required", config.battleReadyRequired == true) {
        onConfigChange(config.copy(battleReadyRequired = it))
    }
    SwitchRow("WYSIWYG Required", config.wysiwygRequired == true) {
        onConfigChange(config.copy(wysiwygRequired = it))
    }
    SwitchRow("Forge World Allowed", config.forgeWorldAllowed != false) {
        onConfigChange(config.copy(forgeWorldAllowed = it))
    }
    SwitchRow("Legends Allowed", config.legendsAllowed == true) {
        onConfigChange(config.copy(legendsAllowed = it))
    }
    SwitchRow("Allow Proxies", config.allowProxies == true) {
        onConfigChange(config.copy(allowProxies = it))
    }

    AnimatedVisibility(visible = config.allowProxies == true) {
        OutlinedTextField(
            value = config.proxyNotes ?: "",
            onValueChange = { onConfigChange(config.copy(proxyNotes = it)) },
            label = { Text("Proxy Notes") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }

    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = config.armyRulesNotes ?: "",
        onValueChange = { onConfigChange(config.copy(armyRulesNotes = it)) },
        label = { Text("Army Rules Notes") },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth()
    )

    // ─── Terrain & Table Section ───
    Spacer(Modifier.height(16.dp))
    SectionHeader("Terrain & Table")

    var terrainExpanded by remember { mutableStateOf(false) }
    val terrainTypes = listOf(
        "tournament" to "Tournament Standard", "casual" to "Casual",
        "bring_your_own" to "Bring Your Own"
    )
    ExposedDropdownMenuBox(expanded = terrainExpanded, onExpandedChange = { terrainExpanded = it }) {
        OutlinedTextField(
            value = terrainTypes.firstOrNull { it.first == (config.terrainType ?: "tournament") }?.second ?: "Tournament Standard",
            onValueChange = {},
            readOnly = true,
            label = { Text("Terrain Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(terrainExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = terrainExpanded, onDismissRequest = { terrainExpanded = false }) {
            terrainTypes.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onConfigChange(config.copy(terrainType = value))
                        terrainExpanded = false
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    var tableSizeExpanded by remember { mutableStateOf(false) }
    val tableSizes = listOf(
        "44x30" to "44\" x 30\" (Combat Patrol)",
        "44x60" to "44\" x 60\" (Standard)",
        "custom" to "Custom"
    )
    ExposedDropdownMenuBox(expanded = tableSizeExpanded, onExpandedChange = { tableSizeExpanded = it }) {
        OutlinedTextField(
            value = tableSizes.firstOrNull { it.first == (config.tableSize ?: "44x60") }?.second ?: "44\" x 60\" (Standard)",
            onValueChange = {},
            readOnly = true,
            label = { Text("Table Size") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tableSizeExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = tableSizeExpanded, onDismissRequest = { tableSizeExpanded = false }) {
            tableSizes.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onConfigChange(config.copy(tableSize = value))
                        tableSizeExpanded = false
                    }
                )
            }
        }
    }

    // ─── Event Structure Section ───
    Spacer(Modifier.height(16.dp))
    SectionHeader("Event Structure")

    var eventTypeExpanded by remember { mutableStateOf(false) }
    val eventTypes = listOf(
        "casual" to "Casual", "tournament" to "Tournament",
        "campaign" to "Campaign", "league" to "League"
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

    AnimatedVisibility(visible = config.eventType == "tournament" || config.eventType == "league") {
        Column {
            Spacer(Modifier.height(8.dp))

            // Tournament Style
            var styleExpanded by remember { mutableStateOf(false) }
            val styles = listOf(
                "swiss" to "Swiss", "single_elimination" to "Single Elimination",
                "round_robin" to "Round Robin"
            )
            ExposedDropdownMenuBox(expanded = styleExpanded, onExpandedChange = { styleExpanded = it }) {
                OutlinedTextField(
                    value = styles.firstOrNull { it.first == (config.tournamentStyle ?: "swiss") }?.second ?: "Swiss",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tournament Style") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(styleExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = styleExpanded, onDismissRequest = { styleExpanded = false }) {
                    styles.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onConfigChange(config.copy(tournamentStyle = value))
                                styleExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Rounds
            OutlinedTextField(
                value = config.roundsCount?.toString() ?: "",
                onValueChange = { onConfigChange(config.copy(roundsCount = it.toIntOrNull())) },
                label = { Text("Rounds") },
                placeholder = { Text("Auto") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Round time
            val roundTime = (config.roundTimeMinutes ?: 150).toFloat()
            Text("Round Time: ${roundTime.toInt()} min", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = roundTime,
                onValueChange = { onConfigChange(config.copy(roundTimeMinutes = it.toInt())) },
                valueRange = 30f..300f,
                steps = 17
            )

            SwitchRow("Include Top Cut", config.includeTopCut == true) {
                onConfigChange(config.copy(includeTopCut = it))
            }

            // Scoring
            var scoringExpanded by remember { mutableStateOf(false) }
            val scoringTypes = listOf(
                "win_loss" to "Win/Loss", "win_draw_loss" to "Win/Draw/Loss",
                "battle_points" to "Battle Points"
            )
            ExposedDropdownMenuBox(expanded = scoringExpanded, onExpandedChange = { scoringExpanded = it }) {
                OutlinedTextField(
                    value = scoringTypes.firstOrNull { it.first == (config.scoringType ?: "win_loss") }?.second ?: "Win/Loss",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Scoring") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(scoringExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = scoringExpanded, onDismissRequest = { scoringExpanded = false }) {
                    scoringTypes.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onConfigChange(config.copy(scoringType = value))
                                scoringExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }

    // ─── Crusade Settings Section ───
    AnimatedVisibility(visible = config.gameType == "crusade") {
        Column {
            Spacer(Modifier.height(16.dp))
            SectionHeader("Crusade Settings")

            val supplyLimit = (config.startingSupplyLimit ?: 1000).toFloat()
            Text("Starting Supply Limit: ${supplyLimit.toInt()}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = supplyLimit,
                onValueChange = { onConfigChange(config.copy(startingSupplyLimit = (it / 250).toInt() * 250)) },
                valueRange = 500f..3000f,
                steps = 9
            )

            val crusadePoints = (config.startingCrusadePoints ?: 0).toFloat()
            Text("Starting Crusade Points: ${crusadePoints.toInt()}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = crusadePoints,
                onValueChange = { onConfigChange(config.copy(startingCrusadePoints = it.toInt())) },
                valueRange = 0f..20f,
                steps = 19
            )

            OutlinedTextField(
                value = config.crusadeProgressionNotes ?: "",
                onValueChange = { onConfigChange(config.copy(crusadeProgressionNotes = it)) },
                label = { Text("Progression Notes") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // ─── Army List Submission Section ───
    AnimatedVisibility(
        visible = config.eventType == "tournament" || config.eventType == "league"
    ) {
        Column {
            Spacer(Modifier.height(16.dp))
            SectionHeader("Army List Submission")

            SwitchRow("Require Army List", config.requireArmyList == true) {
                onConfigChange(config.copy(requireArmyList = it))
            }

            AnimatedVisibility(visible = config.requireArmyList == true) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedTextField(
                        value = config.armyListDeadline ?: "",
                        onValueChange = { onConfigChange(config.copy(armyListDeadline = it)) },
                        label = { Text("Submission Deadline") },
                        placeholder = { Text("YYYY-MM-DD HH:MM") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = config.armyListNotes ?: "",
                        onValueChange = { onConfigChange(config.copy(armyListNotes = it)) },
                        label = { Text("Submission Notes") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
