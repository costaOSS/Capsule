package dev.capsule.image

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: HttpClient
) {
    private val imagesDir: File
        get() = File(context.filesDir, "images").also { it.mkdirs() }

    fun downloadImage(
        imageInfo: ImageInfo,
        onProgress: (DownloadProgress) -> Unit
    ): Flow<DownloadProgress> = flow {
        val outputFile = File(imagesDir, "${imageInfo.name}.sfs")

        try {
            val response = httpClient.get(imageInfo.url)
            val contentLength = response.contentLength() ?: imageInfo.sizeMb * 1024 * 1024

            var bytesDownloaded = 0L

            response.bodyAsChannel().let { channel ->
                val buffer = ByteArray(8192)
                outputFile.outputStream().use { output ->
                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer)
                        if (bytesRead > 0) {
                            output.write(buffer, 0, bytesRead)
                            bytesDownloaded += bytesRead

                            emit(DownloadProgress(
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = contentLength,
                                isComplete = false
                            ))
                        }
                    }
                }
            }

            val verified = verifySha256(outputFile, imageInfo.sha256)
            if (!verified) {
                outputFile.delete()
                emit(DownloadProgress(
                    bytesDownloaded = bytesDownloaded,
                    totalBytes = contentLength,
                    error = "SHA256 verification failed"
                ))
            } else {
                emit(DownloadProgress(
                    bytesDownloaded = bytesDownloaded,
                    totalBytes = contentLength,
                    isComplete = true
                ))
            }

        } catch (e: Exception) {
            outputFile.delete()
            emit(DownloadProgress(
                bytesDownloaded = 0,
                totalBytes = 0,
                error = e.message ?: "Download failed"
            ))
        }
    }.flowOn(Dispatchers.IO)

    private fun verifySha256(file: File, expectedHash: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            hash.equals(expectedHash, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    fun getImagePath(name: String): String {
        return File(imagesDir, "$name.sfs").absolutePath
    }

    fun deleteImage(name: String): Boolean {
        return File(imagesDir, "$name.sfs").delete()
    }

    fun getInstalledImages(): List<File> {
        return imagesDir.listFiles()?.filter { it.extension == "sfs" } ?: emptyList()
    }
}