package calebxzhou.rdi.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * calebxzhou @ 2025-08-13 22:37
 */
@Mixin(Screen.class)
public class mTooltipGuard {
    @Shadow @Final private static Logger LOGGER;

    @Redirect(method = "getTooltipFromItem",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getTooltipLines(Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;"))
    private static List<Component> RDI$NoCrashTooltip(ItemStack item, Item.TooltipContext i, Player list, TooltipFlag mutablecomponent){
        var minecraft = Minecraft.getInstance();
        try {
            return item.getTooltipLines(
                    Item.TooltipContext.of(minecraft.level),
                    minecraft.player,
                    net.neoforged.neoforge.client.ClientTooltipFlag.of(minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)
            );
        } catch (Exception e) {
            LOGGER.error(" ",e);
            return List.of(Component.literal("无法获取tooltip，检查日志.."));
        }
    }
}
