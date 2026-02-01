package calebxzhou.rdi.client.ui2.comp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import calebxzhou.rdi.common.net.httpRequest
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

/**
 * calebxzhou @ 2026-01-12 23:34
 */
@Composable
fun HttpImage(
    imgUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    val state = produceState(initialValue = HttpImageState.loading(), imgUrl) {
        value = HttpImageState.loading()
        value = withContext(Dispatchers.IO) {
            fetch(imgUrl)
        }
    }.value

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.wrapContentSize())
            }
            state.bitmap != null -> {
                Image(
                    bitmap = state.bitmap,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            else -> {
                Text(
                    text = state.error ?: "图片加载失败",
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

private data class HttpImageState(
    val bitmap: ImageBitmap?,
    val error: String?,
    val isLoading: Boolean
) {
    companion object {
        fun loading(): HttpImageState = HttpImageState(null, null, true)
    }
}

private suspend fun fetch(url: String): HttpImageState {
    return runCatching {
        val response = httpRequest { url(url) }
        if (!response.status.isSuccess()) {
            return HttpImageState(null, "图片加载失败", false)
        }
        val bytes = response.bodyAsBytes()
        val bitmap = Image.makeFromEncoded(bytes).use { it.toComposeImageBitmap() }
        HttpImageState(bitmap, null, false)
    }.getOrElse {
        HttpImageState(null, "图片加载失败", false)
    }
}
