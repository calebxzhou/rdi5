package calebxzhou.rdi.mc.server;

import calebxzhou.rdi.mc.server.mixin.AGameRuleIntegerValue;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;

import javax.annotation.Nullable;

/**
 * calebxzhou @ 2026-02-05 11:56
 */
public abstract class RGameRuleIntegerValue extends GameRules.IntegerValue implements AGameRuleIntegerValue {
    public RGameRuleIntegerValue(GameRules.RuleType<GameRules.IntegerValue> pType, int pValue) {
        super(pType, pValue);
    }

    public void set(int value, @Nullable MinecraftServer pServer) {
      //  this.setValue(value);
        onChanged(pServer);
    }
}
