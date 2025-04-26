package calebxzhou.rdi.mixin;

import net.minecraft.network.PacketDecoder;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * calebxzhou @ 2025-04-23 13:23
 */
@Mixin(PacketDecoder.class)
public interface APacketDecoder<T extends PacketListener> {
    @Accessor
    ProtocolInfo<T> getProtocolInfo();
}
