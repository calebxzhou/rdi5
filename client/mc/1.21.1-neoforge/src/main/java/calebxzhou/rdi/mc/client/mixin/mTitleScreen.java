package calebxzhou.rdi.mc.client.mixin;

import calebxzhou.rdi.mc.common.RDI;
import com.google.common.net.HostAndPort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static calebxzhou.rdi.mc.common.RDI.GAME_IP;

/**
 * calebxzhou @ 2025-04-15 10:32
 */
@Mixin(TitleScreen.class)
public class mTitleScreen extends Screen {
    protected mTitleScreen(Component title) {
        super(title);
    }

    @Overwrite
    @Override
    public void renderPanorama(GuiGraphics guiGraphics, float partialTick) {
        guiGraphics.blit(ResourceLocation.tryBuild("rdi","textures/bg/1.jpg"),0,0,0,0,this.width,this.height,this.width,this.height);
    }
    @Redirect(method = "createNormalMenuOptions",at= @At(value = "INVOKE",ordinal = 1, target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent RDI$AddMultiplayerButton(String key){
        return Component.literal("进入主机："+ RDI.HOST_NAME);
    }
    @Inject(method = "lambda$createNormalMenuOptions$8",at=@At("HEAD"), cancellable = true)
    private void RDI$onClickMultiplayer(Button p_280833_, CallbackInfo ci){
        HostAndPort hp = HostAndPort.fromString(GAME_IP);
        ConnectScreen.startConnecting(
                new TitleScreen(),
                this.getMinecraft(),
                new ServerAddress(hp.getHost(), hp.getPort()),
                new ServerData("rdi", GAME_IP, ServerData.Type.OTHER),
                false,
                null
        );
        ci.cancel();
    }
}
