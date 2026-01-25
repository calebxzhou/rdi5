package calebxzhou.rdi.client.ui2.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.std.humanFileSize
import calebxzhou.rdi.client.CodeFontFamily
import calebxzhou.rdi.client.ui2.DEFAULT_MODPACK_ICON
import calebxzhou.rdi.client.IconFontFamily
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.common.model.Modpack

/**
 * calebxzhou @ 2026-01-13 16:14
 */
@Composable
fun Modpack.BriefVo.ModpackCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = clickableModifier.fillMaxWidth(),
        color = Color(0xFFF9F9FB),
        shape = RoundedCornerShape(16.dp),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconBg = MaterialColor.GRAY_200.color
            val iconUrl = icon?.takeIf { it.isNotBlank() }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(iconBg, RoundedCornerShape(12.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (iconUrl != null) {
                    HttpImage(
                        imgUrl = iconUrl,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = name
                    )
                } else {
                    androidx.compose.foundation.Image(
                        bitmap = DEFAULT_MODPACK_ICON,
                        contentDescription = "Modpack Icon",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = name.ifBlank { "未命名模组包" },
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialColor.GRAY_900.color
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "\uEAEF",
                        fontFamily = IconFontFamily,
                        style = MaterialTheme.typography.body1,
                        color = MaterialColor.GRAY_900.color,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = fileSize.humanFileSize,
                        fontFamily = CodeFontFamily,
                        style = MaterialTheme.typography.body2,
                        color = MaterialColor.GRAY_900.color,
                        modifier = Modifier.alignByBaseline()
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "\uDB85\uDCD3",
                        fontFamily = IconFontFamily,
                        style = MaterialTheme.typography.body1,
                        color = MaterialColor.GRAY_900.color,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = modCount.toString(),
                        fontFamily = CodeFontFamily,
                        style = MaterialTheme.typography.body2,
                        color = MaterialColor.GRAY_900.color,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                Text(
                    text = info.trim().ifBlank { "" },
                    style = MaterialTheme.typography.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialColor.GRAY_700.color
                )
            }
        }
    }
}
