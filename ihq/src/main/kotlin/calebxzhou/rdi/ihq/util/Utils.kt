package calebxzhou.rdi.ihq.util

import calebxzhou.rdi.ihq.exception.ParamError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.bson.types.ObjectId
import java.io.File
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.reflect.KCallable

/**
 * calebxzhou @ 2024-06-20 16:46
 */
val scope = CoroutineScope(Dispatchers.IO)
/**
 * Display length where CJK (Chinese/Japanese/Korean) full‑width characters and common emoji count as 2 cells,
 * others count as 1. Useful for monospace alignment / padding.
 */
val String.displayLength: Int
    get() {
        var len = 0
        var i = 0
        while (i < length) {
            val cp = codePointAt(i)
            val count = Character.charCount(cp)
            len += if (cp.isWideCodePoint()) 2 else 1
            i += count
        }
        return len
    }
// Heuristic for wide code points. Covers:
// - CJK Unified Ideographs & Extensions
// - Hangul syllables & Jamo
// - Hiragana, Katakana, Bopomofo
// - Fullwidth and Halfwidth forms (treat fullwidth as wide)
// - Enclosed CJK, Compatibility Ideographs
// - Common emoji blocks (Emoticons, Misc Symbols & Pictographs, Supplemental Symbols & Pictographs, etc.)
private fun Int.isWideCodePoint(): Boolean {
    // Fast path ranges
    return when {
        // CJK Unified Ideographs & Ext
        this in 0x4E00..0x9FFF || this in 0x3400..0x4DBF || this in 0x20000..0x2A6DF || this in 0x2A700..0x2B73F || this in 0x2B740..0x2B81F || this in 0x2B820..0x2CEAF -> true
        // Hangul
        this in 0xAC00..0xD7A3 || this in 0x1100..0x11FF || this in 0x3130..0x318F -> true
        // Hiragana / Katakana / Phonetic extensions
        this in 0x3040..0x309F || this in 0x30A0..0x30FF || this in 0x31F0..0x31FF || this in 0x1B000..0x1B0FF -> true
        // Bopomofo
        this in 0x3100..0x312F || this in 0x31A0..0x31BF -> true
        // Fullwidth forms
        this in 0xFF01..0xFF60 || this in 0xFFE0..0xFFE6 -> true
        // Enclosed / compatibility
        this in 0x3200..0x32FF || this in 0x3300..0x33FF || this in 0xF900..0xFAFF || this in 0x2F800..0x2FA1F -> true
        // Common emoji (approximate). Treat them as wide for alignment.
        this in 0x1F300..0x1F64F || this in 0x1F680..0x1F6FF || this in 0x1F900..0x1F9FF || this in 0x1FA70..0x1FAFF || this in 0x2600..0x26FF || this in 0x2700..0x27BF -> true
        else -> false
    }
}

val String.urlEncoded
    get() = URLEncoder.encode(this, Charsets.UTF_8)
val String.urlDecoded
    get() = java.net.URLDecoder.decode(this, Charsets.UTF_8)
val String.decodeBase64
    get() = String(Base64.getDecoder().decode(this), Charsets.UTF_8)
val String.encodeBase64
    get() = Base64.getEncoder().encodeToString(this.toByteArray(Charsets.UTF_8))
fun String.isValidUuid(): Boolean {
    val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
    return uuidRegex.matches(this)
}
fun java.io.File.safeDirSize(): Long =
    if (!exists()) 0L else walkTopDown().filter { it.isFile }.sumOf { it.length() }
fun String.isValidObjectId(): Boolean {
    return ObjectId.isValid(this)
}

fun String?.isValidHttpUrl(): Boolean {
    if (this == null)
        return false
    val urlRegex = "^(http://|https://).+".toRegex()
    return this.matches(urlRegex)
}
val ObjectId.str get() = toHexString()
private const val ENCRYPT_KEY = "wfygiqh%^(*!@&#$%()*qGH83876127RDI"
fun String.encrypt(): String {
    val key = ENCRYPT_KEY.toByteArray(charset("UTF-8"))
    val secretKey = SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val encryptedValue = cipher.doFinal(this.toByteArray())
    return Base64.getEncoder().encodeToString(encryptedValue)
}

fun String.decrypt(): String {
    val key = ENCRYPT_KEY.toByteArray(charset("UTF-8"))
    val secretKey = SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.DECRYPT_MODE, secretKey)
    val decryptedValue = cipher.doFinal(Base64.getDecoder().decode(this))
    return String(decryptedValue)
}

fun String.isNumber(): Boolean {
    return this.isNotEmpty() && this.all { it.isDigit() }
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

fun ObjectId.toUUID(): UUID {
    val objectIdBytes = this.toByteArray()
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.put(objectIdBytes)
    bb.putLong(0)
    return UUID(bb.getLong(0), bb.getLong(8))
}

val datetime
    get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))

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