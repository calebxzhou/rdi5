package calebxzhou.rdi.client.ui.comp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calebxzhou.rdi.client.ui.MaterialColor
import calebxzhou.rdi.client.ui.asIconText

@Composable
fun ToggleButton(
    icon: String,
    checked: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val bg = if (checked) MaterialTheme.colors.primary else MaterialColor.GRAY_300.color
    val fg = if (checked) Color.White else MaterialColor.GRAY_700.color
    val clickModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }
    Surface(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(999.dp))
            .then(clickModifier),
        color = bg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = icon.asIconText,
                color = fg,
                fontSize = 11.sp
            )
        }
    }
}
