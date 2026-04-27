package com.sasquatsh.app.views.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sasquatsh.app.models.*
import com.sasquatsh.app.viewmodels.ProfileViewModel
import com.sasquatsh.app.views.shared.BadgeView
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileEditSheet(
    profile: UserProfile,
    profileViewModel: ProfileViewModel,
    onDismiss: () -> Unit
) {
    val uiState by profileViewModel.uiState.collectAsState()

    var displayName by rememberSaveable { mutableStateOf(profile.displayName ?: "") }
    var username by rememberSaveable { mutableStateOf(profile.username) }
    var bio by rememberSaveable { mutableStateOf(profile.bio ?: "") }
    var homeCity by rememberSaveable { mutableStateOf(profile.homeCity ?: "") }
    var homeState by rememberSaveable { mutableStateOf(profile.homeState ?: "") }
    var homePostalCode by rememberSaveable { mutableStateOf(profile.homePostalCode ?: "") }
    var maxTravelMiles by rememberSaveable {
        mutableStateOf(profile.maxTravelMiles?.toString() ?: "")
    }
    var favoriteGames by rememberSaveable {
        mutableStateOf(profile.favoriteGames?.joinToString(", ") ?: "")
    }
    var birthYear by rememberSaveable {
        mutableStateOf(profile.birthYear?.toString() ?: "")
    }
    var collectionVisibility by rememberSaveable {
        mutableStateOf(profile.collectionVisibility ?: "private")
    }

    var selectedTimezone by remember {
        mutableStateOf(
            profile.timezone?.let { AppTimezone.fromValue(it) } ?: AppTimezone.EASTERN
        )
    }
    var timezoneExpanded by remember { mutableStateOf(false) }

    var selectedGameTypes by remember {
        mutableStateOf(
            profile.preferredGameTypes
                ?.mapNotNull { GameCategory.fromValue(it) }
                ?.toSet()
                ?: emptySet()
        )
    }

    var isSaving by remember { mutableStateOf(false) }

    // US States list
    val usStates = listOf(
        "" to "Select State",
        "AL" to "Alabama", "AK" to "Alaska", "AZ" to "Arizona", "AR" to "Arkansas",
        "CA" to "California", "CO" to "Colorado", "CT" to "Connecticut", "DE" to "Delaware",
        "FL" to "Florida", "GA" to "Georgia", "HI" to "Hawaii", "ID" to "Idaho",
        "IL" to "Illinois", "IN" to "Indiana", "IA" to "Iowa", "KS" to "Kansas",
        "KY" to "Kentucky", "LA" to "Louisiana", "ME" to "Maine", "MD" to "Maryland",
        "MA" to "Massachusetts", "MI" to "Michigan", "MN" to "Minnesota", "MS" to "Mississippi",
        "MO" to "Missouri", "MT" to "Montana", "NE" to "Nebraska", "NV" to "Nevada",
        "NH" to "New Hampshire", "NJ" to "New Jersey", "NM" to "New Mexico", "NY" to "New York",
        "NC" to "North Carolina", "ND" to "North Dakota", "OH" to "Ohio", "OK" to "Oklahoma",
        "OR" to "Oregon", "PA" to "Pennsylvania", "RI" to "Rhode Island", "SC" to "South Carolina",
        "SD" to "South Dakota", "TN" to "Tennessee", "TX" to "Texas", "UT" to "Utah",
        "VT" to "Vermont", "VA" to "Virginia", "WA" to "Washington", "WV" to "West Virginia",
        "WI" to "Wisconsin", "WY" to "Wyoming"
    )
    var stateExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(
                    onClick = {
                        isSaving = true
                        val games = favoriteGames
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        val gameTypes = if (selectedGameTypes.isEmpty()) null
                        else selectedGameTypes.map { it.value }

                        val input = UpdateProfileInput(
                            username = if (username != profile.username) username else null,
                            displayName = displayName.ifEmpty { null },
                            homeCity = homeCity.ifEmpty { null },
                            homeState = homeState.ifEmpty { null },
                            homePostalCode = homePostalCode.ifEmpty { null },
                            maxTravelMiles = maxTravelMiles.toIntOrNull(),
                            timezone = selectedTimezone.value,
                            bio = bio.ifEmpty { null },
                            favoriteGames = games.ifEmpty { null },
                            preferredGameTypes = gameTypes,
                            collectionVisibility = collectionVisibility,
                            birthYear = birthYear.toIntOrNull()?.let {
                                if (it > 1900 && it <= Calendar.getInstance().get(Calendar.YEAR)) it
                                else null
                            }
                        )
                        profileViewModel.updateProfile(input)
                        isSaving = false
                        if (uiState.error == null) {
                            onDismiss()
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Basic Info
            Text(
                text = "Basic Info",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase() },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = birthYear,
                onValueChange = { birthYear = it },
                label = { Text("Birth Year") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // About
            Text(
                text = "About",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Location
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = homeCity,
                onValueChange = { homeCity = it },
                label = { Text("City") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // State Dropdown
            ExposedDropdownMenuBox(
                expanded = stateExpanded,
                onExpandedChange = { stateExpanded = it }
            ) {
                OutlinedTextField(
                    value = usStates.find { it.first == homeState }?.second ?: "Select State",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("State") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = stateExpanded,
                    onDismissRequest = { stateExpanded = false }
                ) {
                    usStates.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                homeState = code
                                stateExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = homePostalCode,
                onValueChange = { homePostalCode = it },
                label = { Text("Postal Code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = maxTravelMiles,
                onValueChange = { maxTravelMiles = it },
                label = { Text("Max Travel (miles)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timezone
            Text(
                text = "Timezone",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = timezoneExpanded,
                onExpandedChange = { timezoneExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedTimezone.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = timezoneExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = timezoneExpanded,
                    onDismissRequest = { timezoneExpanded = false }
                ) {
                    AppTimezone.entries.forEach { tz ->
                        DropdownMenuItem(
                            text = { Text(tz.displayName) },
                            onClick = {
                                selectedTimezone = tz
                                timezoneExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game Collection Visibility
            Text(
                text = "Game Collection",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = collectionVisibility == "private",
                    onClick = { collectionVisibility = "private" },
                    label = { Text("Private") }
                )
                FilterChip(
                    selected = collectionVisibility == "public",
                    onClick = { collectionVisibility = "public" },
                    label = { Text("Public") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Favorite Games
            Text(
                text = "Favorite Games (comma separated)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = favoriteGames,
                onValueChange = { favoriteGames = it },
                label = { Text("e.g. Catan, Wingspan, Gloomhaven") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preferred Game Types
            Text(
                text = "Preferred Game Types",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = cat in selectedGameTypes,
                        onClick = {
                            selectedGameTypes = if (cat in selectedGameTypes) {
                                selectedGameTypes - cat
                            } else {
                                selectedGameTypes + cat
                            }
                        },
                        label = { Text(cat.displayName) }
                    )
                }
            }

            // Error display
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
