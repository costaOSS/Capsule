package dev.capsule.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.FileDescriptor
import java.nio.ByteBuffer

class PtyReader(private val ptyFd: Int) {

    fun readFlow(): Flow<ByteArray> = callbackFlow {
        withContext(Dispatchers.IO) {
            val buffer = ByteBuffer.allocate(4096)
            while (isActive) {
                try {
                    val bytesRead = read(ptyFd, buffer.array(), 0, buffer.capacity())
                    if (bytesRead > 0) {
                        trySend(buffer.array().copyOf(bytesRead))
                    } else {
                        break
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
        close()
        awaitClose { }
    }

    private fun read(fd: Int, buf: ByteArray, offset: Int, length: Int): Int {
        return try {
            val readMethod = FileDescriptor::class.java.getMethod("read", ByteArray::class.java, Int::class.java, Int::class.java)
            readMethod.invoke(null, buf, offset, length) as Int
        } catch (e: Exception) {
            -1
        }
    }

    fun close() {
        try {
            val closeMethod = FileDescriptor::class.java.getMethod("close")
            closeMethod.invoke(null)
        } catch (e: Exception) {
        }
    }
}