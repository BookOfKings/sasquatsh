package com.sasquatsh.app.services

import android.content.Context
import com.sasquatsh.app.models.RoundCounterState
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoundCounterPersistence @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) {
    private val file: File
        get() = File(context.filesDir, "round_counter_state.json")

    suspend fun save(state: RoundCounterState) {
        try {
            val json = moshi.adapter(RoundCounterState::class.java).toJson(state)
            file.writeText(json)
        } catch (_: Exception) { }
    }

    suspend fun load(): RoundCounterState? {
        return try {
            val json = file.readText()
            moshi.adapter(RoundCounterState::class.java).fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clear() {
        try {
            file.delete()
        } catch (_: Exception) { }
    }
}
