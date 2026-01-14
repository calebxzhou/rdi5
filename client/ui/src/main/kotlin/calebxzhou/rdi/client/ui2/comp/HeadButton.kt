package calebxzhou.rdi.client.ui2.comp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.service.PlayerInfoCache
import calebxzhou.rdi.common.net.httpRequest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.types.ObjectId
import org.jetbrains.skia.Image

/**
 * calebxzhou @ 2026-01-14 19:53
 */
@Composable
fun HeadButton(
    uid: ObjectId,
    onClick: () -> Unit = {}
) {
    var name by remember { mutableStateOf("载入中...") }
    var skinImage by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uid) {
        withContext(Dispatchers.IO) {
            try {
                val info = PlayerInfoCache[uid]
                name = info.name
                val response = httpRequest { url(info.cloth.skin) }
                if (response.status.isSuccess()) {
                    val bytes = response.bodyAsBytes()
                    val skiaImage = Image.makeFromEncoded(bytes)
                    // Legacy skins are 64x32, modern are 64x64. Both have head at (8,8) and hat at (40,8).
                    if (skiaImage.width >= 64 && skiaImage.height >= 32) {
                        skinImage = skiaImage.toComposeImageBitmap()
                    }
                }
            } catch (e: Exception) {
                name = "加载失败"
            }
        }
    }

    Row(
        modifier = Modifier
            .defaultMinSize(minWidth = 120.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(modifier = Modifier.size(24.dp)) {
            val img = skinImage
            if (img != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width.toInt()
                    val h = size.height.toInt()
                    // Draw Head
                    drawImage(
                        image = img,
                        srcOffset = IntOffset(8, 8),
                        srcSize = IntSize(8, 8),
                        dstSize = IntSize(w, h),
                        filterQuality = FilterQuality.None
                    )
                    // Draw Hat (Overlay)
                    drawImage(
                        image = img,
                        srcOffset = IntOffset(40, 8),
                        srcSize = IntSize(8, 8),
                        dstSize = IntSize(w, h),
                        filterQuality = FilterQuality.None
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = Color(0xFFA0A0A0))
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(text = name)
    }
}
