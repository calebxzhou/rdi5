package calebxzhou.rdi.ihq.util

import calebxzhou.rdi.ihq.RDI
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
import kotlin.jvm.java

/**
 * calebxzhou @ 2024-06-20 16:46
 */
private val realIoScope = CoroutineScope(Dispatchers.IO)
@Volatile
internal var testIoScope: CoroutineScope? = null
val ioScope: CoroutineScope
    get() = testIoScope ?: realIoScope
inline fun ioTask(crossinline handler: suspend () -> Unit) = ioScope.launch { handler() }
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
    get() = URLDecoder.decode(this, Charsets.UTF_8)
val String.decodeBase64
    get() = String(Base64.getDecoder().decode(this), Charsets.UTF_8)
val String.encodeBase64
    get() = Base64.getEncoder().encodeToString(this.toByteArray(Charsets.UTF_8))
fun String.isValidUuid(): Boolean {
    val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$".toRegex()
    return uuidRegex.matches(this)
}

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

fun ObjectId.toUUID(): UUID {
    val objectIdBytes = this.toByteArray()
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.put(objectIdBytes)
    bb.putLong(0)
    return UUID(bb.getLong(0), bb.getLong(8))
}

val datetime
    get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
val humanDateTime
    get() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

fun Byte.isWhitespaceCharacter(): Boolean {
    return when (this.toInt() and 0xFF) {
        9, 10, 13, 32 -> true
        else -> false
    }
}
fun File.symlinkRecursively(sourceDir: File){

}
fun jarResource(path: String): InputStream = RDI::class.java.classLoader.getResourceAsStream(path)?:run{
    throw IllegalArgumentException("Resource not found: $path")}
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