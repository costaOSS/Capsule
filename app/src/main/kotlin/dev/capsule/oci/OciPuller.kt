package dev.capsule.oci

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class PullProgress(
    val currentLayer: Int,
    val totalLayers: Int,
    val currentLayerDigest: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isComplete: Boolean = false,
    val error: String? = null
)

@Singleton
class OciPuller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: HttpClient,
    private val ociRegistry: OciRegistry
) {
    private val overlayDir: File
        get() = File(context.filesDir, "overlay").also { it.mkdirs() }

    fun pullImage(
        image: String,
        tag: String,
        distroName: String,
        onProgress: (PullProgress) -> Unit
    ): Flow<PullProgress> = flow {
        val manifestResult = ociRegistry.fetchManifest(image, tag)
        val layers = manifestResult.getOrElse {
            emit(PullProgress(0, 0, "", 0, 0, error = it.message))
            return@flow
        }

        val distoOverlayDir = File(overlayDir, distroName).also { it.mkdirs() }
        val upperDir = File(distoOverlayDir, "upper").also { it.mkdirs() }

        var totalBytesDownloaded = 0L
        val totalSize = layers.sumOf { it.size }

        layers.forEachIndexed { index, layer ->
            val layerDigest = layer.digest.removePrefix("sha256:")
            val layerFile = File(upperDir, layerDigest)

            emit(PullProgress(
                currentLayer = index + 1,
                totalLayers = layers.size,
                currentLayerDigest = layerDigest.take(12),
                bytesDownloaded = totalBytesDownloaded,
                totalBytes = totalSize,
                isComplete = false
            ))

            try {
                downloadLayerBlob(image, layer, layerFile)
                totalBytesDownloaded += layer.size

                emit(PullProgress(
                    currentLayer = index + 1,
                    totalLayers = layers.size,
                    currentLayerDigest = layerDigest.take(12),
                    bytesDownloaded = totalBytesDownloaded,
                    totalBytes = totalSize,
                    isComplete = index == layers.lastIndex
                ))
            } catch (e: Exception) {
                emit(PullProgress(
                    currentLayer = index + 1,
                    totalLayers = layers.size,
                    currentLayerDigest = layerDigest.take(12),
                    bytesDownloaded = totalBytesDownloaded,
                    totalBytes = totalSize,
                    error = "Failed to download layer: ${e.message}"
                ))
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadLayerBlob(image: String, layer: OciLayer, outputFile: File) {
        val (registry, repo) = parseImage(image)
        val blobUrl = when (registry) {
            "ghcr.io" -> "https://ghcr.io/v2/$repo/blobs/${layer.digest}"
            else -> "https://registry-1.docker.io/v2/$repo/blobs/${layer.digest}"
        }

        val response = httpClient.get(blobUrl)
        response.bodyAsChannel().use { channel ->
            outputFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead > 0) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
        }

        val verified = verifyDigest(outputFile, layer.digest)
        if (!verified) {
            outputFile.delete()
            throw Exception("Layer digest verification failed")
        }
    }

    private fun parseImage(image: String): Pair<String, String> {
        return when {
            image.startsWith("ghcr.io/") -> "ghcr.io" to image.removePrefix("ghcr.io/")
            image.startsWith("docker.io/") -> "docker.io" to image.removePrefix("docker.io/")
            else -> "docker.io" to image
        }
    }

    private fun verifyDigest(file: File, expectedDigest: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            val hash = "sha256:" + digest.digest().joinToString("") { "%02x".format(it) }
            hash.equals(expectedDigest, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}