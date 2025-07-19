package calebxzhou.rdi.ihq.util

/**
 * calebxzhou @ 2025-07-12 23:11
 */
import org.bson.*
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistry

class IntKeyMapCodec<T>(private val valueCodec: Codec<T>, private val registry: CodecRegistry) : Codec<Map<Int, T>> {
    override fun encode(writer: BsonWriter, value: Map<Int, T>, encoderContext: EncoderContext) {
        writer.writeStartDocument()
        value.forEach { (key, value) ->
            writer.writeName(key.toString()) // Convert Int key to String
            valueCodec.encode(writer, value, encoderContext)
        }
        writer.writeEndDocument()
    }

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): Map<Int, T> {
        val map = mutableMapOf<Int, T>()
        reader.readStartDocument()
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            val key = reader.readName().toInt() // Convert String key back to Int
            val value = valueCodec.decode(reader, decoderContext)
            map[key] = value
        }
        reader.readEndDocument()
        return map
    }

    override fun getEncoderClass(): Class<Map<Int, T>> = Map::class.java as Class<Map<Int, T>>
}