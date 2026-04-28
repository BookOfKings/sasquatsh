package com.sasquatsh.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sasquatsh.app.ui.theme.AppearanceManager
import com.sasquatsh.app.ui.theme.AppearanceMode
import com.sasquatsh.app.ui.theme.SasquatshTheme
import com.sasquatsh.app.views.app.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppearanceManager.init(this)
        enableEdgeToEdge()
        setContent {
            val appearanceMode by AppearanceManager.mode.collectAsState()
            val darkTheme = when (appearanceMode) {
                AppearanceMode.SYSTEM -> isSystemInDarkTheme()
                AppearanceMode.LIGHT -> false
                AppearanceMode.DARK -> true
            }
            SasquatshTheme(darkTheme = darkTheme) {
                AppNavigation()
            }
        }
    }
}
