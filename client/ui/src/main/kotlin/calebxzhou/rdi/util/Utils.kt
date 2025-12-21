package calebxzhou.rdi.util

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.nio.ByteBuffer
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


fun ObjectId.toUUID(): UUID {
    val objectIdBytes = this.toByteArray()
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.put(objectIdBytes)
    return UUID(bb.getLong(0), bb.getLong(8))
}
