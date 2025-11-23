package calebxzhou.rdi.mixin;

import calebxzhou.rdi.ui2.frag.TitleFragment;
import icyllis.modernui.mc.neoforge.MuiForgeApi;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2025-04-15 10:32
 */
@Mixin(TitleScreen.class)
public class mTitleScreen extends Screen {
    protected mTitleScreen(Component title) {
        super(title);
    }

  /*  @Inject(method = "init",at=@At("HEAD"), cancellable = true)
    private void RDI$TitleScreen(CallbackInfo ci){
        //Minecraft.getInstance().setScreen(new RTitleScreen());
        MuiForgeApi.openScreen(new TitleFragment());
        ci.cancel();
    }*/
    @Overwrite
    public void init() {
        this.addRenderableWidget(
                Button.builder(Component.literal("启动RDI核心"), p_344156_ -> MuiForgeApi.openScreen(new TitleFragment()))
                        .bounds(0, 0, 200, 200)
                        .build()
        );
    }
}
