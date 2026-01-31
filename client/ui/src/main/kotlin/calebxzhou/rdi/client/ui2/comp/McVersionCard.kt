package calebxzhou.rdi.client.ui2.comp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import calebxzhou.mykotutils.std.jarResource
import calebxzhou.rdi.RDIClient
import calebxzhou.rdi.client.model.firstLoader
import calebxzhou.rdi.client.model.metadata
import calebxzhou.rdi.client.service.GameService
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.common.model.McVersion
import calebxzhou.rdi.common.model.Task
import org.jetbrains.skia.Image
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * calebxzhou @ 2026-01-29 18:44
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McVersionCard(
    mcver: McVersion,
    highlight: Boolean = false,
    onOpenTask: ((Task) -> Unit)? = null
) {
    val iconBitmap = remember(mcver) {
        RDIClient.jarResource(mcver.icon).use { stream ->
            Image.makeFromEncoded(stream.readBytes()).toComposeImageBitmap()
        }
    }
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (highlight) {
                    Modifier
                        .shadow(8.dp, shape, clip = false)
                        .border(2.dp, MaterialColor.PURPLE_500.color, shape)
                } else {
                    Modifier
                }
            ),
        color = Color(0xFFF9F9FB),
        shape = shape,
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialColor.GRAY_200.color, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = "MC ${mcver.mcVer}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MC ${mcver.mcVer}",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialColor.GRAY_900.color
                    )
                    Text(
                        text = mcver.loaderVersions.keys.joinToString(" / ") { it.name.lowercase() },
                        style = MaterialTheme.typography.body2,
                        color = MaterialColor.GRAY_700.color
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircleIconButton("\uF019", "下载全部所需文件") {
                        onOpenTask?.invoke(GameService.downloadVersion(mcver, mcver.firstLoader))
                    }
                    CircleIconButton("\uF305", "仅下载MC核心", bgColor = Color.Gray) {
                        onOpenTask?.invoke(GameService.downloadClient(mcver.metadata))
                    }
                    CircleIconButton("\uDB84\uDE5F", "仅下载运行库", bgColor = Color.Gray) {
                        onOpenTask?.invoke(GameService.downloadLibraries(mcver.metadata.libraries))
                    }
                    CircleIconButton("\uF001", "仅下载音频资源", bgColor = Color.Gray) {
                        onOpenTask?.invoke(GameService.downloadAssets(mcver.metadata))
                    }
                    mcver.loaderVersions.forEach { (loader, _) ->
                        CircleIconButton("\uEEFF", "安装${loader.name.lowercase()}", bgColor = Color.Gray) {
                            onOpenTask?.invoke(GameService.downloadLoader(mcver, loader))
                        }
                    }
                }
            }
        }
    }
}
