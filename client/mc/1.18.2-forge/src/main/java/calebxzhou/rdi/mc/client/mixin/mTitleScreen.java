package calebxzhou.rdi.mc.client.mixin;

import calebxzhou.rdi.mc.client.RDIMain;
import com.google.common.net.HostAndPort;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static calebxzhou.rdi.mc.common.RDI.*;

/**
 * calebxzhou @ 2025-04-15 10:32
 */
@Mixin(TitleScreen.class)
public class mTitleScreen extends Screen {
    @Shadow
    @Mutable @Final
    private static final ResourceLocation PANORAMA_OVERLAY = RDIMain.BG_RES;
    protected mTitleScreen(Component title) {
        super(title);
    }

    @Redirect(method = "render",at= @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"))
    public void RDI$NOrenderPanorama(PanoramaRenderer instance, float deltaT, float alpha) {

    }

    @Inject(method = "createNormalMenuOptions", at = @At("HEAD"))
    private void RDI$AddMultiplayerButton(int y, int rowHeight, CallbackInfo ci) {
         addRenderableWidget(new Button(100,0,200,20,new TextComponent("进入地图：" + HOST_NAME),(btn)->{
            HostAndPort hp = HostAndPort.fromString(GAME_IP);
            ConnectScreen.startConnecting(
                    new TitleScreen(),
                    this.getMinecraft(),
                    new ServerAddress(hp.getHost(), hp.getPort()),
                    new ServerData("rdi", GAME_IP, false)
            );
        }));
    }

}
