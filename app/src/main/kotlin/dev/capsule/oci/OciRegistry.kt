package dev.capsule.oci

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class OciManifest(
    val schemaVersion: Int = 2,
    val mediaType: String? = null,
    val config: OciDescriptor? = null,
    val layers: List<OciDescriptor> = emptyList()
)

@Serializable
data class OciDescriptor(
    val mediaType: String,
    val digest: String,
    val size: Long
)

data class OciLayer(
    val digest: String,
    val size: Long,
    val mediaType: String
)

@Singleton
class OciRegistry @Inject constructor(
    private val httpClient: HttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val dockerHub = "https://registry.hub.docker.com"
    private val ghcr = "https://ghcr.io"

    suspend fun fetchManifest(image: String, tag: String): Result<List<OciLayer>> {
        return try {
            val (registry, repo) = parseImage(image)
            val layers = when (registry) {
                "docker.io" -> fetchDockerHubManifest(repo, tag)
                "ghcr.io" -> fetchGhcrManifest(repo, tag)
                else -> fetchDockerHubManifest(repo, tag)
            }
            Result.success(layers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseImage(image: String): Pair<String, String> {
        return when {
            image.startsWith("ghcr.io/") -> "ghcr.io" to image.removePrefix("ghcr.io/")
            image.startsWith("docker.io/") -> "docker.io" to image.removePrefix("docker.io/")
            else -> "docker.io" to image
        }
    }

    private suspend fun fetchDockerHubManifest(repo: String, tag: String): List<OciLayer> {
        val tokenUrl = "$dockerHub/v2/token?service=registry.docker.io&scope=repository:$repo:pull"
        val token: String = try {
            val tokenResponse = httpClient.get(tokenUrl)
            val tokenJson = json.parseToJsonElement(tokenResponse.bodyAsText())
            tokenJson.jsonObject["token"]?.jsonPrimitive?.content ?: ""
        } catch (e: Exception) {
            ""
        }

        val manifestUrl = "https://registry-1.docker.io/v2/$repo/manifests/$tag"
        val manifestResponse = httpClient.get(manifestUrl) {
            header("Authorization", "Bearer $token")
            header("Accept", "application/vnd.oci.image.manifest.v1+json")
        }

        val manifest = json.decodeFromString<OciManifest>(manifestResponse.bodyAsText())
        return manifest.layers.map { layer ->
            OciLayer(
                digest = layer.digest,
                size = layer.size,
                mediaType = layer.mediaType
            )
        }
    }

    private suspend fun fetchGhcrManifest(repo: String, tag: String): List<OciLayer> {
        val manifestUrl = "$ghcr/v2/$repo/manifests/$tag"
        val manifestResponse = httpClient.get(manifestUrl) {
            header("Accept", "application/vnd.oci.image.manifest.v1+json")
        }

        val manifest = json.decodeFromString<OciManifest>(manifestResponse.bodyAsText())
        return manifest.layers.map { layer ->
            OciLayer(
                digest = layer.digest,
                size = layer.size,
                mediaType = layer.mediaType
            )
        }
    }
}