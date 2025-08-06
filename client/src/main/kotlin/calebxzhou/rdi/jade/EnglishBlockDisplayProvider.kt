package calebxzhou.rdi.jade

import calebxzhou.rdi.service.EnglishStorage
import calebxzhou.rdi.util.mcComp
import calebxzhou.rdi.util.rdiAsset
import net.minecraft.ChatFormatting
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.TooltipPosition
import snownee.jade.api.config.IPluginConfig

enum class EnglishBlockDisplayProvider : IBlockComponentProvider {
    INSTANCE;

    override fun getUid() = rdiAsset("jade/english_block_display")

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        appendTooltip(tooltip,accessor.block.descriptionId)
    }

    private  fun appendTooltip(tooltip: ITooltip,langKey:String ){
        tooltip.add( EnglishStorage[langKey].mcComp.withStyle(ChatFormatting.ITALIC))
    }
    override fun getDefaultPriority(): Int {
        return TooltipPosition.HEAD + 1
    }
}