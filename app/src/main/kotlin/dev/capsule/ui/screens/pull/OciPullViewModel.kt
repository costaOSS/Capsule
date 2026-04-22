package dev.capsule.ui.screens.pull

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.capsule.oci.OciPuller
import dev.capsule.oci.PullProgress
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class OciPullUiState(
    val isPulling: Boolean = false,
    val currentLayer: Int = 0,
    val totalLayers: Int = 0,
    val progress: Float = 0f,
    val layers: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class OciPullViewModel @Inject constructor(
    private val ociPuller: OciPuller
) : ViewModel() {

    private val _uiState = MutableStateFlow(OciPullUiState())
    val uiState: StateFlow<OciPullUiState> = _uiState.asStateFlow()

    fun pullImage(image: String) {
        val (imageName, tag) = parseImage(image)
        val distroName = "oci_${UUID.randomUUID().toString().take(8)}"

        viewModelScope.launch {
            _uiState.value = OciPullUiState(isPulling = true)

            ociPuller.pullImage(imageName, tag, distroName) { progress ->
                val progressPercent = if (progress.totalBytes > 0) {
                    progress.bytesDownloaded.toFloat() / progress.totalBytes.toFloat()
                } else 0f

                _uiState.value = _uiState.value.copy(
                    isPulling = !progress.isComplete && progress.error == null,
                    currentLayer = progress.currentLayer,
                    totalLayers = progress.totalLayers,
                    progress = progressPercent,
                    layers = if (progress.currentLayer > _uiState.value.layers.size) {
                        _uiState.value.layers + progress.currentLayerDigest
                    } else _uiState.value.layers,
                    error = progress.error
                )
            }
        }
    }

    private fun parseImage(image: String): Pair<String, String> {
        val parts = image.split(":")
        return if (parts.size == 2) {
            parts[0] to parts[1]
        } else {
            image to "latest"
        }
    }
}