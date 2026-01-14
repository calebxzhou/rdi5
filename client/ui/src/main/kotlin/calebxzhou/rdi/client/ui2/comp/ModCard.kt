package calebxzhou.rdi.client.ui2.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.common.model.Mod

/**
 * calebxzhou @ 2026-01-13 21:28
 */
@Composable
fun Mod.CardVo.ModCard(modifier: Modifier = Modifier) {
    /*
                nameCn      name
        icon
                intro
                */
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(Color(255, 255, 255, 235), RoundedCornerShape(16.dp))
            .padding(12.dp, 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        HttpImage(
            imgUrl = iconUrls.firstOrNull { it.isNotBlank() } ?: "",
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(240, 240, 240, 255)),
            contentDescription = name
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            // Title Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                val hasChineseName = !nameCn.isNullOrBlank()
                val primaryText = if (hasChineseName) nameCn!!.trim() else name.trim()

                Text(
                    text = primaryText.ifBlank { name.trim() },
                    color = MaterialColor.GRAY_900.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (hasChineseName) {
                    val secondaryTrimmed = name.trim()
                    if (secondaryTrimmed.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = secondaryTrimmed, // Shortening logic handled by text overflow
                            color = MaterialColor.BLUE_600.color,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))

            // Intro
            Text(
                text = intro.ifBlank { "暂无简介" },
                color = (MaterialColor.GRAY_700.color),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.6.sp // 1.2 * 13sp
            )
        }
    }
}
