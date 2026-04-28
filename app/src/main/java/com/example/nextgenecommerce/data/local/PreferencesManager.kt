package com.example.nextgenecommerce.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val USE_SYSTEM_THEME_KEY = booleanPreferencesKey("use_system_theme")
        val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: false
    }

    val useSystemTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_SYSTEM_THEME_KEY] ?: true
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_KEY] ?: true
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN_KEY] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    suspend fun setUseSystemTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_SYSTEM_THEME_KEY] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }

    suspend fun setLoggedIn(loggedIn: Boolean, userId: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN_KEY] = loggedIn
            if (userId != null) {
                preferences[USER_ID_KEY] = userId
            }
        }
    }

    suspend fun clearPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
