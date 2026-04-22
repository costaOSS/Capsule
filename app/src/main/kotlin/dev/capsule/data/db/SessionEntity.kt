package dev.capsule.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val distroName: String,
    val pid: Int,
    val ptyFd: Int,
    val startedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)