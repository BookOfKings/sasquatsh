package com.sasquatsh.app.views.mtg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sasquatsh.app.models.ScryfallCard
import com.sasquatsh.app.views.shared.BadgeView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailSheet(
    card: ScryfallCard,
    onDismiss: () -> Unit,
    onAdd: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Card image
            card.largeImageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = card.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Name + mana cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    card.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                card.manaCost?.let { mana ->
                    Text(
                        mana,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Type line
            card.typeLine?.let { type ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    type,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Oracle text
            card.oracleText?.let { text ->
                if (text.isNotEmpty()) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Power/Toughness
            if (card.power != null && card.toughness != null) {
                Text(
                    "${card.power}/${card.toughness}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Set + Rarity
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                card.setCode?.let { set ->
                    BadgeView(
                        text = set.uppercase(),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
                card.rarity?.let { rarity ->
                    BadgeView(
                        text = rarity.replaceFirstChar { it.uppercase() },
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
            }

            // Add buttons
            if (onAdd != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onAdd("main") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add to Main")
                    }
                    OutlinedButton(
                        onClick = { onAdd("sideboard") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sideboard")
                    }
                }
            }
        }
    }
}
