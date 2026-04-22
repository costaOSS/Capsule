package dev.capsule.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.capsule.data.prefs.AppPreferences
import dev.capsule.data.prefs.AppPreferencesManager
import dev.capsule.data.prefs.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val fontSize: Int = 14,
    val keepScreenOn: Boolean = false,
    val imagesSizeBytes: Long = 0,
    val overlaySizeBytes: Long = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: AppPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.preferencesFlow.collect { prefs ->
                _uiState.value = _uiState.value.copy(
                    themeMode = prefs.themeMode,
                    fontSize = prefs.fontSize,
                    keepScreenOn = prefs.keepScreenOn
                )
            }
        }

        calculateStorageUsage()
    }

    private fun calculateStorageUsage() {
        viewModelScope.launch {
            val filesDir = context.filesDir
            val imagesDir = File(filesDir, "images")
            val overlayDir = File(filesDir, "overlay")

            val imagesSize = imagesDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            val overlaySize = overlayDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()

            _uiState.value = _uiState.value.copy(
                imagesSizeBytes = imagesSize,
                overlaySizeBytes = overlaySize
            )
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.updateThemeMode(mode)
        }
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch {
            preferencesManager.updateFontSize(size)
        }
    }

    fun updateKeepScreenOn(keepOn: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateKeepScreenOn(keepOn)
        }
    }
}