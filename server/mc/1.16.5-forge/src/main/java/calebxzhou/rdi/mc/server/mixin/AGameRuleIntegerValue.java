package calebxzhou.rdi.mc.server.mixin;

import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * calebxzhou @ 2026-02-05 11:51
 */
@Mixin(GameRules.IntegerValue.class)
public interface AGameRuleIntegerValue {
    @Accessor void setValue(int value);
}
