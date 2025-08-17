package calebxzhou.rdi.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * calebxzhou @ 8/17/2025 3:38 PM
 */
@Mixin(NativeImage.class)
public class mJpgImage {
    @Redirect(method = "read(Lcom/mojang/blaze3d/platform/NativeImage$Format;Ljava/nio/ByteBuffer;)Lcom/mojang/blaze3d/platform/NativeImage;",
    at= @At(value = "INVOKE", target = "Lnet/minecraft/util/PngInfo;validateHeader(Ljava/nio/ByteBuffer;)V"))
    private static void RDI$AlsoValidateJpegHeader(ByteBuffer buffer){
        try {
            rdi$validateJpegHeader(buffer);
        } catch (IOException e) {
            // If JPEG validation fails, try PNG validation
            try {
                rdi$validatePngHeader(buffer);
            } catch (IOException pngException) {
                throw new RuntimeException("Invalid image format: neither PNG nor JPEG", e);
            }
        }
    }

    /**
     * Validates JPEG header similar to PNG validation
     */
    @Unique
    private static void rdi$validateJpegHeader(ByteBuffer buffer) throws IOException {
        ByteOrder byteorder = buffer.order();
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Check for JPEG SOI (Start of Image) marker: 0xFFD8
        if (buffer.getShort(0) != (short)0xFFD8) {
            throw new IOException("Bad JPEG Signature - missing SOI marker");
        }

        // Check for JPEG EOI (End of Image) marker at the end or JFIF/EXIF marker
        // JFIF marker: 0xFFE0, EXIF marker: 0xFFE1
        short secondMarker = buffer.getShort(2);
        if (secondMarker != (short)0xFFE0 && secondMarker != (short)0xFFE1 && secondMarker != (short)0xFFDB && secondMarker != (short)0xFFC0) {
            throw new IOException("Bad JPEG format - invalid second marker");
        }

        buffer.order(byteorder);
    }

    /**
     * Fallback PNG header validation
     */
    @Unique
    private static void rdi$validatePngHeader(ByteBuffer buffer) throws IOException {
        ByteOrder byteorder = buffer.order();
        buffer.order(ByteOrder.BIG_ENDIAN);
        if (buffer.getLong(0) != -8552249625308161526L) {
            throw new IOException("Bad PNG Signature");
        } else if (buffer.getInt(8) != 13) {
            throw new IOException("Bad length for IHDR chunk!");
        } else if (buffer.getInt(12) != 1229472850) {
            throw new IOException("Bad type for IHDR chunk!");
        } else {
            buffer.order(byteorder);
        }
    }
}
