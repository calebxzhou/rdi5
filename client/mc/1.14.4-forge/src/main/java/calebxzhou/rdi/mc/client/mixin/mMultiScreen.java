package calebxzhou.rdi.mc.client.mixin;

import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static calebxzhou.rdi.mc.client.RDIMain.JOIN_BUTTON;

/**
 * calebxzhou @ 2026-01-26 20:41
 */
@Mixin(MultiplayerScreen.class)
public class mMultiScreen extends Screen {
    protected mMultiScreen(ITextComponent title) {
        super(title);
    }

    @Inject(method = "init",at=@At("TAIL"))
    private void RDI$JoinButton(CallbackInfo ci){
        this.addButton(JOIN_BUTTON);
    }
}
