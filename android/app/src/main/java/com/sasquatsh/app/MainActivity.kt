package com.sasquatsh.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sasquatsh.app.views.app.AppNavigation
import com.sasquatsh.app.ui.theme.SasquatshTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SasquatshTheme {
                AppNavigation()
            }
        }
    }
}
