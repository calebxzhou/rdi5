package calebxzhou.rdi.net

import io.netty.buffer.ByteBuf
import net.minecraft.network.FriendlyByteBuf
import org.bson.types.ObjectId

typealias RByteBuf = FriendlyByteBuf


/**
 * calebxzhou @ 2025-04-20 15:46
 */
const val MAX_VARINT21_BYTES: Int = 3
const val MAX_VARINT_SIZE = 5
const val DATA_BITS_MASK = 127
const val CONTINUATION_BIT_MASK = 128
const val DATA_BITS_PER_BYTE = 7
fun getVarIntByteSize(data: Int): Int {
    for (i in 1..4) {
        if ((data and (-1 shl i * 7)) == 0) {
            return i
        }
    }

    return 5
}
fun hasVarIntContinuationBit(data: Byte): Boolean {
    return (data.toInt() and 128) == 128
}
fun ByteBuf.writeObjectId(objectId: ObjectId): ByteBuf {
    writeBytes(objectId.toByteArray())
    return this
}

fun ByteBuf.readObjectId(): ObjectId = ObjectId(
    readBytes(12).nioBuffer()
)
fun ByteBuf.readVarInt() : Int {
    var i = 0
    var j = 0

    var b0: Byte
    do {
        b0 = this.readByte()
        i = i or ((b0.toInt() and 127) shl j++ * 7)
        if (j > 5) {
            throw RuntimeException("VarInt too big")
        }
    } while (hasVarIntContinuationBit(b0))

    return i
}
fun ByteBuf.writeVarInt(value : Int) : ByteBuf{
    var value = value
    while ((value and -128) != 0) {
        writeByte(value and 127 or 128)
        value = value ushr 7
    }

    writeByte(value)
    return this
}
fun RByteBuf.readString() = readUtf()
fun RByteBuf.writeString(str: String) = writeUtf(str)