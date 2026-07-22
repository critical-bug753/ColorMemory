package com.yourcollege.colormemory

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "high_scores")

class ColorRepository(private val context: Context) {

    fun getHighScore(level: GameLevel): Flow<Int> {
        val key = intPreferencesKey("high_score_${level.name.lowercase()}")
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: 0
        }
    }

    suspend fun saveHighScore(level: GameLevel, score: Int) {
        val key = intPreferencesKey("high_score_${level.name.lowercase()}")
        context.dataStore.edit { preferences ->
            preferences[key] = score
        }
    }
}
