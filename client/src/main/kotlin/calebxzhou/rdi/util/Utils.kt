package calebxzhou.rdi.util

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.bson.types.ObjectId
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.util.*

/**
 * calebxzhou @ 2025-04-16 12:23
 */

fun notifyOs(msg: String){
    TinyFileDialogs.tinyfd_notifyPopup("RDI提示", msg,"info" )
}
val ioScope: CoroutineScope
    get() = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
    throwable.printStackTrace()
}
)
//保留小数点后x位
fun Float.toFixed(decPlaces: Int): String{
    return String.format("%.${decPlaces}f",this)
}
fun Double.toFixed(decPlaces: Int): String{
    return this.toFloat().toFixed(decPlaces)
}
fun UUID.toBytes(): ByteArray {
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.putLong(this.mostSignificantBits)
    bb.putLong(this.leastSignificantBits)
    return bb.array()
}
fun UUID.toObjectId() : ObjectId {
    val uuidBytes = this.toBytes()
    val objectIdBytes = uuidBytes.sliceArray(0..11)
    return ObjectId(objectIdBytes)
}
val UUID.objectId
    get() = this.toObjectId()
fun ObjectId.toUUID() : UUID{
    val objectIdBytes = this.toByteArray()
    val bb = ByteBuffer.wrap(ByteArray(16))
    bb.put(objectIdBytes)
    return UUID(bb.getLong(0), bb.getLong(8))
}
val String.urlEncoded
    get() = URLEncoder.encode(this, Charsets.UTF_8)