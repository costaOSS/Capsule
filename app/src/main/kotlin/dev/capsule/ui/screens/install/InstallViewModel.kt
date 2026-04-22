package dev.capsule.ui.screens.install

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.capsule.image.DownloadProgress
import dev.capsule.image.ImageInfo
import dev.capsule.image.ImageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstallUiState(
    val availableImages: List<ImageInfo> = emptyList(),
    val isLoading: Boolean = false,
    val downloadingImages: Set<String> = emptySet(),
    val downloadProgress: Map<String, Float> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class InstallViewModel @Inject constructor(
    private val imageRepository: ImageRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(InstallUiState())
    val uiState: StateFlow<InstallUiState> = _uiState.asStateFlow()

    fun loadAvailableImages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val images = imageRepository.getAvailableImages()
                _uiState.value = _uiState.value.copy(
                    availableImages = images,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun installImage(imageInfo: ImageInfo) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                downloadingImages = _uiState.value.downloadingImages + imageInfo.name
            )

            imageRepository.downloadImage(imageInfo) { progress ->
                val progressPercent = if (progress.totalBytes > 0) {
                    progress.bytesDownloaded.toFloat() / progress.totalBytes.toFloat()
                } else 0f

                _uiState.value = _uiState.value.copy(
                    downloadProgress = _uiState.value.downloadProgress + (imageInfo.name to progressPercent)
                )

                if (progress.isComplete) {
                    _uiState.value = _uiState.value.copy(
                        downloadingImages = _uiState.value.downloadingImages - imageInfo.name,
                        downloadProgress = _uiState.value.downloadProgress - imageInfo.name
                    )
                }

                if (progress.error != null) {
                    _uiState.value = _uiState.value.copy(
                        downloadingImages = _uiState.value.downloadingImages - imageInfo.name,
                        downloadProgress = _uiState.value.downloadProgress - imageInfo.name,
                        error = progress.error
                    )
                }
            }
        }
    }
}