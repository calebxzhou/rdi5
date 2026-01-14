package calebxzhou.rdi.client.ui2.comp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.ui2.DEFAULT_HOST_ICON
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.client.ui2.asIconText
import calebxzhou.rdi.common.model.Host

/**
 * calebxzhou @ 2026-01-14 21:48
 */
@Composable
fun Host.Vo.HostCard(
    modifier: Modifier = Modifier,
    onClickPlay: ((Host.Vo) -> Unit)? = null,
    onClick: ((Host.Vo) -> Unit)? = null
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable { onClick(this) }
    } else {
        modifier
    }

    Surface(
        modifier = cardModifier.fillMaxWidth(),
        color = Color(0xFFF9F9FB),
        shape = RoundedCornerShape(16.dp),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconBg = MaterialColor.GRAY_200.color
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val iconUrl = iconUrl?.takeIf { it.isNotBlank() }
                if (iconUrl != null) {
                    HttpImage(
                        imgUrl = iconUrl,
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    Image(
                        bitmap = DEFAULT_HOST_ICON,
                        contentDescription = "Host Icon",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$modpackName $packVer",
                    style = MaterialTheme.typography.body2,
                    color = MaterialColor.GRAY_500.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = intro,
                        style = MaterialTheme.typography.caption,
                        color = MaterialColor.GRAY_500.color,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { onClickPlay?.invoke(this@HostCard) },
                        enabled = onClickPlay != null,
                        colors = ButtonDefaults.textButtonColors(
                            backgroundColor = MaterialColor.GREEN_900.color,
                            contentColor = Color.White
                        ),
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("▶".asIconText)
                    }
                }
            }
        }
    }
}
