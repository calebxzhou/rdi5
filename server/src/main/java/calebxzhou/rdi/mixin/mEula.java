package calebxzhou.rdi.mixin;

import net.minecraft.server.Eula;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2024-05-26 10:14
 */
@Mixin(Eula.class)
public class mEula {
    @Overwrite
    public boolean hasAgreedToEULA() {
        return true;
    }
}
