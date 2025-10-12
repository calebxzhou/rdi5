package calebxzhou.rdi.util

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*

/**
 * calebxzhou @ 2025-04-16 12:23
 */

fun notifyOs(msg: String) {
    TinyFileDialogs.tinyfd_notifyPopup("RDI提示", msg, "info")
}

val ioScope: CoroutineScope
    get() = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }
    )

fun ioTask(handler: suspend () -> Unit) = ioScope.launch { handler() }

//保留小数点后x位
fun Float.toFixed(decPlaces: Int): String {
    return String.format("%.${decPlaces}f", this)
}

fun Double.toFixed(decPlaces: Int): String {
    return this.toFloat().toFixed(decPlaces)
}

fun UUID.toBytes(): ByteArray {
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.putLong(this.mostSignificantBits)
    bb.putLong(this.leastSignificantBits)
    return bb.array()
}

fun UUID.toObjectId(): ObjectId {
    val uuidBytes = this.toBytes()
    val objectIdBytes = uuidBytes.sliceArray(0..11)
    return ObjectId(objectIdBytes)
}

val UUID.objectId
    get() = this.toObjectId()

fun ObjectId.toUUID(): UUID {
    val objectIdBytes = this.toByteArray()
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.put(objectIdBytes)
    return UUID(bb.getLong(0), bb.getLong(8))
}

val String.urlEncoded
    get() = URLEncoder.encode(this, Charsets.UTF_8)
val String.urlDecoded
    get() = java.net.URLDecoder.decode(this, Charsets.UTF_8)
val String.decodeBase64
    get() = String(Base64.getDecoder().decode(this), Charsets.UTF_8)
val String.encodeBase64
    get() = Base64.getEncoder().encodeToString(this.toByteArray(Charsets.UTF_8))

fun File.digest(algo: String): String {
    val digest = MessageDigest.getInstance(algo)
    inputStream().use { input ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

val File.sha1: String
    get() = digest("SHA-1")
val File.sha256: String
    get() = digest("SHA-256")
val File.md5: String
    get() = digest("MD5")
val File.sha512: String
    get() = digest("SHA-512")


