package dev.capsule.runtime

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private var currentSession: Session? = null
    private var ptyReader: PtyReader? = null
    private var readerJob: Job? = null

    private val basePath: String
        get() = context.filesDir.absolutePath

    suspend fun createSession(
        distroId: Long,
        onOutput: (ByteArray) -> Unit
    ): Session = withContext(Dispatchers.IO) {
        val distroName = "distro_$distroId"
        val sfsPath = "$basePath/images/$distroName.sfs"
        val baseDir = "$basePath/mnt/$distroName/base"
        val overlayDir = "$basePath/overlay/$distroName"
        val mergedPath = "$basePath/mnt/$distroName/merged"

        File(mergedPath).mkdirs()
        File(overlayDir).mkdirs()

        val mountResult = CapsuleRuntime.mountSquashfs(sfsPath, baseDir)
        if (mountResult < 0) {
            throw RuntimeException("Failed to mount squashfs")
        }

        val overlayResult = CapsuleRuntime.mountOverlay(baseDir, overlayDir, mergedPath)
        if (overlayResult < 0) {
            CapsuleRuntime.unmount(baseDir)
            throw RuntimeException("Failed to mount overlay")
        }

        val devResult = CapsuleRuntime.setupDev(mergedPath)
        if (devResult < 0) {
            CapsuleRuntime.unmount(mergedPath)
            CapsuleRuntime.unmount(baseDir)
            throw RuntimeException("Failed to setup dev")
        }

        val shell = findShell(mergedPath)
        val ptyResult = CapsuleRuntime.enterSession(mergedPath, shell)
        if (ptyResult == null || ptyResult.size < 2) {
            CapsuleRuntime.unmount(mergedPath)
            CapsuleRuntime.unmount(baseDir)
            throw RuntimeException("Failed to start session")
        }

        val ptyFd = ptyResult[0]
        val pid = ptyResult[1]

        val session = Session(
            id = distroId,
            distroName = distroName,
            pid = pid,
            ptyFd = ptyFd,
            mergedPath = mergedPath
        )

        currentSession = session
        ptyReader = PtyReader(ptyFd)

        readerJob = CoroutineScope(Dispatchers.IO).launch {
            ptyReader?.readFlow()?.collect { data ->
                onOutput(data)
            }
        }

        _sessions.value = _sessions.value + session
        session
    }

    private fun findShell(mergedPath: String): String {
        val shells = listOf("/bin/bash", "/bin/sh", "/usr/bin/bash", "/usr/bin/sh")
        for (shell in shells) {
            if (File("$mergedPath$shell").exists()) {
                return shell
            }
        }
        return "/bin/sh"
    }

    suspend fun writeToPty(data: ByteArray) {
        currentSession?.let { session ->
            withContext(Dispatchers.IO) {
                CapsuleRuntime.writePty(session.ptyFd, data)
            }
        }
    }

    suspend fun closeCurrentSession() {
        currentSession?.let { session ->
            readerJob?.cancel()
            ptyReader?.close()

            withContext(Dispatchers.IO) {
                CapsuleRuntime.killSession(session.pid, 15)
                delay(1000)
                CapsuleRuntime.killSession(session.pid, 9)

                val mergedPath = session.mergedPath
                val baseDir = mergedPath.replace("/merged", "/base")

                CapsuleRuntime.unmount(mergedPath)
                CapsuleRuntime.unmount(baseDir)
            }

            currentSession = null
            ptyReader = null

            _sessions.value = _sessions.value.filter { it.id != session.id }
        }
    }

    suspend fun resizePty(rows: Int, cols: Int) {
        currentSession?.let { session ->
            withContext(Dispatchers.IO) {
                CapsuleRuntime.resizePty(session.ptyFd, rows, cols)
            }
        }
    }
}