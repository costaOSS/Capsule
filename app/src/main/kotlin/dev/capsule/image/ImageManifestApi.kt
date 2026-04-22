package dev.capsule.image

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageManifestApi @Inject constructor(
    private val httpClient: HttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val manifestUrl = "https://github.com/costaOSS/Capsule/releases/download/images-latest/manifest.json"

    suspend fun fetchManifest(): Result<ImageManifest> {
        return try {
            val response: HttpResponse = httpClient.get(manifestUrl)
            val body = response.bodyAsText()
            val manifest = json.decodeFromString<ImageManifest>(body)
            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableImages(): List<ImageInfo> {
        return fetchManifest().getOrNull()?.images ?: emptyList()
    }
}