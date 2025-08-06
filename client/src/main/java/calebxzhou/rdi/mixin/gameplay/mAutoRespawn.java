package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2024-05-23 12:11
 */
@Mixin(LocalPlayer.class)
public class mAutoRespawn {
    /**
     * @author calebxzhou
     * @reason 死亡直接复活
     */
    @Overwrite
    public boolean shouldShowDeathScreen() {
        return false;
    }
}