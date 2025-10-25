package calebxzhou.rdi.mixin;

import io.netty.channel.ChannelFuture;
import net.minecraft.client.gui.screens.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ConnectScreen.class)
public interface AConnectScreen {
    @Accessor
    ChannelFuture getChannelFuture();
}
