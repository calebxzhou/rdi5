package calebxzhou.rdi.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerStatusPacketListenerImpl.class)
public interface ASStatusPacketListener {
    @Accessor
    Connection getConnection();

}
