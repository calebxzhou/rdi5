package calebxzhou.rdi.net

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * calebxzhou @ 2025-05-10 23:20
 */

val HttpResponse<String>.body
    get() = this.body()

fun HttpRequestBuilder.accountAuthHeader(){
    RAccount.now?.let {
        header(HttpHeaders.Authorization, "Bearer ${it.jwt}")
    }
}