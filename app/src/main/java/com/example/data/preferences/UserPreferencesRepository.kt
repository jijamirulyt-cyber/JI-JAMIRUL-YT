package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "feelkar_preferences")

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val READ_RECEIPTS_ENABLED = booleanPreferencesKey("read_receipts_enabled")
        val DEFAULT_STORY_PRIVACY = stringPreferencesKey("default_story_privacy")
    }

    val themePreference: Flow<ThemePreference> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            ThemePreference.LIGHT.name -> ThemePreference.LIGHT
            ThemePreference.DARK.name -> ThemePreference.DARK
            else -> ThemePreference.SYSTEM
        }
    }

    val currentUserId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_USER_ID]
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val readReceiptsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.READ_RECEIPTS_ENABLED] ?: true
    }

    val defaultStoryPrivacy: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_STORY_PRIVACY] ?: "PUBLIC"
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME_MODE] = theme.name }
    }

    suspend fun setCurrentUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId == null) prefs.remove(Keys.CURRENT_USER_ID)
            else prefs[Keys.CURRENT_USER_ID] = userId
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setReadReceiptsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.READ_RECEIPTS_ENABLED] = enabled }
    }

    suspend fun setDefaultStoryPrivacy(privacy: String) {
        context.dataStore.edit { it[Keys.DEFAULT_STORY_PRIVACY] = privacy }
    }
}
