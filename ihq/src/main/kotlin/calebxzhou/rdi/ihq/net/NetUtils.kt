package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.exception.AuthError
import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.model.RAccount
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.DecoderException
import io.netty.handler.codec.EncoderException
import io.netty.util.AttributeKey
import org.bson.types.ObjectId
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveParameters
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.benwoodworth.knbt.Nbt
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.NbtContentPolymorphicSerializer
import net.benwoodworth.knbt.NbtTag
import net.benwoodworth.knbt.internal.NbtTagType

/**
 * calebxzhou @ 2024-06-07 16:54
 */
typealias RByteBuf = ByteBuf
const val MAX_VARINT21_BYTES: Int = 3
const val MAX_VARINT_SIZE = 5
const val DATA_BITS_MASK = 127
const val CONTINUATION_BIT_MASK = 128
const val DATA_BITS_PER_BYTE = 7
fun createByteBuf(): RByteBuf{
    return io.netty.buffer.Unpooled.buffer()
}
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
fun ByteBuf.writeByteArray(array: ByteArray) {
    writeVarInt(array.size)
    writeBytes(array)
}
fun ByteBuf.readByteArray(): ByteArray {
    val size = readVarInt()
    if (size < 0) {
        throw DecoderException("Received byte array with negative size: $size")
    }
    val array = ByteArray(size)
    readBytes(array)
    return array
}
fun ByteBuf.writeByte(byte: Byte){
    writeByte(byte.toInt())
}
fun ByteBuf.writeNbt(nbt: NbtCompound){
    val array = ByteArray(2*1024*1024)
    Nbt {  }.encodeToByteArray(array)
    writeByteArray(array)
}
fun ByteBuf.readNbt(): NbtCompound{
    return Nbt {  }.decodeFromByteArray(readByteArray())
}
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
fun ByteBuf.writeVarIntArray(array: IntArray): ByteBuf {
    writeVarInt(array.size)
    for (value in array) {
        writeVarInt(value)
    }
    return this
}
fun ByteBuf.readVarIntArray(): IntArray {
    val size = readVarInt()
    if (size < 0) {
        throw DecoderException("Received varint array with negative size: $size")
    }
    val array = IntArray(size)
    for (i in 0 until size) {
        array[i] = readVarInt()
    }
    return array
}
fun ByteBuf.readUUID(): java.util.UUID {
    val mostSigBits = readLong()
    val leastSigBits = readLong()
    return java.util.UUID(mostSigBits, leastSigBits)
}
fun ByteBuf.writeUUID(uuid: java.util.UUID): ByteBuf {
    writeLong(uuid.mostSignificantBits)
    writeLong(uuid.leastSignificantBits)
    return this
}
fun getVarLongSize(input: Long): Int {
    for (i in 1..9) {
        if (input and (-1L shl i * 7) == 0L) {
            return i
        }
    }
    return 10
}

fun getVarIntSize(input: Int): Int {
    for (i in 1..4) {
        if (input and (-1 shl i * 7) == 0) {
            return i
        }
    }
    return 5
}

private fun getMaxEncodedUtfLength(i: Int): Int {
    return i * 3
}

fun ByteBuf.writeObjectId(objectId: ObjectId): ByteBuf {
    writeBytes(objectId.toByteArray())
    return this
}

fun ByteBuf.readObjectId(): ObjectId = ObjectId(
    readBytes(12).nioBuffer()
)

fun ByteBuf.readString(): String {
    return this.readString(32767)
}

fun ByteBuf.readString(i: Int): String {
    val j: Int = getMaxEncodedUtfLength(i)
    val k = readVarInt()
    return if (k > j) {
        throw DecoderException("The received encoded string buffer length is longer than maximum allowed ($k > $j)")
    } else if (k < 0) {
        throw DecoderException("The received encoded string buffer length is less than zero! Weird string!")
    } else {
        val string = this.toString(this.readerIndex(), k, StandardCharsets.UTF_8)
        this.readerIndex(this.readerIndex() + k)
        if (string.length > i) {
            val var10002 = string.length
            throw DecoderException("The received string length is longer than maximum allowed ($var10002 > $i)")
        } else {
            string
        }
    }
}

fun ByteBuf.writeString(string: String): ByteBuf {
    return this.writeString(string, 32767)
}

fun ByteBuf.writeString(string: String, i: Int): ByteBuf {
    return if (string.length > i) {
        val var10002 = string.length
        throw EncoderException("String too big (was $var10002 characters, max $i)")
    } else {
        val bs = string.toByteArray(StandardCharsets.UTF_8)
        val j: Int = getMaxEncodedUtfLength(i)
        if (bs.size > j) {
            throw EncoderException("String too big (was " + bs.size + " bytes encoded, max " + j + ")")
        } else {
            writeVarInt(bs.size)
            this.writeBytes(bs)
            this
        }
    }
}




fun <T> ChannelHandlerContext.got(key: String): T? {
    return this.channel().attr<T>(AttributeKey.valueOf<T>(key)).get()
}

operator fun <T> ChannelHandlerContext.set(key: String, value: T) {
    this.channel().attr<T>(AttributeKey.valueOf<T>(key)).set(value)
}

var ChannelHandlerContext.clientIp: InetSocketAddress
    get() = this.got("clientIp")!!
    set(value) = this.set("clientIp", value)


var ChannelHandlerContext.account: RAccount?
    get() = this.got("account")
    set(value) = this.set("account", value)
