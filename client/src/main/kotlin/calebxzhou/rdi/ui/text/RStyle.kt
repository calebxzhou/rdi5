package calebxzhou.rdi.ui.text

import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

class RStyle(
    val itemStack: ItemStack?,
    val icon: String?,
) : Style(
    null as TextColor?,
    null as Boolean?,
    null as Boolean?,
    null as Boolean?,
    null as Boolean?,
    null as Boolean?,
    null as ClickEvent?,
    null as HoverEvent?,
    null as String?,
    null as ResourceLocation?) {
}