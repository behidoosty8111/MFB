package com.example.mfb.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mfb.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mfb_user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("mfb_theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("mfb_app_language")
        val LAST_VIEWED_VERSION_CODE = intPreferencesKey("mfb_last_viewed_version_code")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            when (preferences[PreferencesKeys.THEME_MODE]) {
                ThemeMode.LIGHT.name -> ThemeMode.LIGHT
                ThemeMode.DARK.name -> ThemeMode.DARK
                ThemeMode.SYSTEM.name -> ThemeMode.SYSTEM
                else -> ThemeMode.DARK // Default to dark-first as specified in design guidelines
            }
        }

    val appLanguageFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] ?: "en"
        }

    val lastViewedVersionCodeFlow: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_VIEWED_VERSION_CODE] ?: 0
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = languageCode
        }
    }

    suspend fun setLastViewedVersionCode(versionCode: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_VIEWED_VERSION_CODE] = versionCode
        }
    }
}
