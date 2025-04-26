package calebxzhou.rdi.mixin;

import calebxzhou.rdi.service.LevelService;
import com.aayushatharva.brotli4j.decoder.BrotliInputStream;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * calebxzhou @ 2025-04-14 22:39
 */
public class mCompressFormat {
}
@Mixin(NbtIo.class)
class mCompressNbtFormat{
    //nbt数据用br压缩
    //原版nbt结构数据读取不了 不能用
    /*@Redirect(method = "createDecompressorStream",at= @At(value = "NEW", target = "(Ljava/io/InputStream;)Lnet/minecraft/util/FastBufferedInputStream;"))
    private static FastBufferedInputStream RDI$Decompress(InputStream in) throws IOException {
        return new FastBufferedInputStream(new BrotliInputStream(in));
    }
    @Redirect(method = "createCompressorStream",at= @At(value = "NEW", target = "(Ljava/io/OutputStream;)Ljava/io/BufferedOutputStream;"))
    private static BufferedOutputStream RDI$compress(OutputStream in) throws IOException {
        return new BufferedOutputStream(new BrotliOutputStream(in));
    }*/
}
@Mixin(RegionFileVersion.class)
abstract
class mMapCompressFormat{

    //地图数据br压缩
    //先不用
    /*@Final @Mutable @Shadow
    public static final RegionFileVersion DEFAULT = LevelService.BR_STORAGE_VERSION;

    @Shadow
    private static RegionFileVersion register(RegionFileVersion fileVersion) {
        return null;
    }

    @Overwrite
    public static RegionFileVersion getSelected() {
        return LevelService.BR_STORAGE_VERSION;
    }*/
}