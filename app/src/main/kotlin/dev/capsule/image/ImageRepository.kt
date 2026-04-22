package dev.capsule.image

import dev.capsule.data.db.ImageDao
import dev.capsule.data.db.ImageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class ImageWithStatus(
    val entity: ImageEntity,
    val isDownloaded: Boolean,
    val overlaySizeMb: Int = 0
)

@Singleton
class ImageRepository @Inject constructor(
    private val imageDao: ImageDao,
    private val imageManifestApi: ImageManifestApi,
    private val imageDownloader: ImageDownloader
) {
    fun getInstalledImages(): Flow<List<ImageWithStatus>> {
        return imageDao.getAllImages().map { entities ->
            entities.map { entity ->
                ImageWithStatus(
                    entity = entity,
                    isDownloaded = java.io.File(entity.sfsPath).exists()
                )
            }
        }
    }

    suspend fun getAvailableImages(): List<ImageInfo> {
        return imageManifestApi.getAvailableImages()
    }

    suspend fun downloadImage(imageInfo: ImageInfo, onProgress: (DownloadProgress) -> Unit): Result<Long> {
        imageDownloader.downloadImage(imageInfo, onProgress).collect { progress ->
            if (progress.error != null) {
                return Result.failure(Exception(progress.error))
            }
            if (progress.isComplete) {
                val entity = ImageEntity(
                    name = imageInfo.name,
                    displayName = imageInfo.display,
                    version = imageInfo.version,
                    arch = imageInfo.arch,
                    sfsPath = imageDownloader.getImagePath(imageInfo.name),
                    sha256 = imageInfo.sha256,
                    sizeMb = imageInfo.sizeMb
                )
                val id = imageDao.insert(entity)
                return Result.success(id)
            }
        }
        return Result.failure(Exception("Download failed"))
    }

    suspend fun deleteImage(id: Long) {
        val image = imageDao.getImageById(id)
        image?.let {
            imageDownloader.deleteImage(it.name)
            imageDao.deleteById(id)
        }
    }

    suspend fun getImageById(id: Long): ImageEntity? {
        return imageDao.getImageById(id)
    }
}