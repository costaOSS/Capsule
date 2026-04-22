package dev.capsule.runtime

data class Session(
    val id: Long,
    val distroName: String,
    val pid: Int,
    val ptyFd: Int,
    val mergedPath: String,
    val startedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)