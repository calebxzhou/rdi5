package calebxzhou.rdi.ihq.util

import calebxzhou.rdi.ihq.exception.ParamError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.bson.types.ObjectId
import java.nio.ByteBuffer
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * calebxzhou @ 2024-06-20 16:46
 */
val scope = CoroutineScope(Dispatchers.IO)
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

