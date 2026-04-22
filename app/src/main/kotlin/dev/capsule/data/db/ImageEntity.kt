package dev.capsule.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val displayName: String,
    val version: String,
    val arch: String,
    val sfsPath: String,
    val sha256: String,
    val sizeMb: Long,
    val installedAt: Long = System.currentTimeMillis()
)