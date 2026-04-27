package com.sasquatsh.app.views.events

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.sasquatsh.app.models.YugiohConfigState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YugiohConfigFormSections(
    config: YugiohConfigState,
    onConfigChange: (YugiohConfigState) -> Unit
) {
    // ─── Format Section ───
    SectionHeader("Yu-Gi-Oh! Format")

    var formatExpanded by remember { mutableStateOf(false) }
    val formats = listOf(
        null to "Select Format",
        "advanced" to "Advanced",
        "traditional" to "Traditional",
        "speed_duel" to "Speed Duel",
        "time_wizard" to "Time Wizard",
        "casual" to "Casual"
    )
    ExposedDropdownMenuBox(expanded = formatExpanded, onExpandedChange = { formatExpanded = it }) {
        OutlinedTextField(
            value = formats.firstOrNull { it.first == config.formatId }?.second ?: "Select Format",
            onValueChange = {},
            readOnly = true,
            label = { Text("Format") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(formatExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
            formats.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onConfigChange(config.copy(formatId = value))
                        formatExpanded = false
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
        "casual" to "Casual", "locals" to "Locals", "ots" to "OTS Championship",
        "regional" to "Regional", "ycs" to "YCS",
        "nationals" to "Nationals", "worlds" to "Worlds"
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

            // Tournament Style
            var styleExpanded by remember { mutableStateOf(false) }
            val styles = listOf(
                "swiss" to "Swiss", "single_elimination" to "Single Elimination",
                "double_elimination" to "Double Elimination"
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

            // Best Of segmented
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val bestOfOptions = listOf(1 to "Best of 1", 3 to "Best of 3")
                bestOfOptions.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = (config.bestOf ?: 1) == value,
                        onClick = { onConfigChange(config.copy(bestOf = value)) },
                        shape = SegmentedButtonDefaults.itemShape(index, bestOfOptions.size)
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Round time
            val roundTime = (config.roundTimeMinutes ?: 40).toFloat()
            Text("Round Time: ${roundTime.toInt()} min", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = roundTime,
                onValueChange = { onConfigChange(config.copy(roundTimeMinutes = it.toInt())) },
                valueRange = 10f..120f,
                steps = 21
            )

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

            // Top Cut
            OutlinedTextField(
                value = config.topCut?.toString() ?: "",
                onValueChange = { onConfigChange(config.copy(topCut = it.toIntOrNull())) },
                label = { Text("Top Cut") },
                placeholder = { Text("None") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
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

    SwitchRow("Require Deck Registration", config.requireDeckRegistration == true) {
        onConfigChange(config.copy(requireDeckRegistration = it))
    }

    AnimatedVisibility(visible = config.requireDeckRegistration == true) {
        OutlinedTextField(
            value = config.deckSubmissionDeadline ?: "",
            onValueChange = { onConfigChange(config.copy(deckSubmissionDeadline = it)) },
            label = { Text("Submission Deadline") },
            placeholder = { Text("YYYY-MM-DD HH:MM") },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }

    SwitchRow("Allow Side Deck", config.allowSideDeck != false) {
        onConfigChange(config.copy(allowSideDeck = it))
    }

    SwitchRow("Enforce Format Legality", config.enforceFormatLegality != false) {
        onConfigChange(config.copy(enforceFormatLegality = it))
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

    // ─── Prizes & Official Play Section ───
    Spacer(Modifier.height(16.dp))
    SectionHeader("Prizes & Official Play")

    SwitchRow("Official Event", config.isOfficialEvent == true) {
        onConfigChange(config.copy(isOfficialEvent = it))
    }

    AnimatedVisibility(visible = config.isOfficialEvent == true) {
        SwitchRow("Awards OTS Points", config.awardsOtsPoints == true) {
            onConfigChange(config.copy(awardsOtsPoints = it))
        }
    }

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
