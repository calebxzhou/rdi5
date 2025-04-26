package calebxzhou.rdi.ui.layout

import calebxzhou.rdi.lgr
import calebxzhou.rdi.ui.UiHeight
import calebxzhou.rdi.ui.UiWidth
import calebxzhou.rdi.ui.component.RCheckbox
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.component.button.RIconButton
import calebxzhou.rdi.ui.component.button.RItemButton
import calebxzhou.rdi.ui.component.button.RPlayerHeadButton
import calebxzhou.rdi.ui.component.button.RTextButton
import calebxzhou.rdi.ui.component.editbox.REditBox
import calebxzhou.rdi.ui.component.editbox.RPasswordEditBox
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.Item
import org.bson.types.ObjectId

open class RLayout(
    open val screen: RScreen,
    open var startX: Int = UiWidth / 2,
    open val startY: Int = UiHeight - 20,
) {
    val children: LinkedHashMap<String, AbstractWidget> = linkedMapOf()

    fun AbstractWidget.add(id: String = ObjectId().toString()) {
        if(children.containsKey(id))
            lgr.warn("已经注册过ID相同的组件:${this}")
        //如果是按钮，就把图片名称作id
        children +=
            (if (this is RIconButton) this.icon else id) to this
    }

    fun icon(
        icon: String,
        text: String? = null,
        init: RIconButton.() -> Unit = {},
        click: (Button) -> Unit = {},
    ) {
        RIconButton(icon,text,click).apply(init).add()
    }
    fun item(
        item: Item,
        init: RItemButton.() -> Unit = {},
        click: (Button) -> Unit = {}
    ){
        RItemButton(item,click).apply(init).add()
    }
    fun text(
        comp: MutableComponent,
        init: RTextButton.() -> Unit = {},
        click: (Button) -> Unit = {}
    ){
        RTextButton(comp,click).apply { init();textComp=comp }.add()
    }
    fun head(
        uid: ObjectId,
        init: RPlayerHeadButton.() -> Unit = {},
        click: (Button) -> Unit = {}
    ){
        RPlayerHeadButton(uid ,click).apply(init).add(uid.toHexString())
    }
    fun textBox(
        id: String,
        label: String,
        length: Int,
        init: REditBox.()->Unit = {}
    ){
        REditBox(length,label).apply(init).add(id)
    }
    fun passwordBox(
        id: String ="pwd",
        label: String="密码",
        init: RPasswordEditBox.()->Unit = {}
    ){
        RPasswordEditBox(label).apply(init).add(id)
    }
    fun checkBox(
        id: String,
        label: String,
        selected: Boolean = false,
        init: RCheckbox.()->Unit = {}
    ): RCheckbox{
        return RCheckbox(label,selected).apply(init).also {  it.add(id)}
    }

}