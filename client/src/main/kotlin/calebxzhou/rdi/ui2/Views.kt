package calebxzhou.rdi.ui2

import calebxzhou.rdi.ui2.component.REditText
import calebxzhou.rdi.ui2.component.RTextButton
import icyllis.modernui.core.Context
import icyllis.modernui.widget.Button
import icyllis.modernui.widget.FrameLayout
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView

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

fun linearLayout(
    context: Context,
    init: LinearLayout.() -> Unit = {}
): LinearLayout {
    val layout = LinearLayout(context)
    layout.apply(init)
    return layout
}
fun frameLayout(
    context: Context,
    init: FrameLayout.() -> Unit = {}
): FrameLayout {
    val layout = FrameLayout(context)
    layout.apply(init)
    return layout
}

fun textView(
    context: Context,
    init: TextView.() -> Unit = {}
): TextView {
    val textView = TextView(context)
    textView.apply(init)
    return textView
}
fun button(
    context: Context,
    init: Button.() -> Unit = {}
): Button {
    val button = Button(context)
    button.apply(init)
    return button
}
fun textButton(
    context: Context,
    msg: String,
    onClick: () -> Unit = {},
    init: RTextButton.() -> Unit = {}
): TextView {
    val button = RTextButton(context,msg,onClick)
    button.apply(init)
    return button
}
fun editText(
    context: Context,
    msg: String = "",
    width: Float = 200f,
    init: REditText.() -> Unit = {}
): REditText {
    val editText = REditText(context, msg, width)
    editText.apply(init)
    return editText
}
