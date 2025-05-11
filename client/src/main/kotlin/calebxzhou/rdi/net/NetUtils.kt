package calebxzhou.rdi.net

import calebxzhou.rdi.Const
import calebxzhou.rdi.lgr
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import net.minecraft.network.FriendlyByteBuf

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.netty.buffer.ByteBuf
import kotlinx.coroutines.runBlocking
import net.minecraft.nbt.CompoundTag
import org.bson.types.ObjectId
import java.nio.charset.StandardCharsets

typealias RByteBuf = FriendlyByteBuf


/**
 * calebxzhou @ 2025-04-20 15:46
 */

fun ByteBuf.writeObjectId(objectId: ObjectId): ByteBuf {
    writeBytes(objectId.toByteArray())
    return this
}

fun ByteBuf.readObjectId(): ObjectId = ObjectId(
    readBytes(12).nioBuffer()
)