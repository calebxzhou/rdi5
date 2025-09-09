package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.ui2.HoldToConfirm.clearHoldToConfirm
import calebxzhou.rdi.ui2.HoldToConfirm.holdTooltipEnabled
import calebxzhou.rdi.ui2.HoldToConfirm.holdTooltipFormatter
import calebxzhou.rdi.ui2.HoldToConfirm.onLongPress
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.paddingDp
import icyllis.modernui.R
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.view.ContextMenu
import icyllis.modernui.view.Menu
import icyllis.modernui.view.MotionEvent
import icyllis.modernui.widget.Button
 

open class RButton(
    context: Context,
    val color: MaterialColor = MaterialColor.WHITE,
    val onClick: (RButton) -> Unit = {},
): Button(context,null, R.attr.buttonStyle,R.style.Widget_Material3_Button_IconButton) {


    private var menuItems = listOf<Pair<String, () -> Unit>>()
    private var mContextMenuAnchorX = 0f;
    private var mContextMenuAnchorY = 0f
    var contextMenu: (List<Pair<String, () -> Unit>>) -> Unit = { items ->
        menuItems = items
    }

    init {
        // Ensure normal click works when hold-to-confirm is not enabled
        setOnClickListener { onClick(this) }
        paddingDp(16, 8, 16, 8)
        background = object : Drawable() {
            override fun draw(canvas: Canvas) {
                val paint = Paint.obtain()
                paint.color = color.colorValue
                // Keep consistent with existing usage of Paint style
                paint.style = Paint.Style.FILL.ordinal
                // Rounded rect background
                canvas.drawRoundRect(
                    bounds.left.toFloat(),
                    bounds.top.toFloat(),
                    bounds.right.toFloat(),
                    bounds.bottom.toFloat(),
                    48f,
                    paint
                )
                paint.recycle()
            }
        }

        // Ensure text has sufficient contrast
        val textColor = if (color.isDarkColor) MaterialColor.WHITE.colorValue else MaterialColor.BLACK.colorValue
        setTextColor(textColor)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        // Preserve right-click context menu behavior
        if (event.isButtonPressed(MotionEvent.BUTTON_SECONDARY)) {
            showContextMenu(event.x, event.y)
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }





    override fun onCreateContextMenu(menu: ContextMenu) {
        menu.setHeaderView(this)
        menu.setHeaderTitle("test")
        menu.add("testtest")
        menu.add(Menu.NONE, 123543687, 0, "1231320").isEnabled = true
            /*.setOnMenuItemClickListener(mOnContextMenuItemClickListener)
            .setEnabled(mTextView.canUndo())*/
        menu.setQwertyMode(true)
        super.onCreateContextMenu(menu)
    }
    override fun showContextMenu(x: Float, y: Float): Boolean {
        mContextMenuAnchorX = x;
        mContextMenuAnchorY = y;
        return super.showContextMenu(x, y)
    }
}