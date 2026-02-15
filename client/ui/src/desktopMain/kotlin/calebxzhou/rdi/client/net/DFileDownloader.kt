package calebxzhou.rdi.client.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.io.path.Path

actual suspend fun writeFileBytes(filePath: String, bytes: ByteArray, append: Boolean) {
    withContext(Dispatchers.IO) {
        FileOutputStream(filePath, append).use { it.write(bytes) }
    }
}

actual suspend fun ensureParentDirs(filePath: String) {
    withContext(Dispatchers.IO) {
        val parent = Path(filePath).parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }
    }
}
