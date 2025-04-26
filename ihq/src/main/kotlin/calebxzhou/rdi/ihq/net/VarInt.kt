package calebxzhou.rdi.ihq.net

import io.ktor.utils.io.*
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled.buffer
import io.netty.handler.codec.DecoderException

object VarInt {
    private const val MAX_VARINT_SIZE = 5
    private const val DATA_BITS_MASK = 0x7F // 127
    private const val CONTINUATION_BIT_MASK = 0x80 // 128
    private const val DATA_BITS_PER_BYTE = 7

    // Calculate the number of bytes needed to encode a VarInt
    fun getByteSize(data: Int): Int {
        for (i in 1 until 5) {
            if (data and (-1 shl (i * 7)) == 0) {
                return i
            }
        }
        return 5
    }

    // Check if a byte has the continuation bit set
    fun hasContinuationBit(data: Byte): Boolean {
        return (data.toInt() and CONTINUATION_BIT_MASK) == CONTINUATION_BIT_MASK
    }

    // Read a VarInt from a ByteReadChannel
    fun ByteBuf.readVarInt(): Int {
        var result = 0
        var shift = 0
        var byte: Byte

        do {
            byte = readByte()
            result = result or ((byte.toInt() and DATA_BITS_MASK) shl (shift * DATA_BITS_PER_BYTE))
            shift++
            if (shift > MAX_VARINT_SIZE) {
                throw RuntimeException("VarInt too big")
            }
        } while (hasContinuationBit(byte))

        return result
    }

    // Write a VarInt to a ByteWriteChannel
    fun ByteBuf.writeVarInt(value: Int): ByteBuf {
        var newValue = value
        while ((value and -128) != 0) {
            writeByte(value and 127 or 128)
            newValue = value ushr 7
        }

        writeByte(value)
        return this
    }
    /**
     * Writes an array of VarInts to the buffer, prefixed by the length of the array (as a VarInt).
     *
     * @see .readVarIntArray
     *
     *
     * @param array the array to write
     */
    fun ByteBuf.writeVarIntArray(array: IntArray): ByteBuf {
        writeVarInt(array.size)
        for (i in array) {
            writeVarInt(i)
        }
        return this
    }

    /**
     * Reads an array of VarInts from this buffer.
     *
     * @see .writeVarIntArray
     */
    fun ByteBuf.readVarIntArray(): IntArray {
        return readVarIntArray(readableBytes())
    }

    fun ByteBuf.readVarIntArray(maxLength: Int): IntArray {
        val i = readVarInt()
        return if (i > maxLength) {
            throw DecoderException("VarIntArray with size $i is bigger than allowed $maxLength")
        } else {
            val `is` = IntArray(i)
            for (j in `is`.indices) {
                `is`[j] = readVarInt()
            }
            `is`
        }
    }

    // Helper to encode a VarInt to a ByteArray (for client-side testing)
    fun encodeToByteArray(value: Int): ByteArray {
        val bytes = mutableListOf<Byte>()
        var temp = value
        while (temp and -CONTINUATION_BIT_MASK != 0) {
            bytes.add(((temp and DATA_BITS_MASK) or CONTINUATION_BIT_MASK).toByte())
            temp = temp ushr DATA_BITS_PER_BYTE
        }
        bytes.add(temp.toByte())
        return bytes.toByteArray()
    }
}