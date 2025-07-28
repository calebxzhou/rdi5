package calebxzhou.rdi.ui2

import calebxzhou.rdi.ui2.component.*
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.FrameLayout
import icyllis.modernui.widget.GridLayout
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView
import org.bson.types.ObjectId

/**
 * calebxzhou @ 2025-07-22 21:28
 */
val PARENT = LinearLayout.LayoutParams.MATCH_PARENT
val SELF = LinearLayout.LayoutParams.WRAP_CONTENT
fun linearLayoutParam(
    width: Int = PARENT,
    height: Int = SELF,
    init: LinearLayout.LayoutParams.() -> Unit = {}
): LinearLayout.LayoutParams {

    return LinearLayout.LayoutParams(width, height).apply(init)
}

fun frameLayoutParam(
    width: Int = PARENT,
    height: Int = SELF,
    init: FrameLayout.LayoutParams.() -> Unit = {}
): FrameLayout.LayoutParams {
    return FrameLayout.LayoutParams(width, height).apply(init)
}

fun ViewGroup.linearLayout(
    init: LinearLayout.() -> Unit = {}
) = LinearLayout(this.context).apply(init).also { this += it }

fun ViewGroup.frameLayout(
    init: FrameLayout.() -> Unit = {}
) = FrameLayout(this.context).apply(init).also { this += it }

fun ViewGroup.gridLayout(
    init: GridLayout.() -> Unit = {}
) = GridLayout(this.context).apply(init).also { this += it }

fun ViewGroup.textView(
    init: TextView.() -> Unit = {}
) = TextView(this.context).apply(init).also { this += it }

fun ViewGroup.textButton(
    msg: String,
    init: RTextButton.() -> Unit = {},
    onClick: () -> Unit = {},
) = RTextButton(this.context, msg, onClick).apply(init).also { this += it }

fun ViewGroup.editText(
    msg: String = "",
    width: Float = 200f,
    init: REditText.() -> Unit = {}
) = REditText(this.context, msg, width).apply(init).also { this += it }
fun ViewGroup.editPwd(
    msg: String = "",
    width: Float = 200f,
    init: REditPassword.() -> Unit = {}
) = REditPassword(this.context, msg, width).apply(init).also { this += it }

fun ViewGroup.headButton(
    id: ObjectId,
    init: RPlayerHeadButton.() -> Unit = {},
    onClick: () -> Unit = {},
) = RPlayerHeadButton(context, id, onClick).apply(init).also { this += it }


fun ViewGroup.iconButton(
    icon: String,
    text: String,
    init: RIconButton.() -> Unit = {},
    onClick: () -> Unit = {},
) = RIconButton(this.context, icon, text, onClick).apply(init).also { this += it }
