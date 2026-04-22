package dev.capsule.image

import kotlinx.serialization.Serializable

@Serializable
data class ImageManifest(
    val updated: String,
    val images: List<ImageInfo>
)

@Serializable
data class ImageInfo(
    val name: String,
    val display: String,
    val version: String,
    val arch: String,
    val url: String,
    val sha256: String,
    val sizeMb: Long,
    val icon: String
)

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isComplete: Boolean = false,
    val error: String? = null
)