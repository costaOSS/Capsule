package dev.capsule.ui.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.capsule.runtime.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalUiState(
    val distroName: String = "",
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    private var terminalOutputCallback: ((ByteArray) -> Unit)? = null

    fun startSession(distroId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true)
            try {
                sessionManager.createSession(distroId) { callback ->
                    terminalOutputCallback = callback
                }
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    isConnected = true,
                    distroName = "Linux"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    error = e.message
                )
            }
        }
    }

    fun onTerminalOutput(data: ByteArray) {
        terminalOutputCallback?.invoke(data)
    }

    fun writeToTerminal(data: ByteArray) {
        viewModelScope.launch {
            sessionManager.writeToPty(data)
        }
    }

    fun closeSession() {
        viewModelScope.launch {
            sessionManager.closeCurrentSession()
        }
    }
}