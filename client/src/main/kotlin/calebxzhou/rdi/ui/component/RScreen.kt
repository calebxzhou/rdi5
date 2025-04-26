package calebxzhou.rdi.ui.component

import calebxzhou.rdi.ui.CenterX
import calebxzhou.rdi.ui.component.button.RIconButton
import calebxzhou.rdi.ui.drawText
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mcComp
import calebxzhou.rdi.util.pressingEnter
import calebxzhou.rdi.util.pressingKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.MutableComponent
import org.bson.types.ObjectId
import java.util.concurrent.ConcurrentHashMap

abstract class RScreen(open var title: MutableComponent) : Screen(title) {
    protected val background: CoroutineScope = CoroutineScope(Dispatchers.IO)
    constructor(name: String) : this(name.mcComp)
    open var showTitle = true
    open var closeable = true
    open var showCloseButton = closeable
    //标题位置 等于0=在中间
    open var titleX = 0
    var pressingEnterKeyOnInit = false
    var tickCounter = 0
    val widgets = linkedMapOf<String, AbstractWidget>()

    private var buttonHoldTime = ConcurrentHashMap<Int, HoldButton>()
    data class HoldButton(val btn: Int, var ticks: Int,val maxTicks: Int, val onFinish: () -> Unit)
    fun holdButton(btn: Int, ticks: Int,onFinish: ()->Unit){
        buttonHoldTime += btn to HoldButton(btn,0,ticks,onFinish)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {

        if (showTitle) {
            //titleX为负数则居中
            if(titleX<=0)
                guiGraphics.drawText(comp=title,x= CenterX(title.string), y=4)
            else
                guiGraphics.drawText(comp=title,x=titleX,y=4)
        }
        doRender(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    open fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {}

    override fun tick() {
        if (mc.pressingEnter && !pressingEnterKeyOnInit){
            onPressEnterKey()
        }
        if(!mc.pressingEnter && tickCounter%10==0){
            pressingEnterKeyOnInit=false
        }

        buttonHoldTime.forEach { key,btn ->
            if(mc pressingKey key)
                buttonHoldTime[key]?.ticks++
            buttonHoldTime[key]?.let {
                if(it.ticks>=it.maxTicks){
                    it.onFinish()
                    buttonHoldTime.remove(key)
                }
            }

        }
        doTick()
        tickCounter++
    }
    open fun doTick() {

    }
    open fun doInit(){}
    override fun init() {
        super.init()
        doInit()
        if (mc.pressingEnter) {
            pressingEnterKeyOnInit=true
        }
        if (closeable&&showCloseButton) {
            val backBtn = RIconButton(icon = "back") { onClose() }
            widgets += "back" to backBtn
            addRenderableWidget(backBtn)
        }

    }

    fun <T : AbstractWidget> ofWidget(id: String): T {
        return widgets[id] as T
    }

    fun addWidget(widget: AbstractWidget, id: String = ObjectId().toHexString()) {
        if(widgets.contains(id))
            removeWidget(id)
        //如果是按钮，就把图片名称作id
        widgets += if (widget is RIconButton) {
            widget.icon to widget
        } else {
            id to widget
        }
        addRenderableWidget(widget)
    }
    fun removeWidget(id: String){

        if(widgets.contains(id)){
            val removed = widgets.remove(id)
            removeWidget(removed)
        }
    }

    operator fun plusAssign(widget: AbstractWidget) {
        addRenderableWidget(widget)
    }

    operator fun minusAssign(widget: AbstractWidget) {
        removeWidget(widget)
    }

    override fun clearWidgets() {
        widgets.clear()
        super.clearWidgets()
    }

    override fun onClose() {
        if(closeable){
            mc.popGuiLayer()
        }
    }
    override fun shouldCloseOnEsc(): Boolean {
        return closeable
    }
    open fun onPressEnterKey(){

    }
    fun onRightClick(mouseX: Double, mouseY: Double) {
        widgets.forEach { (id,widget)->
            if(widget is RightClickable && widget.isMouseOver(mouseX,mouseY)){
                focused = widget
                widget.onRightClick(mouseX,mouseY)
            }
        }
    }

}