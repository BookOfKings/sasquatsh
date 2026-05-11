package com.sasquatsh.app.views.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sasquatsh.app.R

@Composable
fun PoweredByBggLogo(
    modifier: Modifier = Modifier,
    height: Dp = 24.dp
) {
    Image(
        painter = painterResource(id = R.drawable.powered_by_bgg),
        contentDescription = "Powered by BoardGameGeek",
        modifier = modifier.height(height),
        contentScale = ContentScale.Fit
    )
}
