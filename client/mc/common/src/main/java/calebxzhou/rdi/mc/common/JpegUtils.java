package calebxzhou.rdi.mc.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * calebxzhou @ 2026-01-05 22:43
 */
public class JpegUtils {
    /**
     * Validates JPEG header similar to PNG validation
     */
    public static void validateJpegHeader(ByteBuffer buffer) throws IOException {
        ByteOrder byteorder = buffer.order();
        buffer.order(ByteOrder.BIG_ENDIAN);

        // Check for JPEG SOI (Start of Image) marker: 0xFFD8
        if (buffer.getShort(0) != (short) 0xFFD8) {
            throw new IOException("Bad JPEG Signature - missing SOI marker");
        }

        // Check for JPEG EOI (End of Image) marker at the end or JFIF/EXIF marker
        // JFIF marker: 0xFFE0, EXIF marker: 0xFFE1
        short secondMarker = buffer.getShort(2);
        if (secondMarker != (short) 0xFFE0 && secondMarker != (short) 0xFFE1 && secondMarker != (short) 0xFFDB && secondMarker != (short) 0xFFC0) {
            throw new IOException("Bad JPEG format - invalid second marker");
        }

        buffer.order(byteorder);
    }

    /**
     * Fallback PNG header validation
     */
    public static void validatePngHeader(ByteBuffer buffer) throws IOException {
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
