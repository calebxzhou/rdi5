package calebxzhou.rdi.ui.layout

import calebxzhou.rdi.ui.UiHeight
import calebxzhou.rdi.ui.UiWidth
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.component.button.RButton
import calebxzhou.rdi.ui.component.button.RIconButton
import calebxzhou.rdi.ui.component.button.RItemButton
import calebxzhou.rdi.ui.component.button.RPlayerHeadButton
import calebxzhou.rdi.ui.general.HAlign
import calebxzhou.rdi.util.mcComp
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.gui.layouts.LayoutElement
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.Item
import org.bson.types.ObjectId


fun gridLayout(
    screen: RScreen,
    x: Int = UiWidth / 2,
    y: Int = UiHeight - 20,
    hAlign: HAlign = HAlign.CENTER,
    maxColumns: Int = 0,
    rowSpacing: Int = 0,
    colSpacing: Int = 0,
    builder: GridLayoutBuilder.() -> Unit,
): GridLayout {
    return GridLayoutBuilder(screen, maxColumns, x, y, hAlign, rowSpacing, colSpacing).apply(builder).build()
}

class GridLayoutBuilder(
    val screen: RScreen,
    val maxColumns: Int,
    val x: Int,
    val y: Int,
    val hAlign: HAlign,
    val rowSpacing: Int = 0,
    val colSpacing: Int = 0,
) {
    val children = arrayListOf<LayoutElement>()

    fun LayoutElement.add(){
        children += this
    }
    fun widget(widget: LayoutElement) {
        children += widget
    }

    fun button(text: String, onClick: (Button) -> Unit) {
        button(text.mcComp, onClick)
    }

    fun button(text: MutableComponent, onClick: (Button) -> Unit) {
        children += RButton(text.string, onClick = onClick)
    }

    fun head(
        id: ObjectId,
        onClick: ( Button) -> Unit
    ) {
        widget(RPlayerHeadButton(id,    onClick))
    }
    fun button(
        text: String="",
        comp: MutableComponent = text.mcComp
    ){

    }
    fun button(
        icon: String ,
        text: String = "",
        comp: MutableComponent = text.mcComp,
        hoverText: String = "",
        active: Boolean = true,
        x: Int = 0,
        y: Int = 0,
        size: Int = 64,
        scale: Double = 1.0,
         onClick: (Button) -> Unit = {}
    ) = RIconButton(icon, text,  onClick).apply {
        this.active = active
         }.also { children += it }

    fun button(
        item: Item,
        size: Int=16,
        tooltip: String = "",
        onClick: (Button) -> Unit
    ){
        widget(RItemButton(item,  onClick))
    }
    fun build(): GridLayout {
        val layout = GridLayout(x, y)

        val rowHelper = layout.createRowHelper(if (maxColumns > 0) maxColumns else children.size)
        layout.rowSpacing(rowSpacing)
        layout.columnSpacing(colSpacing)
        children.forEach { rowHelper.addChild(it) }
        layout.arrangeElements()
        when (hAlign) {
            HAlign.LEFT -> {}
            HAlign.RIGHT -> layout.x += layout.width / 2
            HAlign.CENTER -> layout.x -= layout.width / 2
        }

        layout.visitWidgets { screen.addWidget(it) }
        return layout
    }

}
