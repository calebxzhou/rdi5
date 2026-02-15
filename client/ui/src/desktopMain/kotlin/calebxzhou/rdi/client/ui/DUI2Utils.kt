package calebxzhou.rdi.client.ui

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.JOptionPane

/**
 * Desktop-only UI utilities (JVM/AWT/Swing)
 */


fun alertErrOs(msg: String) {
    JOptionPane.showMessageDialog(
        null,
        msg,
        "错误",
        JOptionPane.ERROR_MESSAGE
    )
}

fun clipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}
