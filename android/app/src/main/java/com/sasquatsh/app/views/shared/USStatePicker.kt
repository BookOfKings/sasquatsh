package com.sasquatsh.app.views.shared

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * US state selection dropdown for forms.
 * Port of the iOS USStatePicker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun USStatePicker(
    selection: String,
    onSelectionChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "State"
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = USState.allStates.find { it.abbreviation == selection }?.name ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // "Select" placeholder option
            DropdownMenuItem(
                text = { Text("Select") },
                onClick = {
                    onSelectionChanged("")
                    expanded = false
                }
            )

            USState.allStates.forEach { state ->
                DropdownMenuItem(
                    text = { Text(state.name) },
                    onClick = {
                        onSelectionChanged(state.abbreviation)
                        expanded = false
                    }
                )
            }
        }
    }
}

data class USStateEntry(
    val abbreviation: String,
    val name: String
)

object USState {
    val allStates: List<USStateEntry> = listOf(
        USStateEntry("AL", "Alabama"),
        USStateEntry("AK", "Alaska"),
        USStateEntry("AZ", "Arizona"),
        USStateEntry("AR", "Arkansas"),
        USStateEntry("CA", "California"),
        USStateEntry("CO", "Colorado"),
        USStateEntry("CT", "Connecticut"),
        USStateEntry("DE", "Delaware"),
        USStateEntry("FL", "Florida"),
        USStateEntry("GA", "Georgia"),
        USStateEntry("HI", "Hawaii"),
        USStateEntry("ID", "Idaho"),
        USStateEntry("IL", "Illinois"),
        USStateEntry("IN", "Indiana"),
        USStateEntry("IA", "Iowa"),
        USStateEntry("KS", "Kansas"),
        USStateEntry("KY", "Kentucky"),
        USStateEntry("LA", "Louisiana"),
        USStateEntry("ME", "Maine"),
        USStateEntry("MD", "Maryland"),
        USStateEntry("MA", "Massachusetts"),
        USStateEntry("MI", "Michigan"),
        USStateEntry("MN", "Minnesota"),
        USStateEntry("MS", "Mississippi"),
        USStateEntry("MO", "Missouri"),
        USStateEntry("MT", "Montana"),
        USStateEntry("NE", "Nebraska"),
        USStateEntry("NV", "Nevada"),
        USStateEntry("NH", "New Hampshire"),
        USStateEntry("NJ", "New Jersey"),
        USStateEntry("NM", "New Mexico"),
        USStateEntry("NY", "New York"),
        USStateEntry("NC", "North Carolina"),
        USStateEntry("ND", "North Dakota"),
        USStateEntry("OH", "Ohio"),
        USStateEntry("OK", "Oklahoma"),
        USStateEntry("OR", "Oregon"),
        USStateEntry("PA", "Pennsylvania"),
        USStateEntry("RI", "Rhode Island"),
        USStateEntry("SC", "South Carolina"),
        USStateEntry("SD", "South Dakota"),
        USStateEntry("TN", "Tennessee"),
        USStateEntry("TX", "Texas"),
        USStateEntry("UT", "Utah"),
        USStateEntry("VT", "Vermont"),
        USStateEntry("VA", "Virginia"),
        USStateEntry("WA", "Washington"),
        USStateEntry("WV", "West Virginia"),
        USStateEntry("WI", "Wisconsin"),
        USStateEntry("WY", "Wyoming"),
        USStateEntry("DC", "Washington D.C."),
        USStateEntry("PR", "Puerto Rico"),
        USStateEntry("GU", "Guam"),
        USStateEntry("VI", "U.S. Virgin Islands")
    )
}
