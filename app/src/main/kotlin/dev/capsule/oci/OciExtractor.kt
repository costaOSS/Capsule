package dev.capsule.oci

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.*
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OciExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun extractLayer(layerFile: File, outputDir: File): Result<Int> {
        return try {
            outputDir.mkdirs()
            var extractedCount = 0

            GZIPInputStream(FileInputStream(layerFile)).use { gzipInput ->
                TarInputStream(gzipInput).use { tarInput ->
                    var entry = tarInput.nextEntry
                    while (entry != null) {
                        val outputFile = File(outputDir, entry.name)

                        when {
                            entry.name.contains(".wh.") -> {
                                val targetName = entry.name.removePrefix(".wh.")
                                val targetFile = File(outputDir, targetName)
                                if (targetFile.exists()) {
                                    targetFile.delete()
                                }
                            }
                            entry.isDirectory -> {
                                outputFile.mkdirs()
                            }
                            entry.isSymbolicLink -> {
                                // Skip symlinks for security
                            }
                            else -> {
                                outputFile.parentFile?.mkdirs()
                                tarInput.copyTo(FileOutputStream(outputFile))
                                extractedCount++
                            }
                        }

                        entry = tarInput.nextEntry
                    }
                }
            }

            Result.success(extractedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private class TarInputStream(inputStream: InputStream) : FilterInputStream(inputStream) {
        private val buffer = ByteArray(512)
        private var currentEntry: TarEntry? = null

        fun nextEntry(): TarEntry? {
            val read = read(buffer)
            if (read < 0) return null

            val name = String(buffer, 0, 100).trimEnd('\0')
            val size = String(buffer, 124, 12, Charsets.US_ASCII).trimEnd(' ').toLongOrNull() ?: 0
            val type = buffer[156].toInt()

            currentEntry = TarEntry(name, size, type)
            return currentEntry
        }

        fun copyTo(output: OutputStream): Long {
            var remaining = currentEntry?.size ?: 0
            val tempBuffer = ByteArray(8192)

            while (remaining > 0) {
                val toRead = minOf(tempBuffer.size.toLong(), remaining).toInt()
                val read = read(tempBuffer, 0, toRead)
                if (read <= 0) break
                output.write(tempBuffer, 0, read)
                remaining -= read
            }

            val paddedSize = ((currentEntry?.size ?: 0) + 511) / 512 * 512
            val dataRead = (currentEntry?.size ?: 0) - remaining
            if (paddedSize > dataRead) {
                skip((paddedSize - dataRead).toLong())
            }

            return currentEntry?.size ?: 0
        }

        data class TarEntry(
            val name: String,
            val size: Long,
            val type: Int
        ) {
            val isDirectory: Boolean get() = type == 5 || name.endsWith("/")
            val isSymbolicLink: Boolean get() = type == 2
        }
    }
}