package calebxzhou.rdi.util

import calebxzhou.rdi.RDI
import calebxzhou.rdi.net.formatBytes
import calebxzhou.rdi.net.formatSpeed
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.jvm.optionals.getOrNull

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
    get() = URLDecoder.decode(this, Charsets.UTF_8)
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
val javaExePath = ProcessHandle.current()
    .info()
    .command().orElseThrow { IllegalArgumentException("找不到java运行路径") }
val File.sha1: String
    get() = digest("SHA-1")
val File.sha256: String
    get() = digest("SHA-256")
val File.md5: String
    get() = digest("MD5")
val File.sha512: String
    get() = digest("SHA-512")
val File.murmur2: Long
    get() {
        val multiplex = 1540483477u
        val normalizedLength = computeNormalizedLength()

        var num2 = 1u xor normalizedLength
        var num3 = 0u
        var num4 = 0

        val buffer = ByteArray(8192)
        inputStream().use { stream ->
            while (true) {
                val read = stream.read(buffer)
                if (read == -1) break

                for (i in 0 until read) {
                    val byte = buffer[i]
                    if (byte.isWhitespaceCharacter()) continue

                    val value = (byte.toInt() and 0xFF).toUInt()
                    num3 = num3 or (value shl num4)
                    num4 += 8

                    if (num4 == 32) {
                        val num6 = num3 * multiplex
                        val num7 = (num6 xor (num6 shr 24)) * multiplex
                        num2 = num2 * multiplex xor num7
                        num3 = 0u
                        num4 = 0
                    }
                }
            }
        }

        if (num4 > 0) {
            num2 = (num2 xor num3) * multiplex
        }

        var num6 = (num2 xor (num2 shr 13)) * multiplex
        num6 = num6 xor (num6 shr 15)
        return num6.toLong()
    }

fun File.computeNormalizedLength(): UInt {
    var count = 0u
    val buffer = ByteArray(8192)
    inputStream().use { stream ->
        while (true) {
            val read = stream.read(buffer)
            if (read == -1) break

            for (i in 0 until read) {
                if (!buffer[i].isWhitespaceCharacter()) {
                    count += 1u
                }
            }
        }
    }
    return count
}

fun Byte.isWhitespaceCharacter(): Boolean {
    return when (this.toInt() and 0xFF) {
        9, 10, 13, 32 -> true
        else -> false
    }
}
val Long.humanSize: String
    get() = formatBytes(this)
val Int.humanSize: String
    get() = toLong().humanSize
val Double.humanSpeed:String
    get() = formatSpeed(this)
val humanDateTime
    get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
val ObjectId.humanDateTime
    get() = (this.timestamp* 1000L).humanDateTime
val Long.humanDateTime
    get() = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
fun jarResource(path: String): InputStream = RDI::class.java.classLoader.getResourceAsStream(path)?:run{
    throw IllegalArgumentException("Resource not found: $path")}
fun exportJarResource(path: String, dest: File?=null): File {
    jarResource(path).use { input ->
        val dest = dest?: RDI.DIR.resolve(path)
        dest.outputStream().use { output ->
            input.copyTo(output)
        }
        return dest
    }
}