package com.sasquatsh.app.views.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sasquatsh.app.models.GameSystem

@Composable
fun GameSystemChip(
    gameSystem: GameSystem,
    modifier: Modifier = Modifier
) {
    val chipColor = when (gameSystem) {
        GameSystem.BOARD_GAME -> MaterialTheme.colorScheme.primaryContainer
        GameSystem.MTG -> MaterialTheme.colorScheme.secondaryContainer
        GameSystem.POKEMON_TCG -> MaterialTheme.colorScheme.tertiaryContainer
        GameSystem.YUGIOH -> Color(0xFF4D3380).copy(alpha = 0.3f)
        GameSystem.WARHAMMER_40K -> Color(0xFF801A1A).copy(alpha = 0.3f)
    }

    val textColor = when (gameSystem) {
        GameSystem.BOARD_GAME -> MaterialTheme.colorScheme.onPrimaryContainer
        GameSystem.MTG -> MaterialTheme.colorScheme.onSecondaryContainer
        GameSystem.POKEMON_TCG -> MaterialTheme.colorScheme.onTertiaryContainer
        GameSystem.YUGIOH -> MaterialTheme.colorScheme.onSurface
        GameSystem.WARHAMMER_40K -> MaterialTheme.colorScheme.onSurface
    }

    BadgeView(
        text = gameSystem.shortName,
        color = chipColor,
        textColor = textColor,
        modifier = modifier
    )
}
