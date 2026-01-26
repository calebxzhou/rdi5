package calebxzhou.rdi.mc.client.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static calebxzhou.rdi.mc.client.RDIMain.JOIN_BUTTON;

/**
 * calebxzhou @ 2026-01-26 20:41
 */
@Mixin(JoinMultiplayerScreen.class)
public class mMultiScreen extends Screen {
    protected mMultiScreen(Component title) {
        super(title);
    }

    @Inject(method = "init",at=@At("TAIL"))
    private void RDI$JoinButton(CallbackInfo ci){
        this.addRenderableWidget(JOIN_BUTTON);
    }
}
