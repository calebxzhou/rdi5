package calebxzhou.rdi.jade

import calebxzhou.rdi.service.EnglishStorage
import calebxzhou.rdi.util.mcComp
import calebxzhou.rdi.util.rdiAsset
import net.minecraft.ChatFormatting
import snownee.jade.api.EntityAccessor
import snownee.jade.api.IEntityComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.TooltipPosition
import snownee.jade.api.config.IPluginConfig

enum class EnglishEntityDisplayProvider : IEntityComponentProvider {
    INSTANCE;

    override fun getUid() = rdiAsset("jade/english_entity_display")

    override fun appendTooltip(tooltip: ITooltip, accessor: EntityAccessor, config: IPluginConfig) {
        appendTooltip(tooltip,accessor.entity.type.descriptionId)
    }
    private  fun appendTooltip(tooltip: ITooltip,langKey:String ){
        tooltip.add( EnglishStorage[langKey].mcComp.withStyle(ChatFormatting.ITALIC))
    }
    override fun getDefaultPriority(): Int {
        return TooltipPosition.HEAD + 1
    }
}