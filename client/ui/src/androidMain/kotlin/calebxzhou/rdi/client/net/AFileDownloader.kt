package calebxzhou.rdi.client.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual suspend fun writeFileBytes(filePath: String, bytes: ByteArray, append: Boolean) {
    withContext(Dispatchers.IO) {
        FileOutputStream(filePath, append).use { it.write(bytes) }
    }
}

actual suspend fun ensureParentDirs(filePath: String) {
    withContext(Dispatchers.IO) {
        val parent = File(filePath).parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
    }
}
