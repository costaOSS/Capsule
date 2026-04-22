package dev.capsule.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images ORDER BY installedAt DESC")
    fun getAllImages(): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE id = :id")
    suspend fun getImageById(id: Long): ImageEntity?

    @Query("SELECT * FROM images WHERE name = :name")
    suspend fun getImageByName(name: String): ImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ImageEntity): Long

    @Delete
    suspend fun delete(image: ImageEntity)

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM images")
    suspend fun getImageCount(): Int
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE isActive = 1")
    fun getActiveSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE distroName = :distroName AND isActive = 1")
    suspend fun getSessionByDistro(distroName: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Query("UPDATE sessions SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: Long)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Long)
}