package calebxzhou.rdi.util
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.*

/**
 * calebxzhou @ 2025-04-14 23:08
 */
val String.mcComp: MutableComponent
    get() = Component.literal(this)
val String.mcTooltip
    get() = Tooltip.create(this.mcComp)
val EmptyComp
    get() = Component.empty()

operator fun MutableComponent.plus(component: Component): MutableComponent {
    return append(component)
}

operator fun MutableComponent.plus(component: String): MutableComponent {
    return append(component)
}
fun MutableComponent.hoverText(str:String): MutableComponent {
    withStyle(
        Style.EMPTY
            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, str.mcComp))
    )
    return this
}
fun MutableComponent.clickCommand(cmd: String) : MutableComponent{
    hoverText("点击运行指令")
    withStyle(Style.EMPTY.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd)))
    return this
}
fun MutableComponent.clickCopy(text: String) : MutableComponent{
    hoverText("点击复制")
    withStyle(Style.EMPTY.withClickEvent(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text)))
    return this
}
fun MutableComponent.clickBrowse(url: String) : MutableComponent{
    hoverText("点击打开链接")
    withStyle(Style.EMPTY.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, url)))
    return this
}