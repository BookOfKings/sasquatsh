package com.sasquatsh.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sasquatsh.app.domain.model.GameSystem

private fun gameSystemColor(gameSystem: GameSystem): Color = when (gameSystem) {
    GameSystem.BOARD_GAME -> Color(0xFF2D5A3D)
    GameSystem.MTG -> Color(0xFF6B3FA0)
    GameSystem.POKEMON_TCG -> Color(0xFFE3A100)
    GameSystem.YUGIOH -> Color(0xFFC62828)
    GameSystem.WARHAMMER_40K -> Color(0xFF1565C0)
}

private fun gameSystemOnColor(gameSystem: GameSystem): Color = when (gameSystem) {
    GameSystem.POKEMON_TCG -> Color(0xFF1A1A1A)
    else -> Color.White
}

@Composable
fun GameSystemChip(
    gameSystem: GameSystem,
    modifier: Modifier = Modifier,
) {
    val bgColor = gameSystemColor(gameSystem)
    val textColor = gameSystemOnColor(gameSystem)

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = bgColor,
    ) {
        Text(
            text = gameSystem.displayName,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
