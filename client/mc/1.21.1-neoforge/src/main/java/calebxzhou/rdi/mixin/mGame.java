package calebxzhou.rdi.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2026-01-01 13:13
 */
@Mixin(Minecraft.class)
public class mGame {
    @Overwrite
    public boolean allowsTelemetry() {
        return false;
    }

    @Overwrite
    public boolean allowsMultiplayer() {
        return true;
    }
}
