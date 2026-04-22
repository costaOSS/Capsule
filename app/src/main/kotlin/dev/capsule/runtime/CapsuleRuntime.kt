package dev.capsule.runtime

object CapsuleRuntime {
    init {
        System.loadLibrary("capsule-runtime")
    }

    external fun mountSquashfs(sfsPath: String, basePath: String): Int
    external fun mountOverlay(basePath: String, overlayPath: String, mergedPath: String): Int
    external fun setupDev(mergedPath: String): Int
    external fun enterSession(mergedPath: String, shell: String): IntArray
    external fun writePty(ptyFd: Int, data: ByteArray): Int
    external fun killSession(pid: Int, signal: Int): Int
    external fun unmount(path: String): Int
    external fun isFuseAvailable(): Boolean
    external fun resizePty(ptyFd: Int, rows: Int, cols: Int): Int
}