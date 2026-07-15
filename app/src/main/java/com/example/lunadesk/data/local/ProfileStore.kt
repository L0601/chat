package com.example.lunadesk.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "lunadesk_settings")

@Serializable
data class ApiProfile(
    val id: String,
    val name: String,
    val baseUrl: String = "",
    val apiKey: String = "",
    val selectedModel: String = "",
    val temperature: Float = 0.3f,
    val maxTokens: Int = 20000
)

@Serializable
data class ApiProfileSettings(
    val activeProfileId: String,
    val profiles: List<ApiProfile>
) {
    val activeProfile: ApiProfile
        get() = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.first()
}

interface ProfileStore {
    val settings: Flow<ApiProfileSettings>

    suspend fun upsert(profile: ApiProfile, makeActive: Boolean = false)

    suspend fun setActive(profileId: String)

    suspend fun delete(profileId: String)
}

class DataStoreProfileStore(
    private val context: Context,
    private val json: Json = profileJson
) : ProfileStore {
    private object Keys {
        val profiles = stringPreferencesKey("api_profiles_v1")
    }

    override val settings: Flow<ApiProfileSettings> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> decodeSettings(preferences[Keys.profiles], json) }
        .distinctUntilChanged()

    override suspend fun upsert(profile: ApiProfile, makeActive: Boolean) {
        context.dataStore.edit { preferences ->
            val current = decodeSettings(preferences[Keys.profiles], json)
            val updated = upsertProfile(current, profile, makeActive)
            preferences[Keys.profiles] = json.encodeToString(
                ApiProfileSettings.serializer(),
                updated
            )
        }
    }

    override suspend fun setActive(profileId: String) {
        context.dataStore.edit { preferences ->
            val current = decodeSettings(preferences[Keys.profiles], json)
            if (current.profiles.none { it.id == profileId }) return@edit
            preferences[Keys.profiles] = json.encodeToString(
                ApiProfileSettings.serializer(),
                current.copy(activeProfileId = profileId)
            )
        }
    }

    override suspend fun delete(profileId: String) {
        context.dataStore.edit { preferences ->
            val current = decodeSettings(preferences[Keys.profiles], json)
            val updated = deleteProfile(current, profileId)
            if (updated == current) return@edit
            preferences[Keys.profiles] = json.encodeToString(
                ApiProfileSettings.serializer(),
                updated
            )
        }
    }
}

internal val profileJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun defaultProfileSettings(): ApiProfileSettings {
    val profile = ApiProfile(id = "default", name = "默认配置")
    return ApiProfileSettings(activeProfileId = profile.id, profiles = listOf(profile))
}

internal fun decodeSettings(raw: String?, json: Json = profileJson): ApiProfileSettings {
    val decoded = raw?.let {
        runCatching { json.decodeFromString(ApiProfileSettings.serializer(), it) }.getOrNull()
    }
    return normalizeSettings(decoded)
}

internal fun normalizeSettings(settings: ApiProfileSettings?): ApiProfileSettings {
    if (settings == null || settings.profiles.isEmpty()) return defaultProfileSettings()
    val profiles = settings.profiles.distinctBy { it.id }
    if (profiles.isEmpty()) return defaultProfileSettings()
    val activeId = settings.activeProfileId.takeIf { id -> profiles.any { it.id == id } }
        ?: profiles.first().id
    return ApiProfileSettings(activeProfileId = activeId, profiles = profiles)
}

internal fun upsertProfile(
    settings: ApiProfileSettings,
    profile: ApiProfile,
    makeActive: Boolean
): ApiProfileSettings {
    val index = settings.profiles.indexOfFirst { it.id == profile.id }
    val profiles = if (index >= 0) {
        settings.profiles.toMutableList().apply { this[index] = profile }
    } else {
        settings.profiles + profile
    }
    return ApiProfileSettings(
        activeProfileId = if (makeActive) profile.id else settings.activeProfileId,
        profiles = profiles
    )
}

internal fun deleteProfile(
    settings: ApiProfileSettings,
    profileId: String
): ApiProfileSettings {
    if (settings.profiles.size <= 1) return settings
    val profiles = settings.profiles.filterNot { it.id == profileId }
    if (profiles.size == settings.profiles.size) return settings
    val activeId = if (settings.activeProfileId == profileId) profiles.first().id
    else settings.activeProfileId
    return ApiProfileSettings(activeProfileId = activeId, profiles = profiles)
}
