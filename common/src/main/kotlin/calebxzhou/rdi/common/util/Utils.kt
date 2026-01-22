package calebxzhou.rdi.common.util

import calebxzhou.mykotutils.std.displayLength
import calebxzhou.rdi.common.exception.RequestError
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import java.nio.ByteBuffer
import java.util.*

fun ObjectId.toUUID(): UUID {
    val objectIdBytes = this.toByteArray()
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.put(objectIdBytes)
    return UUID(bb.getLong(0), bb.getLong(8))
}
fun UUID.toBytes(): ByteArray {
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.putLong(this.mostSignificantBits)
    bb.putLong(this.leastSignificantBits)
    return bb.array()
}

val UUID.objectId : ObjectId
    get() {
        val uuidBytes = this.toBytes()
        val objectIdBytes = uuidBytes.sliceArray(0..11)
        return ObjectId(objectIdBytes)

    }
val ioScope: CoroutineScope
    get() = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }
    )
fun ioTask(handler: suspend () -> Unit) = ioScope.launch { handler() }

val ObjectId.str get() = toHexString()
fun validateName(name: String): Result<Unit> {
    val trimmed = name.trim()
    val len = trimmed.displayLength
    if (len !in 3..32) throw RequestError("名称长度需在3~32个字符（一个汉字算两个）")
    return Result.success(Unit)
}