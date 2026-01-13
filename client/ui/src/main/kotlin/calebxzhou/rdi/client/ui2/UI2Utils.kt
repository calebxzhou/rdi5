package calebxzhou.rdi.client.ui2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import javax.swing.JOptionPane

/**
 * calebxzhou @ 2026-01-12 21:11
 */
@Composable
fun alertOk(msg: String){
    val title = "成功"
    val icon = "\uF058"
    createAlertDialog(title = title, icon = icon, msg = msg, accentColor = MaterialColor.GREEN_900.color)
}
@Composable
fun alertWarn(msg: String){
    val title = "警告"
    val icon = "\uEA6C"
    createAlertDialog(title = title, icon = icon, msg = msg, accentColor = Color(0xFFE0A800))
}
@Composable
fun alertErr(msg: String){
    val title = "错误"
    val icon = "\uEA87"
    createAlertDialog(title = title, icon = icon, msg = msg, accentColor = Color(0xFFD64545))
}
fun alertErrOs(msg: String) {
    JOptionPane.showMessageDialog(
        null,
        msg,
        "错误",
        JOptionPane.ERROR_MESSAGE
    )
}
@Composable
private fun createAlertDialog(
    title: String,
    icon: String,
    msg: String,
    accentColor: Color
){
    var visible by remember { mutableStateOf(true) }
    if (!visible) return

    AlertDialog(
        onDismissRequest = { visible = false },
        title = {
            val titleStyle = MaterialTheme.typography.subtitle1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    color = Color.White,
                    style = titleStyle,
                    fontFamily = IconFontFamily,
                    modifier = Modifier
                        .background(accentColor, CircleShape)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                        .alignByBaseline()
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alignByBaseline()
                )
            }
        },
        text = {
            Text(
                text = msg,
                textAlign = TextAlign.Left,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { visible = false }) {
                Text("明白", color = accentColor)
            }
        },
        backgroundColor = Color(0xFFF1ECF8),
        shape = RoundedCornerShape(28.dp)
    )
}
//pua codepoint识别并应用图标字体
val String.asIconText
    get() = buildAnnotatedString {
        val str = this@asIconText
        var i = 0
        while (i < str.length) {
            val codePoint = str.codePointAt(i)
            // BMP PUA: E000-F8FF
            // Plane 15 PUA: F0000-FFFFD
            // Plane 16 PUA: 100000-10FFFD
            // Nerd Font icons often in E000-F8FF (BMP PUA) and F0000-FFFFD (Supplementary PUA A)
            val isPua = (codePoint in 0xE000..0xF8FF) ||
                    (codePoint in 0xF0000..0xFFFFD) ||
                    (codePoint in 0x100000..0x10FFFD)

            val charCount = Character.charCount(codePoint)
            if (isPua) {
                withStyle(style = SpanStyle(fontFamily = IconFontFamily)) {
                    append(str.substring(i, i + charCount))
                }
            } else {
                append(str.substring(i, i + charCount))
            }
            i += charCount
        }
    }
