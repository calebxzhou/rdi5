package calebxzhou.rdi.mixin;

import com.aayushatharva.brotli4j.decoder.BrotliInputStream;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.FastBufferedInputStream;
import org.spongepowered.asm.mixin.Mixin;
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
    @Redirect(method = "createDecompressorStream",at= @At(value = "NEW", target = "(Ljava/io/InputStream;)Lnet/minecraft/util/FastBufferedInputStream;"))
    private static FastBufferedInputStream RDI$Decompress(InputStream in) throws IOException {
        return new FastBufferedInputStream(new BrotliInputStream(in));
    }
    @Redirect(method = "createCompressorStream",at= @At(value = "NEW", target = "(Ljava/io/OutputStream;)Ljava/io/BufferedOutputStream;"))
    private static BufferedOutputStream RDI$compress(OutputStream in) throws IOException {
        return new BufferedOutputStream(new BrotliOutputStream(in));
    }
}
