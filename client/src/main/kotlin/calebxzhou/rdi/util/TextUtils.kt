package calebxzhou.rdi.util
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

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
