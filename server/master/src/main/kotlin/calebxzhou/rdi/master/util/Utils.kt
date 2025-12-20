package calebxzhou.rdi.master.util

import calebxzhou.mykotutils.std.isWideCodePoint
import calebxzhou.rdi.master.RDI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * calebxzhou @ 2024-06-20 16:46
 */
private val realIoScope = CoroutineScope(Dispatchers.IO)
@Volatile
internal var testIoScope: CoroutineScope? = null
val ioScope: CoroutineScope
    get() = testIoScope ?: realIoScope


val ObjectId.str get() = toHexString()



/**
 * Recursively delete a directory and all its contents, but when encountering a symbolic link,
 * only delete the link itself, not the target it points to.
 */
fun File.deleteRecursivelyNoSymlink() {
    val path = this.toPath()
    
    if (!this.exists()) {
        return
    }
    
    // If this is a symbolic link, just delete the link itself
    if (Files.isSymbolicLink(path)) {
        Files.delete(path)
        return
    }
    
    // If it's a directory, recursively delete its contents first
    if (this.isDirectory) {
        this.listFiles()?.forEach { child ->
            child.deleteRecursivelyNoSymlink()
        }
    }
    
    // Finally delete this file/directory
    this.delete()
}