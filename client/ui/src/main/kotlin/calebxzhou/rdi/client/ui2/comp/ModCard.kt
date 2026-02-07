package calebxzhou.rdi.client.ui2.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import calebxzhou.rdi.client.ui2.wM
import calebxzhou.rdi.common.model.Mod

/**
 * calebxzhou @ 2026-01-13 21:28
 */
@Composable
fun Mod.CardVo.ModCard(
    modifier: Modifier = Modifier,
    currentSide: Mod.Side = side,
    onSideChange: ((Mod.Side) -> Unit)? = null
) {
    val (clientEnabled, serverEnabled) = sideToFlags(currentSide)
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
            val hasChineseName = !nameCn.isNullOrBlank()
            val primaryText = if (hasChineseName) nameCn!!.trim() else name.trim()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = primaryText.ifBlank { name.trim() },
                    color = MaterialColor.GRAY_900.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(8.wM)
                ToggleButton(
                    icon = "\uF108",
                    checked = clientEnabled,
                    onClick = onSideChange?.let {
                        {
                            it(flagsToSide(clientEnabled = !clientEnabled, serverEnabled = serverEnabled))
                        }
                    }
                )
                Spacer(6.wM)
                ToggleButton(
                    icon = "\uF233",
                    checked = serverEnabled,
                    onClick = onSideChange?.let {
                        {
                            it(flagsToSide(clientEnabled = clientEnabled, serverEnabled = !serverEnabled))
                        }
                    }
                )
            }

            if (hasChineseName) {
                val secondaryTrimmed = name.trim()
                if (secondaryTrimmed.isNotEmpty()) {
                    Text(
                        text = secondaryTrimmed,
                        color = MaterialColor.BLUE_600.color,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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

private fun sideToFlags(side: Mod.Side): Pair<Boolean, Boolean> = when (side) {
    Mod.Side.CLIENT -> true to false
    Mod.Side.SERVER -> false to true
    Mod.Side.BOTH -> true to true
    Mod.Side.UNKNOWN -> false to false
}

private fun flagsToSide(clientEnabled: Boolean, serverEnabled: Boolean): Mod.Side = when {
    clientEnabled && serverEnabled -> Mod.Side.BOTH
    clientEnabled -> Mod.Side.CLIENT
    serverEnabled -> Mod.Side.SERVER
    else -> Mod.Side.UNKNOWN
}
