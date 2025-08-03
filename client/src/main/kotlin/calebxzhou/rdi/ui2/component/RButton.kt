package calebxzhou.rdi.ui2.component

import icyllis.modernui.core.Context
import icyllis.modernui.view.ContextMenu
import icyllis.modernui.view.Menu
import icyllis.modernui.view.MotionEvent
import icyllis.modernui.widget.Button

open class RButton(
    context: Context,
    val onClick: () -> Unit = {},
): Button(context) {
    private var menuItems = listOf<Pair<String, () -> Unit>>()
    private var   mContextMenuAnchorX = 0f;
    private var mContextMenuAnchorY = 0f
    var contextMenu: (List<Pair<String, () -> Unit>>) -> Unit = { items ->
        menuItems = items
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        when (event.action) {
            event.action if event.isButtonPressed(MotionEvent.BUTTON_SECONDARY) -> {
                //显示右键菜单
                showContextMenu(event.x,event.y)
                return true
            }
            event.action if event.isButtonPressed(MotionEvent.BUTTON_PRIMARY) -> {
                //onClick()
                showContextMenu(event.x,event.y)
                return true
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun onCreateContextMenu(menu: ContextMenu) {
        menu.setHeaderView(this)
        menu.setHeaderTitle("test")
        menu.add("testtest")
        menu.add(Menu.NONE, 123543687, 0, "1231320").setEnabled(true)
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