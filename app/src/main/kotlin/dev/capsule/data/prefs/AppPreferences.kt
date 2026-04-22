package dev.capsule.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "capsule_prefs")

enum class ThemeMode {
    DARK, LIGHT, AMOLED
}

data class AppPreferences(
    val defaultShell: String = "/bin/bash",
    val fontSize: Int = 14,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val showStatusBar: Boolean = true,
    val keepScreenOn: Boolean = false
)

@Singleton
class AppPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val DEFAULT_SHELL = stringPreferencesKey("default_shell")
        val FONT_SIZE = intPreferencesKey("font_size")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_STATUS_BAR = booleanPreferencesKey("show_status_bar")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    val preferencesFlow: Flow<AppPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppPreferences(
                defaultShell = preferences[PreferencesKeys.DEFAULT_SHELL] ?: "/bin/bash",
                fontSize = preferences[PreferencesKeys.FONT_SIZE] ?: 14,
                themeMode = try {
                    ThemeMode.valueOf(preferences[PreferencesKeys.THEME_MODE] ?: "DARK")
                } catch (e: Exception) {
                    ThemeMode.DARK
                },
                showStatusBar = preferences[PreferencesKeys.SHOW_STATUS_BAR] ?: true,
                keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: false
            )
        }

    suspend fun updateDefaultShell(shell: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_SHELL] = shell
        }
    }

    suspend fun updateFontSize(size: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE] = size
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun updateShowStatusBar(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_STATUS_BAR] = show
        }
    }

    suspend fun updateKeepScreenOn(keepOn: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEEP_SCREEN_ON] = keepOn
        }
    }
}