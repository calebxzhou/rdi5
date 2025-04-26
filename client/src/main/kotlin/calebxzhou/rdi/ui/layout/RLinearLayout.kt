package calebxzhou.rdi.ui.layout

import calebxzhou.rdi.ui.UiHeight
import calebxzhou.rdi.ui.UiWidth
import calebxzhou.rdi.ui.component.RScreen

// DSL Function: linearLayout
fun linearLayout(
    screen: RScreen,
    init: RLinearLayout.() -> Unit
): RLinearLayout {
    return RLinearLayout(screen).apply(init).build()
}

class RLinearLayout(
    override val screen: RScreen,
    override var startX: Int = UiWidth / 2,
    override var startY: Int = UiHeight - 20
) : RLayout(screen, startX, startY) {

    // 居中对齐
    var center: Boolean = true

    // 水平/垂直布局
    var horizontal: Boolean = true

    // 水平间隔
    var marginX: Int = 5

    // 垂直间隔
    var marginY: Int = 5

    //自动换行
    var autoWrap = false
    // 每行最大数量（0 表示不限制）
    val maxRowWidgetCount: Int = 0

    // 每列最大数量（0 表示不限制）
    val maxColWidgetCount: Int = 0

    // 总宽度和高度
    var totalWidth: Int = 0
        private set
    var totalHeight: Int = 0
        private set

    fun build(): RLinearLayout {
        updatePosition()
        children.forEach { (id, ele) ->
            screen.addWidget(ele, id)
        }

        return this
    }
    fun updatePosition(){
        if(children.isEmpty())
            return
        if (horizontal) {
            calculateHorizontalLayout()
        } else {
            calculateVerticalLayout()
        }

        // 确保 totalWidth 和 totalHeight 在没有换行/换列时也被正确计算
        if (horizontal) {
            totalHeight = children.maxOf { (_, w) -> w.height } // 单行时，最高的元素高度=总高度
        } else {
            totalWidth = children.maxOf { (_, w) -> w.width } // 单列时，最宽元素宽度=总宽度
        }

        // 中心对齐调整
        children.forEach { (id, ele) ->
            if (center) {
                ele.x -= totalWidth / 2
            }
        }
    }
    fun removeWidget(id: String){
        screen.removeWidget(id)
        children.remove(id)
        updatePosition()
    }
    private fun calculateHorizontalLayout() {
        var widgetX = startX
        var widgetY = startY
        val entryList = children.entries.toList()
        var currentRowWidth = 0
        entryList.forEachIndexed { i, (_, ele) ->
            val reachMaxRowCount = (maxRowWidgetCount > 0 && i % maxRowWidgetCount == 0 && i != 0)
            val reachMaxWidth =  widgetX + ele.width > if(center) UiWidth*2 else UiWidth

            // 换行条件
            if (autoWrap && (reachMaxRowCount || reachMaxWidth) ) {
                widgetX = startX
                widgetY += entryList[i - 1].value.height + marginY
                currentRowWidth = 0
            }
            ele.x = widgetX
            ele.y = widgetY
            // 更新位置和尺寸
            widgetX += ele.width + marginX
            currentRowWidth += widgetX
            totalWidth = maxOf(totalWidth, widgetX - marginX - startX)
            totalHeight = maxOf(totalHeight, widgetY + ele.height - startY)
        }
    }

    private fun calculateVerticalLayout() {
        var widgetX = startX
        var widgetY = startY
        val entryList = children.entries.toList()

        entryList.forEachIndexed { i, (_, ele) ->

            // 换列条件
            if (autoWrap && (widgetY + ele.height > UiHeight || (maxColWidgetCount > 0 && i % maxColWidgetCount == 0 && i != 0))) {
                widgetY = startY
                widgetX += entryList[i - 1].value.width + marginX
            }
            ele.x = widgetX
            ele.y = widgetY

            // 更新位置和尺寸
            widgetY += ele.height + marginY
            totalWidth = maxOf(totalWidth, widgetX + ele.width - startX)
            totalHeight = maxOf(totalHeight, widgetY - marginY - startY)
        }
    }
}