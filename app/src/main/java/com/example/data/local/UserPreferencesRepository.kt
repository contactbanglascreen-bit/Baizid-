package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ContentLanguage
import com.example.data.model.ContentSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cinePulseDataStore by preferencesDataStore(name = "cinepulse_prefs")

data class UserPreferences(
    val source: ContentSource = ContentSource.ALL,
    val language: ContentLanguage = ContentLanguage.EN,
    val safeMode: Boolean = true,
    val customApiKey: String = ""
)

class UserPreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.cinePulseDataStore

    private object Keys {
        val SOURCE = stringPreferencesKey("content_source")
        val LANGUAGE = stringPreferencesKey("content_language")
        val SAFE_MODE = booleanPreferencesKey("safe_mode")
        val CUSTOM_API_KEY = stringPreferencesKey("custom_cinepulse_api_key")
    }

    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            // MovieLinkBD is hidden for now (its server returns no titles), so anyone who had picked
            // it gets "All" instead of an empty Home.
            source = ContentSource.fromApi(prefs[Keys.SOURCE]).let { if (it == ContentSource.MOVIELINKBD) ContentSource.ALL else it },
            language = ContentLanguage.fromCode(prefs[Keys.LANGUAGE]),
            safeMode = prefs[Keys.SAFE_MODE] ?: true,
            customApiKey = prefs[Keys.CUSTOM_API_KEY].orEmpty()
        )
    }

    suspend fun setSource(source: ContentSource) {
        dataStore.edit { it[Keys.SOURCE] = source.apiValue }
    }

    suspend fun setLanguage(language: ContentLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setSafeMode(enabled: Boolean) {
        dataStore.edit { it[Keys.SAFE_MODE] = enabled }
    }

    suspend fun setCustomApiKey(apiKey: String) {
        dataStore.edit { it[Keys.CUSTOM_API_KEY] = apiKey.trim() }
    }
}
