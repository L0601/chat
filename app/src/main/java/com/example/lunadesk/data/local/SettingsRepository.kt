package com.example.lunadesk.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lunadesk_settings")

data class UserSettings(
    val baseUrl: String = "",
    val selectedModel: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val apiKey: String = ""
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val baseUrl = stringPreferencesKey("base_url")
        val selectedModel = stringPreferencesKey("selected_model")
        val temperature = floatPreferencesKey("temperature")
        val maxTokens = intPreferencesKey("max_tokens")
        val apiKey = stringPreferencesKey("api_key")
    }

    suspend fun getSettings(): UserSettings {
        return context.dataStore.data.map(::mapSettings).first()
    }

    suspend fun saveSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.baseUrl] = settings.baseUrl
            prefs[Keys.selectedModel] = settings.selectedModel
            prefs[Keys.temperature] = settings.temperature
            prefs[Keys.maxTokens] = settings.maxTokens
            prefs[Keys.apiKey] = settings.apiKey
        }
    }

    private fun mapSettings(prefs: Preferences): UserSettings {
        return UserSettings(
            baseUrl = prefs[Keys.baseUrl].orEmpty(),
            selectedModel = prefs[Keys.selectedModel].orEmpty(),
            temperature = prefs[Keys.temperature] ?: 0.7f,
            maxTokens = prefs[Keys.maxTokens] ?: 2048,
            apiKey = prefs[Keys.apiKey].orEmpty()
        )
    }
}

