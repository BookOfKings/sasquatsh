package com.sasquatsh.app.ui.theme

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppearanceMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String): AppearanceMode =
            entries.find { it.key == key } ?: SYSTEM
    }
}

object AppearanceManager {
    private const val PREFS_NAME = "appearance"
    private const val KEY_MODE = "appearanceMode"

    private val _mode = MutableStateFlow(AppearanceMode.SYSTEM)
    val mode: StateFlow<AppearanceMode> = _mode

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_MODE, "system") ?: "system"
        _mode.value = AppearanceMode.fromKey(saved)
    }

    fun setMode(context: Context, mode: AppearanceMode) {
        _mode.value = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.key)
            .apply()
    }
}

val LocalAppearanceMode = compositionLocalOf { AppearanceMode.SYSTEM }
